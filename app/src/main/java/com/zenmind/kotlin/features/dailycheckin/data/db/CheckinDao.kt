package com.zenmind.kotlin.features.dailycheckin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CheckinDao {

    @Insert
    suspend fun insertCheckin(checkin: CheckinEntity): Long

    @Query("SELECT * FROM checkin_table WHERE date = :date AND isGuest = :isGuest LIMIT 1")
    suspend fun getByDate(date: String, isGuest: Boolean): CheckinEntity?

    @Query("SELECT * FROM checkin_table WHERE synced = 0 AND isGuest = 0 ORDER BY id")
    suspend fun getPending(): List<CheckinEntity>

    @Query("UPDATE checkin_table SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
