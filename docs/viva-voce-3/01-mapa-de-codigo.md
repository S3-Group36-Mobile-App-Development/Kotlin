# Mapa de los 12 archivos del juego

Rutas relativas a `app/src/main/java/com/zenmind/kotlin/features/games/`. Se distinguen el **valor de retorno Kotlin** (a menudo `Unit`) y el **estado observable** (`GameState`, `StateFlow`, Room), que no son lo mismo. Cada apartado corresponde a un archivo real de la rama revisada.

## 1. `view/BubbleGameScreen.kt` — `BubbleGameScreen` y composables privados

**Para qué existe / responsabilidad:** presenta Juego, cabecera, tablero, mensaje de guardado, estadísticas e historial. Recibe `onBack: () -> Unit` desde `ZenNavGraph` y un `BubbleGameViewModel` creado con `BubbleGameViewModel.Factory`. Su función `@Composable` devuelve `Unit`: no devuelve una partida ni persiste por su cuenta.

**Quién la llama / a quién llama:** `ZenNavGraph` registra `Routes.GAMES` y la invoca. La pantalla recoge `viewModel.uiState` y `viewModel.records` mediante `collectAsStateWithLifecycle`; envía `resize(width, height)` cuando cambia el tamaño, `start()`, `tap(number)`, `resume()`, `retrySave()` y `loadHistory()` al ViewModel. El botón de regreso invoca `onBack`. `LifecycleEventEffect(ON_STOP)` llama `pause()` y `LaunchedEffect` llama `tick()` cada 250 ms **solo durante Running y mientras el lifecycle está STARTED**.

**Cómo trabaja:** `BoxWithConstraints` obtiene dimensiones disponibles en dp, `safeDrawingPadding()` respeta barras del sistema, un `Column` con `verticalScroll` permite ver el contenido en pantallas pequeñas. Las burbujas proceden del `GameState`; la pantalla no decide si acertaron. Debajo del tablero muestra **primero `GameStatisticsPanel`, luego las últimas cinco filas de historial**, con estado y UUID. `GameTitle`, `ResultRow`, `GameStatisticsPanel`, `formatStatsTime` y `formatTime` son auxiliares de presentación; tiempos formateados no reemplazan los milisegundos almacenados.

**Si falla:** cuando no se pudo cargar propietario/historial muestra mensaje y «Reintentar»; una escritura fallida muestra «Reintentar guardado», no una confirmación falsa. Cuando GET remoto falla, `records.refreshMessage` informa que sigue mostrando partidas locales. La pantalla no crea excepciones de red ni resuelve problemas de almacenamiento.

**Conceptos:** Compose, estado observado con lifecycle, callbacks, insets, efectos cancelables y navegación. Compose/Flow ya estaban en el repositorio; no presentarlos como APIs desarrolladas en el PDF del curso.

## 2. `viewmodel/BubbleGameViewModel.kt` — `BubbleGameViewModel`, `GameRecordsState`, `SaveStatus`

**Para qué existe / responsabilidad:** coordina reglas, propietario, guardado, observación Room, sincronización visible, GET y estadísticas, separando UI de datos. `Factory` recibe la `ZenMindApplication`, construye `GameSessionRepository` con DAO/`TokenStorage` y pasa `applicationScope` y `applicationContext`.

**Entradas y salidas principales:**

| Método | Quién llama y parámetros | Efecto / resultado observable |
|---|---|---|
| `resize(width: Float, height: Float)` | Pantalla; dp disponibles | Recalcula posiciones/tamaño en `BubbleGame`; publica `uiState`; retorna `Unit`. |
| `start()` | Botón iniciar/nueva partida | Si hay owner y no está guardando ni falló guardado: reinicia `pendingSession`, invoca `game.start()` y publica estado. En otro caso no inicia. `Unit`. |
| `tap(number: Int)` | Clic en una burbuja | Invoca `game.tap(number)`; **solo** si pasó a `Finished`, crea `completedSession` y llama `savePending()`. `Unit`. |
| `tick()`, `pause()`, `resume()` | Efectos Compose/lifecycle/botón | Delegan al modelo y publican `uiState`. `Unit`. |
| `loadHistory()` | `init` y botón reintentar | Cancela trabajos previos, resuelve owner; lanza observación de sincronización y GET si autenticado; recoge `observeHistory(owner)`. `Unit` inmediato, resultados llegan por `records`. |
| `retrySave()` | UI si `Failed` | Llama `savePending()` con **el mismo** `pendingSession`/UUID. `Unit`. |
| `savePending()` (privado) | `tap` o `retrySave` | `applicationScope.async` escribe primero en Room; si corresponde, encola sincronización. `viewModelScope.launch` espera con `await()` y actualiza `Saving` → `Saved` o `Failed`. `Unit`. |

