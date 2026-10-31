package com.zenmind.kotlin

import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.completedSession
import com.zenmind.kotlin.features.games.model.GamePhase
import com.zenmind.kotlin.features.games.model.GameState
import java.util.Date
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionRecordTest {
    @Test fun completedGameHasBackendCompatibleIdUtcDateAndGuestOwnership() {
        val session = completedSession(
            GameState(phase = GamePhase.Finished, correctTaps = 20, errors = 3, elapsedMs = 45210),
            GameOwner.Guest,
            completedAt = Date(0)
        )

        assertEquals(session.id, UUID.fromString(session.id).toString())
        assertEquals("1970-01-01T00:00:00.000Z", session.completedAt)
        assertEquals(45210, session.durationMs)
        assertEquals(3, session.errors)
        assertEquals(20, session.correctTaps)
        assertEquals("pending", session.syncStatus)
        assertEquals("guest", session.ownerId)
        assertTrue(session.isGuest)
    }

    @Test fun accountOwnerIsNotGuestAndUnfinishedGameIsRejected() {
        val account = completedSession(
            GameState(phase = GamePhase.Finished, correctTaps = 20),
            GameOwner("user-1", false), completedAt = Date(0)
        )
        assertEquals("user-1", account.ownerId)
        assertFalse(account.isGuest)

        try {
            completedSession(GameState(phase = GamePhase.Running), GameOwner.Guest)
            throw AssertionError("Unfinished game was accepted")
        } catch (_: IllegalArgumentException) {
            // Only a finished game can be stored.
        }
    }
}
