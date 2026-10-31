package com.zenmind.kotlin.features.games.model

import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.GameSessionEntity

data class GameStatistics(
    val completedGames: Int = 0,
    val averageDurationMs: Long? = null,
    val bestDurationMs: Int? = null,
    val totalErrors: Long = 0,
    val accuracy: Double = 0.0
)

/** Pure CPU aggregation; the caller selects Dispatchers.Default for nontrivial histories. */
fun calculateGameStatistics(history: List<GameSessionEntity>, owner: GameOwner): GameStatistics {
    val games = history.asSequence()
        .filter { it.ownerId == owner.id && it.isGuest == owner.isGuest }
        .distinctBy { it.id }
        .toList()
    if (games.isEmpty()) return GameStatistics()

    var totalDurationMs = 0L
    var bestDurationMs = Int.MAX_VALUE
    var totalErrors = 0L
    var totalCorrectTaps = 0L
    games.forEach { game ->
        totalDurationMs += game.durationMs.toLong()
        bestDurationMs = minOf(bestDurationMs, game.durationMs)
        totalErrors += game.errors.toLong()
        totalCorrectTaps += game.correctTaps.toLong()
    }
    val allTaps = totalCorrectTaps + totalErrors
    return GameStatistics(
        completedGames = games.size,
        averageDurationMs = totalDurationMs / games.size,
        bestDurationMs = bestDurationMs,
        totalErrors = totalErrors,
        accuracy = if (allTaps == 0L) 0.0 else totalCorrectTaps.toDouble() / allTaps
    )
}
