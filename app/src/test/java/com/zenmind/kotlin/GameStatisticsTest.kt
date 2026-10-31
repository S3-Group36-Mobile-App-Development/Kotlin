package com.zenmind.kotlin

import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.GameSessionEntity
import com.zenmind.kotlin.features.games.data.GameSyncStatus
import com.zenmind.kotlin.features.games.model.calculateGameStatistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStatisticsTest {
    private val owner = GameOwner("42", false)

    private fun game(id: String, duration: Int, mistakes: Int, hits: Int = 20,
                     player: GameOwner = owner, status: String = GameSyncStatus.PENDING) =
        GameSessionEntity(id, duration, mistakes, hits, "2026-10-31T12:00:00.000Z",
            status, player.id, player.isGuest)

    @Test fun emptyHistoryHasNoAverageOrBestAndZeroPrecision() {
        val result = calculateGameStatistics(emptyList(), owner)
        assertEquals(0, result.completedGames)
        assertNull(result.averageDurationMs)
        assertNull(result.bestDurationMs)
        assertEquals(0L, result.totalErrors)
        assertEquals(0.0, result.accuracy, 0.0)
    }

    @Test fun singleCompletedGameUsesItsOwnMillisecondsAndHits() {
        val result = calculateGameStatistics(listOf(game("one", 45123, 5)), owner)
        assertEquals(1, result.completedGames)
        assertEquals(45123L, result.averageDurationMs)
        assertEquals(45123, result.bestDurationMs)
        assertEquals(5L, result.totalErrors)
        assertEquals(20.0 / 25.0, result.accuracy, 0.000001)
    }

    @Test fun severalGamesCalculateAverageBestTotalErrorsAndGlobalAccuracy() {
        val result = calculateGameStatistics(listOf(
            game("a", 1500, 1), game("b", 2500, 3), game("c", 60000, 6)
        ), owner)
        assertEquals(3, result.completedGames)
        assertEquals(64000L / 3, result.averageDurationMs)
        assertEquals(1500, result.bestDurationMs)
        assertEquals(10L, result.totalErrors)
        assertEquals(60.0 / 70.0, result.accuracy, 0.000001)
    }

    @Test fun zeroDenominatorIsHandledAndAccountsNeverMix() {
        val result = calculateGameStatistics(listOf(
            game("empty-taps", 0, 0, hits = 0),
            game("other", 9000, 5, player = GameOwner("7", false)),
            game("guest", 8000, 2, player = GameOwner.Guest)
        ), owner)
        assertEquals(1, result.completedGames)
        assertEquals(0.0, result.accuracy, 0.0)
        assertEquals(0L, result.totalErrors)
        assertEquals(1, calculateGameStatistics(listOf(game("guest", 8000, 2, player = GameOwner.Guest)),
            GameOwner.Guest).completedGames)
    }

    @Test fun localAndRemoteCopyOfSameUuidAreCountedOnce() {
        val local = game("same-uuid", 3000, 2)
        val remote = local.copy(syncStatus = GameSyncStatus.SYNCED)
        val result = calculateGameStatistics(listOf(local, remote), owner)
        assertEquals(1, result.completedGames)
        assertEquals(3000L, result.averageDurationMs)
        assertEquals(2L, result.totalErrors)
        assertEquals(20.0 / 22.0, result.accuracy, 0.000001)
    }
}