**Cómo trabaja `loadHistory`:** `historyJob`, `syncJob` y `refreshJob` se cancelan antes de reiniciar la lectura; `repository.currentOwner()` fija el contexto del historial. Una cuenta autenticada consulta `GameSyncScheduler.running()` y llama `repository.refreshFromBackend(owner)` en un trabajo ligado al ViewModel. Room emite historial con `Flow`; `collectLatest` publica filas y calcula estadísticas con `withContext(Dispatchers.Default)`, luego actualiza `records`. `uiState` es `StateFlow<GameState>`; `records` contiene `owner`, `history`, `saveStatus`, `syncing`, `statistics`, estados de cálculo y refresco.

**Si falla:** excepciones de propietario/Room en `loadHistory` producen mensaje; el GET fallido deja historial local. Un fallo de `enqueue` después del guardado **no deshace la escritura** ni debe anunciar pérdida de la partida; fallo de `repository.save` sí deja `Failed`. `CancellationException` se relanza. Una vez iniciada la escritura, salir de pantalla cancela la espera en `viewModelScope`, pero no por ello la tarea de `applicationScope`.

**Conceptos:** MVVM ya presente (B, pp. 608–610); `viewModelScope`, `async/await`, `launch`, `withContext(Default)` (A, pp. 1077–1097), StateFlow/Flow ya disponibles en el proyecto (D respecto al PDF).

## 3. `model/BubbleGame.kt` — `BubbleGame`, `GameState`, `Bubble`, `GamePhase`

**Para qué existe / responsabilidad:** reglas locales sin Room, Retrofit, Context ni Compose. Recibe `clock: () -> Long` (en producción `SystemClock.elapsedRealtime`) y `Random`; conserva un `state: GameState` legible desde el ViewModel. `GamePhase` tiene `Ready`, `Running`, `Paused`, `Finished`; `Bubble` lleva número y coordenadas dp.

**Métodos y retornos:** `resize(width, height)` conserva fase y reubica restantes si corre/pausa; `start()` crea 1..20, limpia contadores y arranca reloj si hay área suficiente y no está corriendo/pausado; `tap(number)` ignora toques fuera de Running, números ausentes y doble toque del mismo número dentro de 250 ms. Un número incorrecto suma `errors` y llama `place` para redistribuir; uno correcto quita la burbuja, avanza `nextNumber` y al 20 fija duración y `Finished`. `tick()` solo actualiza `displayMs`; `pause()` acumula tiempo, `resume()` inicia otro tramo monotónico. Todos retornan `Unit`; el ViewModel lee `state` después.

**Métodos privados:** `diameter()` calcula tamaño según tablero; `place(numbers, previous)` retorna `List<Bubble>` o lista vacía si el diámetro es menor a 40 dp. Usa 20 celdas (4×5), mezcla celdas y desplaza cada burbuja dentro de su celda; así evita solapamientos sin un bucle de intentos sin límite. Si al reorganizar sale exactamente la misma asignación de celdas, la rota. `startedAt` y `elapsedMs` miden intervalos de juego; `displayMs` es valor para UI.

**Si falla / límites:** si el tablero no cabe, `start()` no inicia (sin red ni datos que perder). Una entrada incorrecta no causa excepción ni fin de partida. Cambios de reloj de pared no afectan la duración. No confundir la prueba de `resize` con un test de rotación real de la Activity: `MainActivity` fija orientación vertical.

**Conceptos:** estado/colecciones de Kotlin (T), reloj monotónico y lifecycle de Android (B, pp. 773–791); generación de solo 20 posiciones breve, sin threads ni dispatcher dedicado.

## 4. `model/GameStatistics.kt` — `GameStatistics`, `calculateGameStatistics`

**Para qué existe / responsabilidad:** función pura de agregación, sin UI ni E/S. El ViewModel llama `calculateGameStatistics(history: List<GameSessionEntity>, owner: GameOwner)` y recibe `GameStatistics(completedGames, averageDurationMs, bestDurationMs, totalErrors, accuracy)`.

**Dentro:** filtra por `owner.id` y `isGuest`, elimina duplicados por UUID mediante `distinctBy`, acumula milisegundos, aciertos y errores en `Long`, toma mínimo y calcula `accuracy = ΣcorrectTaps / (ΣcorrectTaps + Σerrors)`. Sin filas devuelve cantidad 0, tiempos `null`, errores 0, precisión 0. Con denominador 0 evita división por cero. El promedio usa división entera en milisegundos; UI solo formatea el valor.

