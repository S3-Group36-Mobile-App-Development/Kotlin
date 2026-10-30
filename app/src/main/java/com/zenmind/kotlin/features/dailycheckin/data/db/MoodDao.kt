package com.zenmind.kotlin.features.dailycheckin.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MoodDao {

    @Query("SELECT * FROM mood_table ORDER BY id")
    suspend fun getAll(): List<MoodEntity>

    @Upsert
    suspend fun upsertAll(moods: List<MoodEntity>)
}
