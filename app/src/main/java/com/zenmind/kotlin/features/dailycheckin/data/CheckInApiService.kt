package com.zenmind.kotlin.features.dailycheckin.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface CheckInApiService {

    @GET("api/v1/estados-animo")
    suspend fun getMoods(): List<MoodDto>

    @GET("api/v1/usuarios/me")
    suspend fun getProfile(@Header("Authorization") authorization: String): ProfileDto

    @GET("api/v1/chequeos")
    suspend fun getCheckIns(@Header("Authorization") authorization: String): List<CheckInDto>

    @POST("api/v1/chequeos")
    suspend fun saveCheckIn(
        @Header("Authorization") authorization: String,
        @Body request: CheckInRequest
    ): Response<Unit>

    @GET("api/v1/flashcards")
    suspend fun getFlashcards(@Query("categoria") category: String): List<FlashcardDto>
}

data class MoodDto(val id: String, val nombre: String)

data class StreakDto(val rachaActualDias: Int, val rachaMasLargaDias: Int)

data class ProfileDto(val racha: StreakDto?)

data class CheckInDto(val registradoEn: String, val estadoAnimo: MoodDto)

data class CheckInRequest(val estadoAnimoId: Long, val comentario: String?)

data class FlashcardDto(val categoria: String, val contenido: String)
