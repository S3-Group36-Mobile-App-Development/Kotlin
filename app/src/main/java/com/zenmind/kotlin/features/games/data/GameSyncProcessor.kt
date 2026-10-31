package com.zenmind.kotlin.features.games.data

import java.io.IOException
import kotlinx.coroutines.CancellationException

data class GameSessionAuth(val owner: GameOwner, val token: String)

enum class GameSyncOutcome { Done, Retry }

/** One account's pending outbox. No game UI or WorkManager dependencies. */
class GameSyncProcessor(
    private val dao: GameSessionDao,
    private val api: GameApiService,
    private val auth: suspend () -> GameSessionAuth?
) {
    suspend fun sync(ownerId: String): GameSyncOutcome {
        if (auth()?.owner != GameOwner(ownerId, false)) return GameSyncOutcome.Done

        for (session in dao.pendingForOwner(ownerId)) {
            val current = auth() ?: return GameSyncOutcome.Done
            if (current.owner != GameOwner(ownerId, false)) return GameSyncOutcome.Done

            val response = try {
                api.registerSession("Bearer ${current.token}", session.toRequest())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                return GameSyncOutcome.Retry
            } catch (e: Exception) {
                return GameSyncOutcome.Retry
            }

            // The account may have changed while the HTTP call was in flight.
            val afterRequest = auth() ?: return GameSyncOutcome.Done
            if (afterRequest.owner != current.owner) return GameSyncOutcome.Done
            if (response.code() == 401 && afterRequest.token != current.token) {
                return GameSyncOutcome.Retry // A refreshed token may now work.
            }

            when {
                response.isSuccessful && response.body()?.id == session.id ->
                    dao.markSynced(session.id, ownerId)
                response.isSuccessful -> return GameSyncOutcome.Retry // No valid acknowledgement.
                response.code() == 401 -> {
                    dao.blockPendingForOwner(ownerId, GameSyncStatus.AUTH_REQUIRED)
                    return GameSyncOutcome.Done
                }
                response.code() == 403 -> {
                    dao.blockPendingForOwner(ownerId, GameSyncStatus.CONSENT_REQUIRED)
                    return GameSyncOutcome.Done
                }
                response.code() == 408 || response.code() == 429 || response.code() >= 500 ->
                    return GameSyncOutcome.Retry
                response.code() in 400..499 ->
                    dao.markBlocked(session.id, ownerId, GameSyncStatus.REJECTED)
                else -> return GameSyncOutcome.Retry
            }
        }
        return GameSyncOutcome.Done
    }
}
