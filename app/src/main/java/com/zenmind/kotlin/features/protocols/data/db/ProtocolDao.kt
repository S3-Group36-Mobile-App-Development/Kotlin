package com.zenmind.kotlin.features.protocols.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ProtocolDao {

    @Query("SELECT * FROM protocol_table WHERE userId = :userId ORDER BY esPredefinido DESC, id")
    suspend fun getAll(userId: String): List<ProtocolEntity>

    @Query("SELECT * FROM protocol_table WHERE id = :id AND userId = :userId")
    suspend fun getById(id: Long, userId: String): ProtocolEntity?

    @Query("SELECT * FROM protocol_table WHERE userId = :userId AND synced = 0")
    suspend fun getPending(userId: String): List<ProtocolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(protocol: ProtocolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(protocols: List<ProtocolEntity>)

    @Query("DELETE FROM protocol_table WHERE id = :id AND userId = :userId")
    suspend fun delete(id: Long, userId: String)

    @Query("DELETE FROM protocol_table WHERE userId = :userId AND synced = 1")
    suspend fun deleteSynced(userId: String)

    @Transaction
    suspend fun replaceSynced(userId: String, protocols: List<ProtocolEntity>) {
        deleteSynced(userId)
        insertAll(protocols)
    }
}
