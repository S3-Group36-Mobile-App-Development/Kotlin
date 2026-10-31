package com.zenmind.kotlin.features.games.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST

data class GameSessionRequest(
    val id: String,
    val durationMs: Int,
    val errors: Int,
    val correctTaps: Int,
    val completedAt: String
)

fun GameSessionEntity.toRequest() = GameSessionRequest(id, durationMs, errors, correctTaps, completedAt)

data class GameSessionAck(val id: String?)

// The server also returns createdAt and usuarioId; they are never sent in the POST.
data class RemoteGameSession(
    val id: String?,
    val usuarioId: String?,
    val durationMs: Int?,
    val errors: Int?,
    val correctTaps: Int?,
    val completedAt: String?
)

interface GameApiService {
    @GET("api/v1/juegos/sesiones")
    suspend fun listSessions(@Header("Authorization") authorization: String): Response<List<RemoteGameSession>>

    @POST("api/v1/juegos/sesiones")
    suspend fun registerSession(
        @Header("Authorization") authorization: String,
        @Body request: GameSessionRequest
    ): Response<GameSessionAck>
}
