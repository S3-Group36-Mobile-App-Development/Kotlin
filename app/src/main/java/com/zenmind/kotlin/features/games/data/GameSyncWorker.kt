package com.zenmind.kotlin.features.games.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(OWNER_ID) ?: return Result.failure()
        return try {
            val tokenStorage = TokenStorage(applicationContext)
            val processor = GameSyncProcessor(
                AppDatabase.getInstance(applicationContext).gameSessionDao(),
                ApiClient.retrofit.create(GameApiService::class.java)
            ) {
                withContext(Dispatchers.IO) {
                    val token = tokenStorage.getAccessToken() ?: return@withContext null
                    val owner = runCatching { ownerFromToken(token) }.getOrNull()
                        ?: return@withContext null
                    GameSessionAuth(owner, token)
                }
            }
            when (processor.sync(ownerId)) {
                GameSyncOutcome.Done -> Result.success()
                GameSyncOutcome.Retry -> Result.retry()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object { const val OWNER_ID = "ownerId" }
}
