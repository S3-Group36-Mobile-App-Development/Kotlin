package com.zenmind.kotlin.features.breathing.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breathing_exercises")
data class BreathingExerciseEntity (
    @PrimaryKey val id: Int,
    val titulo: String,
    val descripcion: String?,
    val duracion: Int,
    val inhalar: Int?,
    val sostener: Int?,
    val exhalar: Int?,
    val sostener2: Int?,
    val ciclos: Int,
    val vibra: Boolean
)