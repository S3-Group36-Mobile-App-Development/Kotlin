package com.zenmind.kotlin.features.breathing.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BreathingExerciseDao {

    @Upsert
    suspend fun upsertAll(exercises: List<BreathingExerciseEntity>)

    @Query("SELECT * FROM breathing_exercises ORDER BY id")
    suspend fun getAll(): List<BreathingExerciseEntity>
}