**Si falla / límites:** no hay llamadas que puedan fallar por red o Room dentro de la función. Es quien la llama el que decide pasar un historial válido y ejecutar el cómputo en `Dispatchers.Default`. El filtro doble protege frente a mezclar usuario e invitado aunque llegara una lista heterogénea.

**Conceptos:** funciones puras y colecciones Kotlin (T); rendimiento/evitar bloquear UI (B, pp. 1016–1045); dispatcher elegido por ViewModel (A, pp. 1085–1097).

## 5. `data/GameSessionEntity.kt` — `GameSessionEntity`, `GameSyncStatus`

**Para qué existe / responsabilidad:** una fila persistente de `game_sessions`. Room la construye a partir de sus columnas; repositorio, DAO y sincronización crean/leen copias. `@PrimaryKey id: String` fija UUID; `durationMs: Int`, `errors`, `correctTaps`, `completedAt: String`, `syncStatus`, `ownerId`, `isGuest` identifican resultado y propietario.

**Métodos / salidas:** es `data class`: sus funciones generadas (`copy`, igualdad) ayudan a comparar una sesión existente y a representar filas; no contiene operaciones de E/S. `GameSyncStatus` nombra `pending`, `synced`, `blocked_auth`, `blocked_consent`, `blocked_invalid`; el valor inicial es `pending`.

**Si falla:** los campos no confirman por sí solos que un POST haya llegado; `synced` solo debe ser establecido por DAO después de una confirmación válida. La clave única evita dos filas con el mismo UUID. Una fila invitada permanece distinta gracias a `isGuest` y nunca debe enviarse automáticamente.

**Conceptos:** entidad/clave primaria Room, tecnología C ya instalada (mencionada en clase, pp. 895 y 924); separación de datos y persistencia (B, pp. 793–831, 1254–1314).

## 6. `data/GameSessionDao.kt` — `GameSessionDao`

**Para qué existe / responsabilidad:** contratos SQL de Room; la implementación se genera, no se escribe a mano. Lo llaman `GameSessionRepository`, `GameSyncScheduler`, `GameSyncProcessor` y `GameHistoryRefresher`.

| Funciones, entrada → salida | Qué protegen / caso de error |
|---|---|
| `insert(session) → Long`; `byId(id) → GameSessionEntity?` | Inserta con `IGNORE`: un UUID repetido da `-1`; el repositorio comprueba si era realmente la misma fila antes de considerarla guardada. Error de BD se propaga. |
| `forOwner(ownerId, isGuest) → List<…>`; `observeForOwner(ownerId, isGuest) → Flow<List<…>>` | Consultan solo el propietario/invitado indicado; `Flow` emite nuevos valores al cambiar Room. |
| `pendingForOwner(ownerId) → List<…>` | Solo `isGuest = 0` y `syncStatus = pending`, ordenadas para envío. |
| `markSynced(id, ownerId) → Int`; `markConfirmedByHistory(id, ownerId) → Int` | Actualizan únicamente una fila autenticada del propietario indicado. La primera exige `pending`; la segunda se usa al comprobar la misma sesión en GET. |
| `markBlocked(id, ownerId, status) → Int`; `blockPendingForOwner(ownerId, status) → Int` | Fijan motivo de rechazo sin borrar filas ni afectar invitados u otros usuarios. |
| `restoreAfterLogin(ownerId) → Int` | Solo devuelve `blocked_auth` a `pending` del propietario autenticado tras nuevo login/restauración. |

**Conceptos:** `@Dao`, `@Insert`, `@Query`, operaciones suspendidas Room y `Flow` ya usado por la app. Un método `suspend` no equivale por sí solo a «background»; Room gestiona la ejecución de sus consultas.

## 7. `data/GameSessionRepository.kt` — `GameSessionRepository`, `GameOwner`, helpers

**Para qué existe / responsabilidad:** acceso mínimo a datos del juego y a la identidad local. Recibe `GameSessionDao` y `TokenStorage`; ViewModel/scheduler lo construyen o lo consultan.

