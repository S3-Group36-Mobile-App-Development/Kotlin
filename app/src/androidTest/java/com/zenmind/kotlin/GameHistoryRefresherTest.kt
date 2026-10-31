package com.zenmind.kotlin

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.features.games.data.GameApiService
import com.zenmind.kotlin.features.games.data.GameHistoryRefresher
import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.GameSessionAck
import com.zenmind.kotlin.features.games.data.GameSessionAuth
import com.zenmind.kotlin.features.games.data.GameSessionEntity
import com.zenmind.kotlin.features.games.data.GameSessionRequest
import com.zenmind.kotlin.features.games.data.GameSyncStatus
import com.zenmind.kotlin.features.games.data.RemoteGameSession
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class GameHistoryRefresherTest {
    private val owner = GameOwner("42", false)
    private val sameId = "550e8400-e29b-41d4-a716-446655440000"
    private val otherId = "228f0a18-7810-4dad-acab-a48fbc03be29"
    private val date = "2026-10-31T12:00:00.000Z"

    @Test fun remoteHistoryMergesByUuidWithoutLosingLocalOrGuestGames() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.gameSessionDao()
            dao.insert(GameSessionEntity(sameId, 5000, 1, 20, date, ownerId = owner.id, isGuest = false))
            dao.insert(GameSessionEntity("guest-game", 9000, 0, 20, date,
                ownerId = GameOwner.Guest.id, isGuest = true))
            var identity: GameSessionAuth? = GameSessionAuth(owner, "token-42")
            val api = FakeApi(Response.success(listOf(
                remote(sameId), remote(otherId),
                remote("f22b622f-f9d0-4678-b6a6-485d448ee44d", userId = "7")
            )))
            val refresher = GameHistoryRefresher(dao, api) { identity }
            assertTrue(refresher.refresh(owner))
            assertEquals(2, dao.forOwner(owner.id, false).size)
            assertTrue(dao.forOwner(owner.id, false).all { it.syncStatus == GameSyncStatus.SYNCED })
            assertEquals(1, dao.forOwner("guest", true).size)
            assertEquals(GameSyncStatus.PENDING, dao.forOwner("guest", true).single().syncStatus)
            assertTrue(dao.forOwner("7", false).isEmpty())
            assertEquals(listOf("Bearer token-42"), api.headers)

            assertTrue(refresher.refresh(owner)) // Same server history cannot create duplicate IDs.
            assertEquals(2, dao.forOwner(owner.id, false).size)

            api.reply = { Response.error(503, "unavailable".toResponseBody()) }
            assertFalse(refresher.refresh(owner))
            assertEquals(2, dao.forOwner(owner.id, false).size) // Offline/cache remains intact.

            identity = GameSessionAuth(GameOwner("7", false), "token-7")
            assertFalse(refresher.refresh(owner))
            assertEquals(2, dao.forOwner(owner.id, false).size)
        } finally {
            db.close()
        }
    }

    @Test fun accountSwitchDuringGetDoesNotImportData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            var identity: GameSessionAuth? = GameSessionAuth(owner, "token-42")
            val api = FakeApi(Response.success(listOf(remote(otherId))))
            api.reply = {
                identity = GameSessionAuth(GameOwner("7", false), "token-7")
                Response.success(listOf(remote(otherId)))
            }
            assertFalse(GameHistoryRefresher(db.gameSessionDao(), api) { identity }.refresh(owner))
            assertTrue(db.gameSessionDao().forOwner(owner.id, false).isEmpty())
        } finally {
            db.close()
        }
    }

    private fun remote(id: String, userId: String = owner.id) = RemoteGameSession(
        id = id, usuarioId = userId, durationMs = 5000, errors = 1,
        correctTaps = 20, completedAt = date
    )

    private class FakeApi(private val initial: Response<List<RemoteGameSession>>) : GameApiService {
        val headers = mutableListOf<String>()
        var reply: suspend () -> Response<List<RemoteGameSession>> = { initial }
        override suspend fun listSessions(authorization: String): Response<List<RemoteGameSession>> {
            headers.add(authorization)
            return reply()
        }
        override suspend fun registerSession(authorization: String, request: GameSessionRequest): Response<GameSessionAck> =
            error("POST is not needed for the history refresh test")
    }
}
