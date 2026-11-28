package com.zenmind.kotlin.features.flashcards.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.flashcards.data.FlashcardsApiService
import com.zenmind.kotlin.features.flashcards.model.ReaccionRequest

class FlashcardsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).flashcardDao()
        val token = TokenStorage(applicationContext).getAccessToken() ?: return Result.success()
        val bearer = "Bearer $token"

        val service = ApiClient.retrofit.create(FlashcardsApiService::class.java)
        val pending = dao.getPendingReactions()
        if (pending.isEmpty()) return Result.success()

        return try {
            for (reaction in pending) {
                val response = if (reaction.tipo == "DELETE") {
                    service.deleteReaccion(bearer, reaction.flashcardId)
                } else {
                    service.upsertReaccion(bearer, reaction.flashcardId, ReaccionRequest(reaction.tipo))
                }

                if (response.isSuccessful) {
                    dao.markReactionSynced(reaction.id)
                } else {
                    return Result.retry() // Reintento con backoff exponencial si falla la red
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}