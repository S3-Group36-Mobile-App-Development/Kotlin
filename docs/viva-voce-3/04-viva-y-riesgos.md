# Viva Voce 3: preguntas difíciles y límites de una futura P5.2

Respuestas referidas a **métodos de la rama actual**, no a una arquitectura imaginaria. Para sustentar, señala el archivo correspondiente en [01 · Mapa del código](01-mapa-de-codigo.md) y sigue los diagramas de [02 · Flujos](02-flujos.md).

## Preguntas y respuestas que puedes defender en pantalla

### 1. «¿Cómo sé que tocar el 20 no guarda dos partidas?»

`BubbleGame.tap(number)` solo procesa burbujas que aún existen y congela `Finished` cuando el número 20 es correcto. `BubbleGameViewModel.tap` compara **antes y después**:

```kotlin
val wasFinished = game.state.phase == GamePhase.Finished
update { game.tap(number) }
if (!wasFinished && game.state.phase == GamePhase.Finished) {
    val owner = _records.value.owner ?: return
    pendingSession = completedSession(game.state, owner)
    savePending()
}
```

Una recomposición de Compose no llama por sí sola a `completedSession`. Además, Room tiene `id` como clave primaria; `save` solo acepta el duplicado si coincide exactamente con la misma sesión.

### 2. «¿Por qué no se congela la UI al guardar o consultar el servidor?»

El ViewModel inicia trabajos con corrutinas; `applicationScope` utiliza `Dispatchers.IO` para el guardado, `GameSessionRepository.save` usa `withContext(IO)` y los DAO Room son suspendidos. `GameHistoryRefresher.refresh` usa IO; Retrofit GET/POST son suspendidos. `calculateGameStatistics` se ejecuta en `Dispatchers.Default` y luego se publica el resultado en `records`. Generar solo 20 posiciones es una operación breve que continúa en el hilo de UI. No prometas que todo `suspend` cambia de hilo por sí mismo.

### 3. «¿Qué pasa si cierro la pantalla inmediatamente después de acertar el 20?»

`BubbleGameViewModel.savePending` inicia `applicationScope.async { repository.save(session) … }`. La espera `write.await()` vive en `viewModelScope`; esta espera puede cancelarse al salir, mientras la escritura ya iniciada en el scope de aplicación puede terminar. El código **no** garantiza persistencia si Android mata el proceso antes de que Room confirme. Por eso solo anuncia Saved después de `await` cuando la pantalla sigue disponible.

### 4. «¿Qué ocurre si se cancela la corrutina durante el POST?»

`GameSyncProcessor.sync` captura y vuelve a lanzar `CancellationException`; `GameSyncWorker.doWork` también la relanza, en lugar de simular una confirmación o un error temporal. Como `markSynced` ocurre solo tras respuesta válida y comprobación de identidad, una cancelación antes de ese punto deja la fila sin confirmar. No conviertas la cancelación en `Result.success()` durante un refactor.

### 5. «¿Y si el servidor recibe el POST pero se pierde la respuesta?»

El cliente no puede comprobar `ack.id`. `GameSyncProcessor.sync` deja la fila local `pending` y devuelve `Retry`. WorkManager usa backoff y el **mismo UUID** en `GameSessionRequest`. El backend, en `juegos.service.ts`, reconoce un UUID existente del mismo usuario y devuelve esa sesión sin duplicarla. En el siguiente intento, si llega una confirmación con el mismo ID, `markSynced` cambia el estado local.

### 6. «¿Qué haces si el teléfono tiene Wi-Fi, pero el backend responde 503?»

`NetworkType.CONNECTED` solo permite ejecutar el worker; `GameSyncProcessor` recibe la respuesta 503 y devuelve `Retry` sin borrar ni confirmar la fila. Room sigue mostrando historial/estadísticas. El GET remoto fallido en `GameHistoryRefresher.refresh` conserva historial local y el ViewModel informa que no pudo actualizar servidor.

### 7. «¿Por qué 401 y 403 no se tratan como 500?»

`GameSyncProcessor.sync`: 401 con el mismo token bloquea las filas de esa cuenta como `blocked_auth`; 403 las bloquea como `blocked_consent`; otros 4xx permanentes pueden producir `blocked_invalid`. Reintentar indefinidamente no arreglaría credenciales o consentimiento. Después de login/restauración, `GameSyncScheduler.enqueuePending(reauthenticated = true)` llama `restoreAfterLogin(owner.id)` **solo** para `blocked_auth` de ese propietario. Un 401 con token que cambió durante la solicitud permite retry con el nuevo token.

### 8. «¿Qué pasa si cierro sesión o cambio de cuenta mientras hay un POST?»

