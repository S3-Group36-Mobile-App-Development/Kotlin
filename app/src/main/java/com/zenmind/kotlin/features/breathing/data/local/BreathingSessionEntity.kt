package com.zenmind.kotlin.features.breathing.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breathing_sessions")
data class BreathingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ejercicioId: Int,
    val completedAt: Long,
    val synced: Boolean = false
)
