package com.zenmind.kotlin.features.games.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val id: String,
    val durationMs: Int,
    val errors: Int,
    val correctTaps: Int,
    val completedAt: String,
    val syncStatus: String = GameSyncStatus.PENDING,
    val ownerId: String,
    val isGuest: Boolean
)

object GameSyncStatus {
    const val PENDING = "pending"
    const val SYNCED = "synced"
    const val AUTH_REQUIRED = "blocked_auth"
    const val CONSENT_REQUIRED = "blocked_consent"
    const val REJECTED = "blocked_invalid"
}