El worker recibe `ownerId` esperado, pero **no** reutiliza un token guardado cuando se encoló. `GameSyncProcessor.sync` consulta `auth()` antes de cada POST y después de la respuesta. Si el propietario actual ya no coincide, termina sin marcar `synced` la fila anterior. El DAO consulta y actualiza por `ownerId` y excluye `isGuest = 1`. La petición que ya salió no puede retirarse del servidor, pero cualquier reintento conserva el UUID del propietario original.

### 9. «¿Puede una partida de invitado subirse cuando luego inicia sesión?»

No automáticamente. `completedSession` la guarda como `ownerId = "guest", isGuest = true`; `GameSyncScheduler.enqueue` no agenda invitados y `pendingForOwner` consulta solo `isGuest = 0`. Tampoco el GET de otra cuenta la reclama. No existe una operación de «migrar partidas de invitado a usuario» en Juegos.

### 10. «¿Por qué el GET no duplica mis partidas ni sobrescribe una diferente?»

`GameHistoryRefresher.refresh` verifica propietario y valida UUID, métricas y fecha. Hace `byId`; si no existe, intenta `insert(IGNORE)`. Si existe, solo llama `markConfirmedByHistory` cuando propietario, fecha, duración, errores y aciertos coinciden. No reemplaza toda la tabla. `GameStatistics` filtra también por owner y aplica `distinctBy(id)`; el DAO ya protege con clave primaria.

### 11. «¿El historial del servidor es la caché? ¿Cuál es la fuente de la pantalla?»

La fuente observable es **Room**, mediante `GameSessionDao.observeForOwner` → `GameSessionRepository.observeHistory` → `BubbleGameViewModel.loadHistory`. GET completa/actualiza Room, que emite otra lista. Sin red se ven las filas persistidas previamente. Es persistencia local con actualización remota (*cache then network* para la lectura), **no** un `LruCache` en memoria ni una garantía de que el GET ocurra en cada cambio externo del servidor: el refresco se solicita al abrir Juegos.

### 12. «¿Por qué promedio y precisión no se calculan con las cinco partidas que veo?»

`observeForOwner` emite la lista completa; `calculateGameStatistics(history, owner)` procesa todas las filas de ese dueño, quitando UUID repetidos. Solo `BubbleGameScreen` limita la sección visual con `records.history.take(5)`. Promedio usa milisegundos enteros; precisión divide sumas globales de aciertos y de aciertos+errores, con protección cuando el denominador es cero.

### 13. «¿Qué ocurre con las estadísticas si llega una nueva fila mientras estoy calculando?»

`BubbleGameViewModel.loadHistory` usa `collectLatest`: una nueva emisión de Room cancela el bloque suspendido anterior; ejecuta `calculateGameStatistics` en `withContext(Dispatchers.Default)` para el historial más reciente. No afirmar que la función pura verifica cancelación en cada suma: puede terminar un tramo breve de CPU antes de que la cancelación surta efecto. El estado publicado después del `withContext` corresponde al bloque que completó.

### 14. «¿Por qué no hacer la búsqueda de posiciones en otro hilo?»

`BubbleGame.place` baraja solo 20 celdas 4×5 y da un offset aleatorio dentro de cada una. Esa construcción acotada no hace red ni disco ni intentos de colisión ilimitados. Moverla a otro dispatcher introduciría coordinación que esta carga pequeña no justifica. `BubbleGameTest` prueba que quedan dentro de límites y sin solapamiento.

### 15. «¿Cuál es la versión de Room y qué conserva al actualizar?»

`AppDatabase.kt` declara **versión 7** y registra `MIGRATION_4_5`, `MIGRATION_5_6` y `MIGRATION_6_7`. La primera crea `game_sessions` y `breathing_exercises`; la segunda agrega `contacts`; la tercera cambia columnas de check-in/ánimo. No hay `fallbackToDestructiveMigration` en este builder. No atribuir al módulo Juegos toda la responsabilidad sobre las otras tablas. Véase la revisión de la prueba de migración más abajo.

## Decisiones complejas necesarias frente a simplificación superficial

| Mantener para proteger comportamiento/datos | Por qué no conviene borrarlo para «que se vea fácil» |
|---|---|
| `pendingSession` con UUID único, `insert(IGNORE)` + `byId` y ACK con el mismo UUID | Evitan doble guardado, aceptación falsa de colisión y doble envío por reintento. |
| `ownerId` + `isGuest`, consulta por owner y verificaciones antes/después de HTTP | Previenen atribuir filas de otro usuario o de invitado a una nueva cuenta. |
| `applicationScope.async` para guardar y `viewModelScope` para esperar/observar | Separan vida de una escritura importante de la vida de una pantalla. |
| `CancellationException` relanzada, 401/403/400 separados de red/5xx | Evitan interpretar cancelación o rechazo permanente como retry infinito o éxito. |
| `NetworkType.CONNECTED`, trabajo único por owner y backoff | Dan recuperación eventual sin suponer ejecución inmediata ni bombardear servidor. |
| Validación en `GameHistoryRefresher` antes de insertar/confirmar | Evita importar otra cuenta, un UUID/dato inválido o sobrescribir una fila distinta. |

