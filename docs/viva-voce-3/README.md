# Viva Voce 3 — Burbujas en orden (Kotlin)

Esta es una lectura del código **real de la rama `viva-voce-cambios` en `2d0a635`**, no una propuesta de implementación. En P5.1 solo se añadió documentación. Si cambia la rama, volver a contrastar nombres, contratos y versión de la base de datos antes de sustentar.

## Ruta de estudio

1. [01 · Los 12 archivos y sus contratos](01-mapa-de-codigo.md): responsabilidad, llamadas, entradas, salidas y fallos.
2. [02 · Flujos de ejecución](02-flujos.md): diagramas Mermaid desde «Empezar» hasta Room, WorkManager, Retrofit, historial y estadísticas.
3. [03 · Corrutinas, almacenamiento y caché](03-concurrencia-y-datos.md): qué hace realmente cada scope y dispatcher, y qué permanece offline.
4. [04 · Preguntas de Viva y P5.2](04-viva-y-riesgos.md): respuestas basadas en métodos reales, simplificaciones condicionadas y revisión de la prueba de migración.

**Rutas de este párrafo tomadas desde la raíz del repositorio Kotlin.** Los 12 archivos están en `app/src/main/java/com/zenmind/kotlin/features/games/`. Fuera del módulo, la navegación y los disparadores están en `app/src/main/java/com/zenmind/kotlin/navigation/ZenNavGraph.kt` y `app/src/main/java/com/zenmind/kotlin/data/AppContainer.kt`; el scope de aplicación está en `app/src/main/java/com/zenmind/kotlin/ZenMindApplication.kt`; Room se configura en `app/src/main/java/com/zenmind/kotlin/core/database/AppDatabase.kt`. El contrato HTTP se contrastó además con `../Backend/src/modules/juegos/juegos.controller.ts`, `juegos.service.ts` y `dto/sesion-juego.dto.ts` (proyecto hermano local). La guía inicial `../GUIA_CODIGO_JUEGOS_VIVA.md` está en la carpeta superior al repositorio Kotlin; aquí se corrigieron sus referencias a la versión actual.

## Cuatro ideas para explicar en menos de un minuto

- La pantalla solo dibuja y envía eventos; `BubbleGame` decide aciertos, errores, posiciones y duración.
- Terminar crea **un UUID y una fila Room primero**. El guardado no depende de que haya internet.
- WorkManager intenta enviar exclusivamente partidas del usuario autenticado; Retrofit manda cinco campos y la confirmación exige que `ack.id` coincida. Si falla temporalmente, la fila queda pendiente.
- El historial visible viene de Room (también offline). Un GET autenticado puede completar/confirmar filas por UUID; el ViewModel calcula estadísticas del historial **completo** de ese propietario, aunque la pantalla solo liste las cinco últimas.

## Procedencia académica, sin atribuciones inventadas

La guía local `../Guia_Kotlin_Android_ISIS3510_REVISION_EXHAUSTIVA.md` remite a páginas físicas de las diapositivas: arquitectura/acoplamiento (B, pp. 564–640), UX (B, pp. 369–481), almacenamiento (B, pp. 793–831), eventual connectivity y caché (B, pp. 849–931) y Kotlin coroutines (A, pp. 1069–1098). Room y Retrofit figuran como tecnologías **C nombradas** (pp. 895/924 y 490/1076); Compose, Flow y WorkManager son **D no identificadas como enseñadas**, aunque ya están integradas en este proyecto. La sintaxis y configuración concreta son adaptación **T**. No afirmar que el PDF enseña la implementación completa de esas bibliotecas.

**Estado de verificación de P5.1:** lectura de archivos y comparación de rutas; compilación, tests automatizados y ejecución en dispositivo **NO EJECUTADO** en esta fase exclusivamente documental. El hecho de que iteraciones anteriores hayan sido probadas no sustituye una ejecución nueva después de futuros refactors.
