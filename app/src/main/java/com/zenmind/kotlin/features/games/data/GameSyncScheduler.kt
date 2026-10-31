package com.zenmind.kotlin.features.games.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.storage.TokenStorage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object GameSyncScheduler {
    private fun workName(owner: GameOwner) = "game_sync_${owner.id}"

    fun enqueue(context: Context, owner: GameOwner, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
        if (owner.isGuest) return
        val request = OneTimeWorkRequestBuilder<GameSyncWorker>()
            .setInputData(workDataOf(GameSyncWorker.OWNER_ID to owner.id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(owner), policy, request)
    }

    suspend fun enqueuePending(context: Context, reauthenticated: Boolean = false) {
        val repository = GameSessionRepository(AppDatabase.getInstance(context).gameSessionDao(), TokenStorage(context))
        val owner = repository.currentOwner()
        if (owner.isGuest) return
        val dao = AppDatabase.getInstance(context).gameSessionDao()
        if (reauthenticated) dao.restoreAfterLogin(owner.id)
        if (dao.pendingForOwner(owner.id).isNotEmpty()) enqueue(context, owner)
    }

    fun running(context: Context, owner: GameOwner): Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(workName(owner))
            .map { info -> info.any { it.state == WorkInfo.State.RUNNING } }
            .distinctUntilChanged()
}
