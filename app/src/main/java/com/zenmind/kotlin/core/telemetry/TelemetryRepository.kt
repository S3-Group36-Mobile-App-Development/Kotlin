package com.zenmind.kotlin.core.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TelemetryRepository(
    private val api: TelemetryApiService,
    private val externalScope: CoroutineScope
) {

    fun screenView(screen: String) {

        externalScope.launch {
            try {
                api.sendEvent(TelemetryEvent(tipoEvento = "vista_pantalla", nombrePantalla = screen))
            } catch (e: Exception) {

            }
        }
    }
}
