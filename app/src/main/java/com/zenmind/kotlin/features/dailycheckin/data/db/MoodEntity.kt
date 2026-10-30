package com.zenmind.kotlin.features.dailycheckin.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_table")
data class MoodEntity(
    @PrimaryKey val id: Long,
    val name: String
)