| Función, parámetros → retorno | Qué hace / fallos |
|---|---|
| `currentOwner() → GameOwner` (`suspend`) | En `withContext(IO)`, lee token. Sin token devuelve `GameOwner.Guest`; con token usa `ownerFromToken(token)` para obtener el `sub` del JWT. Token no interpretable lanza `IllegalStateException`, no se adjudica a una cuenta arbitraria. |
| `completedSession(state, owner, id = UUID.randomUUID(), completedAt = Date()) → GameSessionEntity` (función de archivo) | Exige `Finished` y 20 aciertos; fecha UTC ISO8601, duración en ms limitada al rango `Int`, propietario e `isGuest`; no hace HTTP. Partida incompleta lanza `IllegalArgumentException`. |
| `save(session)` (`suspend`) → `Unit` | En IO inserta; si `IGNORE` devuelve `-1`, acepta únicamente la **misma** fila por `byId`. Colisión diferente genera error, no falso «guardado». |
| `observeHistory(owner) → Flow<List<…>>`; `history(owner) → List<…>` (`suspend`) | Filtran por propietario/invitado. La pantalla usa `observeHistory`; `history` existe, pero no encontré llamada desde el código principal del módulo. |
| `refreshFromBackend(owner) → Boolean` (`suspend`) | Si invitado, `false`; en otro caso crea `GameHistoryRefresher` con servicio Retrofit y proveedor del token; el booleano indica si se procesó un GET válido. |

**Conceptos:** `withContext(Dispatchers.IO)` (A, pp. 1077–1088); UUID, serialización de fecha y adaptación Kotlin (T); almacenamiento/privacidad (B). Decodificar `sub` permite identificar el propietario local; **no se envía `ownerId` en el POST**.

## 8. `data/GameApiService.kt` — `GameApiService` y modelos HTTP

**Para qué existe / responsabilidad:** contrato declarativo Retrofit. `GameSyncProcessor` llama `registerSession(authorization, request) → Response<GameSessionAck>` (POST); `GameHistoryRefresher` llama `listSessions(authorization) → Response<List<RemoteGameSession>>` (GET). Son funciones `suspend` generadas por Retrofit.

**Entradas/salidas:** `authorization = "Bearer <token>"`. `toRequest()` transforma una fila local en `GameSessionRequest(id, durationMs, errors, correctTaps, completedAt)`; **excluye** `ownerId`, `isGuest`, `syncStatus` y `usuarioId`. `GameSessionAck.id` se comprueba contra el UUID enviado. `RemoteGameSession` permite campos nulos para validar antes de insertar. El backend identifica al usuario a partir del Bearer token (`juegos.controller.ts`).

**Si falla:** Retrofit puede devolver HTTP no exitoso o lanzar excepción de red; el procesador interpreta POST y el refrescador conserva la caché ante GET fallido. Esta interfaz **no** decide reintentos ni marca filas `synced`.

**Conceptos:** HTTP y REST (B, pp. 581–603, 686–720), Retrofit tecnología C ya configurada; `suspend` A. No atribuir al PDF un tutorial completo de Retrofit.

## 9. `data/GameSyncScheduler.kt` — `GameSyncScheduler`

**Para qué existe / responsabilidad:** agendar, no enviar. Lo llaman `BubbleGameViewModel.savePending`, `loadHistory`, `ZenNavGraph` al entrar/restaurar sesión y `AppContainer` cuando vuelve la conexión.

**Métodos:** `enqueue(context, owner, policy = KEEP) → Unit` no agenda invitados; configura un nombre único `game_sync_<ownerId>`, `GameSyncWorker.OWNER_ID` de entrada, `NetworkType.CONNECTED` y backoff **exponencial con base de 30 segundos**. Desde guardado nuevo el ViewModel usa `APPEND_OR_REPLACE` para no perder trabajo añadido mientras otro corre. `enqueuePending(context, reauthenticated = false)` (`suspend`) lee propietario, opcionalmente devuelve `blocked_auth` a `pending` cuando hubo login y solo agenda si hay pendientes. `running(context, owner) → Flow<Boolean>` traduce `WorkInfo.State.RUNNING` al indicador visual.

**Si falla / límites:** conectividad del dispositivo no garantiza que el backend responda; WorkManager puede retrasar la ejecución. El ViewModel captura fallo de agendamiento después de guardar sin convertir el guardado confirmado en error de escritura; próximos disparadores pueden reencolar. No se borra historial local.

**Conceptos:** trabajo diferido y backoff con WorkManager **ya presente** (D respecto al PDF), eventual connectivity (B, pp. 849–931), Flow ya presente (D).

## 10. `data/GameSyncWorker.kt` — `GameSyncWorker`

