package com.zenmind.kotlin.features.dailycheckin.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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

    @GET("api/v1/flashcards/temas")
    suspend fun getFlashcardTopics(): List<FlashcardTopicDto>

    @GET("api/v1/flashcards/temas/{id}")
    suspend fun getFlashcardTopic(@Path("id") id: Long): FlashcardTopicDetailDto
}

data class MoodDto(val id: String, val nombre: String, val orden: Int)

data class StreakDto(val rachaActualDias: Int, val rachaMasLargaDias: Int)

data class ProfileDto(val racha: StreakDto?)

data class CheckInDto(val registradoEn: String, val estadoAnimo: MoodDto, val comentario: String?)

data class CheckInRequest(val estadoAnimoId: Long, val comentario: String?)

data class FlashcardTopicDto(val id: String, val titulo: String)

data class FlashcardTopicDetailDto(val titulo: String, val cards: List<FlashcardDto>)

data class FlashcardDto(val contenido: String, val fuente: String?)
