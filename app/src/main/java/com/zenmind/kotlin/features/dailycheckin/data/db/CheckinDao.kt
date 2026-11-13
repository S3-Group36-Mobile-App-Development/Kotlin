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

    @Query("SELECT DISTINCT date FROM checkin_table WHERE userId = :userId ORDER BY date DESC")
    suspend fun getDates(userId: String): List<String>

    @Query("UPDATE checkin_table SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE checkin_table SET moodId = :moodId, moodName = :moodName, note = :note, synced = 1 WHERE id = :id")
    suspend fun replaceWithServer(id: Long, moodId: Long, moodName: String, note: String?)
}