**Para qué existe / responsabilidad:** punto de entrada de WorkManager. `doWork() → Result` lee `ownerId` del `inputData`, crea DAO y `GameApiService` usando `ApiClient`, y entrega a `GameSyncProcessor` un proveedor suspendido de `GameSessionAuth(owner, token)` leído en IO.

**Resultados:** sin `ownerId`, `Result.failure()`. `GameSyncOutcome.Done` → `Result.success()`; `Retry` o excepción inesperada → `Result.retry()`. `CancellationException` se relanza; no se convierte en «éxito». El worker **no** acepta un invitado como dueño autenticado ni manda directamente un POST: la verificación/decisión es del procesador.

**Conceptos:** `CoroutineWorker`, `withContext(Dispatchers.IO)`, token y lifecycle de trabajo diferido. El worker puede arrancar después de que cambie la pantalla o la cuenta; por eso obtiene el token **en el momento de ejecutarse**, no lo guarda como dato de entrada del trabajo.

## 11. `data/GameSyncProcessor.kt` — `GameSyncProcessor`, `GameSessionAuth`, `GameSyncOutcome`

**Para qué existe / responsabilidad:** decisiones testeables de envío. Constructor: DAO, API y `auth: suspend () -> GameSessionAuth?`. `sync(ownerId: String) → GameSyncOutcome` recibe dueño esperado por el worker y devuelve `Done` o `Retry`.

**Dentro:** verifica identidad del token frente al dueño esperado; consulta `pendingForOwner`; antes de **cada** POST obtiene token actual y comprueba dueño. `session.toRequest()` manda solo cinco campos. Después de la respuesta vuelve a comprobar dueño para no confirmar una partida con otra cuenta. Si hubo `401` pero cambió el token del mismo usuario, devuelve `Retry` para intentar con el nuevo.

| Respuesta o fallo | Acción real |
|---|---|
| 2xx con `response.body()?.id == session.id` | `dao.markSynced(id, ownerId)`; conserva la fila. |
| 2xx sin cuerpo o ID incorrecto | `Retry`; no hay confirmación válida. |
| `IOException`, 408, 429, 5xx u otra excepción no cancelación en POST | `Retry`, sin marcar `synced`; WorkManager aplica backoff. |
| 401 con token aún igual | Bloquea pendientes del dueño como `blocked_auth`; `Done` evita reintento ciego. |
| 403 | Bloquea pendientes de ese dueño como `blocked_consent`; `Done`. |
| Otros 4xx, incluido 400 | Marca la fila como `blocked_invalid`; puede continuar con otras filas. |
| Logout/cambio de owner durante POST | `Done` sin confirmar esa fila bajo otra identidad; queda local para el dueño original. |

**Conceptos:** corrutinas `suspend`, cancelación, idempotencia por UUID, distinción temporal/permanente de fallos, protección interusuario (B, pp. 849–882, 1254–1314). `Done` significa «este intento no requiere retry inmediato»; **no significa que todas las filas estén sincronizadas**.

## 12. `data/GameHistoryRefresher.kt` — `GameHistoryRefresher`

**Para qué existe / responsabilidad:** GET y conciliación local, no guardar el resultado de una partida recién terminada. Constructor: DAO, API y proveedor suspendido de `GameSessionAuth?`. `refresh(owner: GameOwner) → Boolean` ejecuta en `Dispatchers.IO`.

**Dentro:** si invitado o token de otro owner, devuelve `false`; consulta `api.listSessions("Bearer …")`; si HTTP no exitoso o cuerpo **nulo**, `false` (una lista vacía válida devuelve `true`). Revalida owner durante cada elemento. `RemoteGameSession.asLocal(owner)` descarta otro `usuarioId`, UUID incorrecto, métricas negativas/nulas y fecha no reconocida; construye fila `synced` solo para el mismo owner. Si falta UUID local, inserta con `IGNORE`; si ya existe, **solo** llama `markConfirmedByHistory` cuando owner, `isGuest`, duración, errores, aciertos y fecha coinciden. Retorna `true` si pudo recorrer la respuesta; no significa que cada fila remota haya pasado la validación.

**Si falla:** GET fallido no elimina el historial Room; cambio de cuenta durante la petición detiene el refresco. Una colisión de UUID con datos distintos **no sobrescribe** el dato local. Una excepción de red se maneja en el trabajo de refresco del ViewModel, que muestra mensaje y mantiene las filas existentes.

**Conceptos:** `withContext(IO)` (A), eventual connectivity y persistencia/caché (B, pp. 793–931), Retrofit C ya instalado. Se diferencia del POST: aquí se **lee y concilia**; no se envía una nueva partida.
