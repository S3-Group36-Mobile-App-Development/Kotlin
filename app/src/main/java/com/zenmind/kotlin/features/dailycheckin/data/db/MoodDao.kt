package com.zenmind.kotlin.features.dailycheckin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MoodDao {

    @Query("SELECT * FROM mood_table ORDER BY sortOrder, id")
    suspend fun getAll(): List<MoodEntity>

    @Query("DELETE FROM mood_table")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(moods: List<MoodEntity>)

    @Transaction
    suspend fun replaceAll(moods: List<MoodEntity>) {
        deleteAll()
        insertAll(moods)
    }
}
