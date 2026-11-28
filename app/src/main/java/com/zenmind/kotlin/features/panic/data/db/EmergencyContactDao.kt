package com.zenmind.kotlin.features.panic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmergencyContactDao {

    @Query("SELECT * FROM emergency_contact_table WHERE id = 1")
    suspend fun get(): EmergencyContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contact_table")
    suspend fun delete()
}
