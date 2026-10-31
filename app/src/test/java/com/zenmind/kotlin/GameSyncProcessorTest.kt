package com.zenmind.kotlin

import com.google.gson.Gson
import com.zenmind.kotlin.features.games.data.GameApiService
import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.GameSessionAck
import com.zenmind.kotlin.features.games.data.GameSessionAuth
import com.zenmind.kotlin.features.games.data.GameSessionDao
import com.zenmind.kotlin.features.games.data.GameSessionEntity
import com.zenmind.kotlin.features.games.data.GameSessionRequest
import com.zenmind.kotlin.features.games.data.RemoteGameSession
import com.zenmind.kotlin.features.games.data.GameSyncOutcome
import com.zenmind.kotlin.features.games.data.GameSyncProcessor
import com.zenmind.kotlin.features.games.data.GameSyncStatus
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class GameSyncProcessorTest {
    private val account = GameOwner("42", false)
    private val dao = FakeDao(mutableListOf(session("first")))
    private val api = FakeApi()
    private var signedIn: GameSessionAuth? = GameSessionAuth(account, "access-42")
    private fun processor() = GameSyncProcessor(dao, api) { signedIn }

    @Test fun timeoutLeavesPendingAndRetryUsesSameUuidAndOnlyFiveBodyFields() = runTest {
        api.reply = { _, _ -> throw IOException("offline") }
        assertEquals(GameSyncOutcome.Retry, processor().sync("42"))
        assertEquals(GameSyncStatus.PENDING, dao.sessions.single().syncStatus)

        api.reply = { _, request -> Response.success(GameSessionAck(request.id)) }
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        assertEquals(listOf("first", "first"), api.requests.map { it.id })
        assertEquals(listOf("Bearer access-42", "Bearer access-42"), api.headers)
        assertEquals(GameSyncStatus.SYNCED, dao.sessions.single().syncStatus)
        assertEquals(1, dao.sessions.size) // Local history is retained.
        val fields = Gson().toJsonTree(api.requests.last()).asJsonObject.keySet()
        assertEquals(setOf("id", "durationMs", "errors", "correctTaps", "completedAt"), fields)
    }

    @Test fun guestOrAnotherAccountCannotSendOwnerSessions() = runTest {
        signedIn = null
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        signedIn = GameSessionAuth(GameOwner("7", false), "access-7")
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        signedIn = GameSessionAuth(GameOwner.Guest, "guest")
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        assertTrue(api.requests.isEmpty())
        assertEquals(GameSyncStatus.PENDING, dao.sessions.single().syncStatus)
    }

    @Test fun logoutOrAccountChangeDuringRequestDoesNotMarkSyncedOrSendNext() = runTest {
        dao.sessions.add(session("second"))
        api.reply = { _, request ->
            signedIn = GameSessionAuth(GameOwner("7", false), "access-7")
            Response.success(GameSessionAck(request.id))
        }
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        assertEquals(1, api.requests.size)
        assertTrue(dao.sessions.all { it.syncStatus == GameSyncStatus.PENDING })
    }

    @Test fun invalidOrEmptyConfirmationCannotMarkSynced() = runTest {
        api.reply = { _, _ -> Response.success(GameSessionAck("other")) }
        assertEquals(GameSyncOutcome.Retry, processor().sync("42"))
        assertEquals(GameSyncStatus.PENDING, dao.sessions.single().syncStatus)
        api.reply = { _, _ -> Response.success(null) }
        assertEquals(GameSyncOutcome.Retry, processor().sync("42"))
        assertEquals(GameSyncStatus.PENDING, dao.sessions.single().syncStatus)
    }

    @Test fun temporaryHttpFailuresRetryWithoutChangingLocalRow() = runTest {
        for (code in listOf(408, 429, 500, 503)) {
            api.reply = { _, _ -> error(code) }
            assertEquals(GameSyncOutcome.Retry, processor().sync("42"))
            assertEquals(GameSyncStatus.PENDING, dao.sessions.single().syncStatus)
        }
    }

    @Test fun rejectedCredentialsConsentAndBadRequestDoNotRetry() = runTest {
        dao.sessions.add(session("second"))
        api.reply = { _, _ -> error(401) }
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        assertTrue(dao.sessions.all { it.syncStatus == GameSyncStatus.AUTH_REQUIRED })

        dao.sessions.replaceAll { it.copy(syncStatus = GameSyncStatus.PENDING) }
        api.reply = { _, _ -> error(403) }
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        assertTrue(dao.sessions.all { it.syncStatus == GameSyncStatus.CONSENT_REQUIRED })

        dao.sessions.replaceAll { it.copy(syncStatus = GameSyncStatus.PENDING) }
        api.reply = { _, request -> if (request.id == "first") error(400) else Response.success(GameSessionAck(request.id)) }
        assertEquals(GameSyncOutcome.Done, processor().sync("42"))
        assertEquals(GameSyncStatus.REJECTED, dao.sessions.first().syncStatus)
        assertEquals(GameSyncStatus.SYNCED, dao.sessions.last().syncStatus)
    }

    @Test fun newTokenForSameUserMayRetryRatherThanBlockOnOldToken401() = runTest {
        api.reply = { _, _ ->
            signedIn = GameSessionAuth(account, "new-token")
            error(401)
        }
        assertEquals(GameSyncOutcome.Retry, processor().sync("42"))
        assertEquals(GameSyncStatus.PENDING, dao.sessions.single().syncStatus)
    }

    private fun session(id: String) = GameSessionEntity(id, 5000, 2, 20,
        "2026-10-31T12:00:00.000Z", ownerId = "42", isGuest = false)

    private fun error(code: Int): Response<GameSessionAck> = Response.error(code, "error".toResponseBody())

    private class FakeApi : GameApiService {
        override suspend fun listSessions(authorization: String): Response<List<RemoteGameSession>> =
            Response.success(emptyList())
        val requests = mutableListOf<GameSessionRequest>()
        val headers = mutableListOf<String>()
        var reply: suspend (String, GameSessionRequest) -> Response<GameSessionAck> =
            { _, request -> Response.success(GameSessionAck(request.id)) }

        override suspend fun registerSession(authorization: String, request: GameSessionRequest): Response<GameSessionAck> {
            headers.add(authorization)
            requests.add(request)
            return reply(authorization, request)
        }
    }

    private class FakeDao(val sessions: MutableList<GameSessionEntity>) : GameSessionDao {
        override suspend fun insert(session: GameSessionEntity): Long = 0L
        override suspend fun byId(id: String): GameSessionEntity? = sessions.find { it.id == id }
        override suspend fun forOwner(ownerId: String, isGuest: Boolean) =
            sessions.filter { it.ownerId == ownerId && it.isGuest == isGuest }
        override fun observeForOwner(ownerId: String, isGuest: Boolean): Flow<List<GameSessionEntity>> =
            flowOf(sessions.filter { it.ownerId == ownerId && it.isGuest == isGuest })
        override suspend fun pendingForOwner(ownerId: String) =
            sessions.filter { it.ownerId == ownerId && !it.isGuest && it.syncStatus == GameSyncStatus.PENDING }
        override suspend fun markSynced(id: String, ownerId: String): Int = change(id, ownerId, GameSyncStatus.SYNCED)
        override suspend fun markConfirmedByHistory(id: String, ownerId: String): Int {
            val index = sessions.indexOfFirst { it.id == id && it.ownerId == ownerId && !it.isGuest }
            if (index == -1) return 0
            sessions[index] = sessions[index].copy(syncStatus = GameSyncStatus.SYNCED)
            return 1
        }
        override suspend fun markBlocked(id: String, ownerId: String, status: String): Int = change(id, ownerId, status)
        override suspend fun blockPendingForOwner(ownerId: String, status: String): Int {
            val pending = pendingForOwner(ownerId)
            pending.forEach { change(it.id, ownerId, status) }
            return pending.size
        }
        override suspend fun restoreAfterLogin(ownerId: String): Int {
            val blocked = sessions.filter { it.ownerId == ownerId && !it.isGuest && it.syncStatus == GameSyncStatus.AUTH_REQUIRED }
            blocked.forEach { item -> sessions[sessions.indexOf(item)] = item.copy(syncStatus = GameSyncStatus.PENDING) }
            return blocked.size
        }
        private fun change(id: String, ownerId: String, status: String): Int {
            val index = sessions.indexOfFirst { it.id == id && it.ownerId == ownerId && !it.isGuest && it.syncStatus == GameSyncStatus.PENDING }
            if (index == -1) return 0
            sessions[index] = sessions[index].copy(syncStatus = status)
            return 1
        }
    }
}
