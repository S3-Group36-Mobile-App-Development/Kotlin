package com.zenmind.kotlin.features.dailycheckin.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkin_table")
data class CheckinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val moodId: Long,
    val moodName: String,
    val note: String?,
    val synced: Boolean,
    val isGuest: Boolean,
    val userId: String
)
