package com.zenmind.kotlin.features.flashcards.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardDao
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardsSyncWorker
import com.zenmind.kotlin.features.flashcards.model.FlashcardCardDto
import com.zenmind.kotlin.features.flashcards.model.FlashcardTemaDto
import com.zenmind.kotlin.features.flashcards.model.ReaccionRequest
import com.zenmind.kotlin.features.flashcards.model.ReaccionResponse
import com.zenmind.kotlin.features.flashcards.model.UserReactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface FlashcardsApiService {
    @GET("api/v1/flashcards/temas")
    suspend fun getTemas(
        @Query("categoria") categoria: String? = null
    ): Response<List<FlashcardTemaDto>>

    @GET("api/v1/flashcards/temas/{id}")
    suspend fun getTemaDetail(
        @Path("id") id: String
    ): Response<FlashcardTemaDto>

    @GET("api/v1/flashcards/categorias")
    suspend fun getCategorias(): Response<List<String>>

    @GET("api/v1/flashcards/reacciones")
    suspend fun getReacciones(
        @Header("Authorization") authorization: String
    ): Response<List<UserReactionDto>>

    @PUT("api/v1/flashcards/{id}/reaccion")
    suspend fun upsertReaccion(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body request: ReaccionRequest
    ): Response<ReaccionResponse>

    @DELETE("api/v1/flashcards/{id}/reaccion")
    suspend fun deleteReaccion(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): Response<Map<String, Boolean>>
}

class FlashCardsRepository(
    private val dao: FlashcardDao,
    private val context: Context
) {
    private val service: FlashcardsApiService by lazy {
        ApiClient.retrofit.create(FlashcardsApiService::class.java)
    }

    suspend fun getTemas(categoria: String? = null): List<FlashcardTemaDto> {
        try {
            android.util.Log.d("FlashcardsRepo", "Llamando GET /api/v1/flashcards/temas con categoria=$categoria")
            val response = service.getTemas(categoria)
            android.util.Log.d("FlashcardsRepo", "getTemas code=${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful) {
                response.body()?.let { temas ->
                    android.util.Log.d("FlashcardsRepo", "Temas recibidos de red: ${temas.size}")
                    dao.upsertTemas(temas.map { it.toEntity() })
                }
            } else {
                android.util.Log.e("FlashcardsRepo", "Error getTemas body: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("FlashcardsRepo", "Excepcion getTemas: ${e.message}", e)
        }
        val localTemas = dao.getAllTemas()
        android.util.Log.d("FlashcardsRepo", "Temas en Room local: ${localTemas.size}")
        return localTemas.map { it.toDto() }
    }

    suspend fun getTemaDetail(id: String): FlashcardTemaDto? {
        try {
            android.util.Log.d("FlashcardsRepo", "Llamando GET /api/v1/flashcards/temas/$id")
            val response = service.getTemaDetail(id)
            android.util.Log.d("FlashcardsRepo", "getTemaDetail code=${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful) {
                response.body()?.let { tema ->
                    android.util.Log.d("FlashcardsRepo", "Tema detalle recibido: ${tema.titulo}, cards=${tema.cards?.size}")
                    dao.upsertTemas(listOf(tema.toEntity()))
                    tema.cards?.let { cards ->
                        dao.upsertCards(cards.map { it.toEntity() })
                    }
                    return tema
                }
            } else {
                android.util.Log.e("FlashcardsRepo", "Error getTemaDetail body: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("FlashcardsRepo", "Excepcion getTemaDetail: ${e.message}", e)
        }

        // Fallback local desde Room
        val localTemas = dao.getAllTemas()
        val matchingTema = localTemas.firstOrNull { it.id == id } ?: return null
        val localCards = dao.getCardsForTema(id)
        android.util.Log.d("FlashcardsRepo", "Fallback local cards para tema $id: ${localCards.size}")

        return FlashcardTemaDto(
            id = matchingTema.id,
            titulo = matchingTema.titulo,
            categoria = matchingTema.categoria,
            iconType = matchingTema.iconType,
            cardsCount = matchingTema.cardsCount,
            totalLikes = matchingTema.totalLikes,
            totalDislikes = matchingTema.totalDislikes,
            cards = localCards.map { it.toDto() }
        )
    }

    suspend fun getReacciones(token: String): Map<String, String> {
        return try {
            val response = service.getReacciones("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.associate { it.flashcardId to it.tipo } ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun likeCard(cardId: String, token: String): Boolean {
        return try {
            val response = service.upsertReaccion(
                authorization = "Bearer $token",
                id = cardId,
                request = ReaccionRequest(tipo = "LIKE")
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun dislikeCard(cardId: String, token: String): Boolean {
        return try {
            val response = service.upsertReaccion(
                authorization = "Bearer $token",
                id = cardId,
                request = ReaccionRequest(tipo = "DISLIKE")
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeReaction(cardId: String, token: String): Boolean {
        return try {
            val response = service.deleteReaccion(
                authorization = "Bearer $token",
                id = cardId
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun enqueueReactionSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<FlashcardsSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "flashcards_reaction_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
