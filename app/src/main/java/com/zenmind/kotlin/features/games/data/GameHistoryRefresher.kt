package com.zenmind.kotlin.features.games.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Imports only sessions confirmed by the authenticated GET for the same owner. */
class GameHistoryRefresher(
    private val dao: GameSessionDao,
    private val api: GameApiService,
    private val auth: suspend () -> GameSessionAuth?
) {
    suspend fun refresh(owner: GameOwner): Boolean = withContext(Dispatchers.IO) {
        if (owner.isGuest) return@withContext false
        val before = auth() ?: return@withContext false
        if (before.owner != owner) return@withContext false

        val response = api.listSessions("Bearer ${before.token}")
        if (!response.isSuccessful) return@withContext false
        val remote = response.body() ?: return@withContext false
        for (item in remote) {
            if (auth()?.owner != owner) return@withContext false
            val received = item.asLocal(owner) ?: continue
            val existing = dao.byId(received.id)
            if (existing == null) {
                if (dao.insert(received) != -1L) continue
            }
            val local = existing ?: dao.byId(received.id) ?: continue
            if (local.ownerId == owner.id && !local.isGuest &&
                local.durationMs == received.durationMs && local.errors == received.errors &&
                local.correctTaps == received.correctTaps && local.completedAt == received.completedAt) {
                dao.markConfirmedByHistory(local.id, owner.id)
            }
        }
        true
    }
}

private fun RemoteGameSession.asLocal(owner: GameOwner): GameSessionEntity? {
    val uuid = id ?: return null
    if (usuarioId != owner.id ||
        runCatching { UUID.fromString(uuid).toString().equals(uuid, ignoreCase = true) }.getOrDefault(false) != true
    ) return null
    val duration = durationMs?.takeIf { it >= 0 } ?: return null
    val mistakes = errors?.takeIf { it >= 0 } ?: return null
    val hits = correctTaps?.takeIf { it >= 0 } ?: return null
    val date = completedAt ?: return null
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    if (runCatching { format.format(format.parse(date)!!) }.getOrNull() != date) return null
    return GameSessionEntity(uuid, duration, mistakes, hits, date,
        syncStatus = GameSyncStatus.SYNCED, ownerId = owner.id, isGuest = false)
}
