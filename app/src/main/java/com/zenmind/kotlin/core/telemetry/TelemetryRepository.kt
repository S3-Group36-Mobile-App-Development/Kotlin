package com.zenmind.kotlin.core.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Crashes reportados por Firebase Crashlytics
class TelemetryRepository(
    private val api: TelemetryApiService,
    private val externalScope: CoroutineScope
) {

    fun screenView(screen: String) {

        FirebaseCrashlytics.getInstance().setCustomKey("pantalla", screen)

        externalScope.launch {
            try {
                api.sendEvent(TelemetryEvent(tipoEvento = "vista_pantalla", nombrePantalla = screen))
            } catch (e: Exception) {

            }
        }
    }
}
