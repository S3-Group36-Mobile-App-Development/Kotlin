package com.zenmind.kotlin.features.dailycheckin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CheckinDao {

    @Insert
    suspend fun insertCheckin(checkin: CheckinEntity): Long

    @Query("SELECT * FROM checkin_table WHERE date = :date AND userId = :userId LIMIT 1")
    suspend fun getByDate(date: String, userId: String): CheckinEntity?

    @Query("SELECT * FROM checkin_table WHERE synced = 0 AND isGuest = 0 AND userId = :userId ORDER BY id")
    suspend fun getPending(userId: String): List<CheckinEntity>

    @Query("UPDATE checkin_table SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
