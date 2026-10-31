package com.zenmind.kotlin.features.breathing.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BreathingSessionDao {
    
    @Insert
    suspend fun insert (
        session: BreathingSessionEntity
    ): Long

    @Query( "SELECT * FROM breathing_sessions WHERE synced = 0" )
    suspend fun getPending (): List<BreathingSessionEntity>

    @Query ( "UPDATE breathing_sessions SET synced = 1 WHERE id = :id" )
    suspend fun markSynced ( id: Long )

    @Query ( "SELECT COUNT(*) FROM breathing_sessions" )
    suspend fun countAll (): Int
} 