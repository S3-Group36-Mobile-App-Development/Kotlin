package com.zenmind.kotlin.core.telemetry

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TelemetryApiService {

    @POST("api/v1/telemetria/eventos")
    suspend fun sendEvent(@Body event: TelemetryEvent): Response<Unit>
}

data class TelemetryEvent(
    val tipoEvento: String,
    val nombrePantalla: String?,
    val metadata: Map<String, String>? = null,
    val plataforma: String = "android_kotlin"
)