## Métodos difíciles de explicar y posible P5.2 (solo propuesta)

| Prioridad | Método / cambio mínimo propuesto | Riesgo si se refactoriza mal | Pruebas necesarias antes/después |
|---|---|---|---|
| Alta | `BubbleGameViewModel.loadHistory()`: dividir en **privados** `observeLocalHistory`, `observeSyncStatus` y `refreshRemoteHistory` sin cambiar `historyJob`, `syncJob`, `refreshJob` ni scopes. | Múltiples colectores tras reintento, estadísticas antiguas, GET que sobreviva a la pantalla, cuenta equivocada. | Test unitario de estadísticas; instrumentada de historial/identidad; abrir Juegos, salir, volver, offline y cambio de usuario en dispositivo. |
| Media | `BubbleGameScreen`: extraer el bloque de historial a un composable sin cambiar orden, `take(5)`, modificadores de scroll ni estado observado. | Ocultar botón, cambiar medidas/insets, mostrar historial ajeno. | Compilación y comparación visual en teléfono pequeño, invitado/autenticado, historial vacío y con >5 entradas. |
| Media | `GameHistoryRefresher.refresh()`: nombrar en una función privada la comparación de métricas/owner de una fila existente. | Marcar `synced` una partida con fecha o métricas diferentes. | `GameHistoryRefresherTest`, datos distintos con mismo UUID, caída de red y cambio de cuenta durante GET. |
| Baja | Revisar `GameSessionRepository.history()`: no hallé llamada en el código principal del módulo; retirar solo si búsqueda completa confirma que nadie lo utiliza. | Romper tests u otro código fuera del módulo. | Búsqueda de referencias, compilación y suite de juegos; no suprimir `GameSessionDao.forOwner` si tests aún lo usan. |
| Mantener | `GameSyncProcessor.sync()` y `BubbleGame.place()` por ahora: documentarlos antes de reescribir. | Perder códigos HTTP, verificación de identidad, cancelación, idempotencia o garantía geométrica. | Tests de `GameSyncProcessorTest` y `BubbleGameTest`, más prueba en dispositivo de reconexión. |

## Revisión concreta de `GameDatabaseMigrationTest.kt` frente a `AppDatabase` v7

**Conclusión:** el método `upgradeFromFourPreservesCheckinsBreathingAndPersistsGamesOnReopen` está **desactualizado como prueba contra la base actual**, aunque parece querer verificar la introducción de `game_sessions` en la migración 4→5. Construye una BD con `old.version = 4` y registra **solo** `.addMigrations(MIGRATION_4_5)`, pero `Room.databaseBuilder(..., AppDatabase::class.java, ...)` apunta a `@Database(version = 7)`. Sin 5→6 y 6→7 no hay ruta completa hasta el esquema declarado; no constituye una prueba aislada correcta de una `AppDatabase` v5. Además, los DAO del target v7 esperan las transformaciones posteriores. **No la ejecutamos en P5.1**, por lo que este juicio es una lectura del código, no el resultado de un test nuevo.

Para P5.2, si autorizas tocar pruebas, plantearía registrar la cadena **4→5→6→7**, preparar una semilla v4 coherente con las entidades históricas y comprobar la conservación de filas de juego, respiración y check-in más la lectura en v7. Primero separar el alcance de las migraciones de otras funcionalidades del equipo; no editar su esquema ni usar migración destructiva para «hacer pasar» la prueba. `jwtSubSelectsOnlyTheAuthenticatedOwner` es otro método dentro del mismo archivo y no depende de ese upgrade.

## Puerta de verificación para P5.2

Tras aprobar **una** simplificación concreta, inspeccionar diff y ejecutar `:app:testDebugUnitTest`, compilación `:app:assembleDebug`, pruebas instrumentadas pertinentes de migración/reconciliación **si hay emulador o dispositivo disponible**, y prueba manual del APK instalado sobre la app existente: reglas 1..20, errores, pausa, guardado offline/reinicio, invitado y otra cuenta, POST y ACK/reintento, GET sin duplicados, estadísticas y scroll/insets. Reportar cada comando y escenario como **ejecutado** o **NO EJECUTADO**, sin extrapolar un test unitario a un servidor real o teléfono real.
