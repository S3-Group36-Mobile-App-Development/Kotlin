package com.zenmind.kotlin.features.breathing.data

import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class CrearSesionRequest(val ejercicioId: Int)
data class SesionResponse(val id: Int)

interface BreathingApiService {
    @POST("api/v1/respira/sesiones")
    suspend fun crearSesion(
        @Header("Authorization") authorization: String,
        @Body request: CrearSesionRequest
    ): Response<SesionResponse>

    @PATCH("api/v1/respira/sesiones/{id}/completar")
    suspend fun completarSesion(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Response<SesionResponse>
}


class BreathingRepository(
    private val tokenStorage: TokenStorage
) {
    private val service: BreathingApiService by lazy {
        ApiClient.retrofit.create(BreathingApiService::class.java)
    }

    /**
     * Guarda una sesion completada.
     * Devuelve true si se guardo, false si no hay token,
     * no hay red o el backend respondio error.
     */
    suspend fun guardarSesion(ejercicioId: Int): Boolean {
        val token = tokenStorage.getAccessToken() ?: return false
        val bearer = "Bearer $token"
        return try {
            val crear = service.crearSesion(bearer, CrearSesionRequest(ejercicioId))
            if (!crear.isSuccessful) return false
            val id = crear.body()?.id ?: return false
            service.completarSesion(bearer, id).isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
