package com.zenmind.kotlin.features.games.data

import android.util.Base64
import com.zenmind.kotlin.core.network.ApiClient
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.games.model.GamePhase
import com.zenmind.kotlin.features.games.model.GameState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GameOwner(val id: String, val isGuest: Boolean) {
    companion object {
        val Guest = GameOwner("guest", true)
    }
}

fun completedSession(
    state: GameState,
    owner: GameOwner,
    id: String = UUID.randomUUID().toString(),
    completedAt: Date = Date()
): GameSessionEntity {
    require(state.phase == GamePhase.Finished && state.correctTaps == 20)
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return GameSessionEntity(
        id = id,
        durationMs = state.elapsedMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        errors = state.errors,
        correctTaps = state.correctTaps,
        completedAt = formatter.format(completedAt),
        ownerId = owner.id,
        isGuest = owner.isGuest
    )
}

class GameSessionRepository(
    private val dao: GameSessionDao,
    private val tokenStorage: TokenStorage
) {
    suspend fun currentOwner(): GameOwner = withContext(Dispatchers.IO) {
        val token = tokenStorage.getAccessToken() ?: return@withContext GameOwner.Guest
        ownerFromToken(token)
    }

    suspend fun save(session: GameSessionEntity) = withContext(Dispatchers.IO) {
        // The UUID is stable for this completion; a retry cannot create a second row.
        if (dao.insert(session) == -1L && dao.byId(session.id) != session) {
            throw IllegalStateException("Este identificador ya corresponde a otra partida.")
        }
    }

    suspend fun history(owner: GameOwner): List<GameSessionEntity> = withContext(Dispatchers.IO) {
        dao.forOwner(owner.id, owner.isGuest)
    }

    fun observeHistory(owner: GameOwner): Flow<List<GameSessionEntity>> =
        dao.observeForOwner(owner.id, owner.isGuest)

    suspend fun refreshFromBackend(owner: GameOwner): Boolean {
        if (owner.isGuest) return false
        return GameHistoryRefresher(dao, ApiClient.retrofit.create(GameApiService::class.java)) {
            val token = withContext(Dispatchers.IO) { tokenStorage.getAccessToken() } ?: return@GameHistoryRefresher null
            val currentOwner = runCatching { ownerFromToken(token) }.getOrNull() ?: return@GameHistoryRefresher null
            GameSessionAuth(currentOwner, token)
        }.refresh(owner)
    }
}

fun ownerFromToken(token: String): GameOwner = try {
    val payload = token.split(".")[1]
    val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
    val id = JSONObject(decoded).getString("sub")
    require(id.isNotBlank())
    GameOwner(id, false)
} catch (e: Exception) {
    throw IllegalStateException("No se pudo identificar tu cuenta para guardar la partida.", e)
}
