package com.zenmind.kotlin.features.games.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: GameSessionEntity): Long

    @Query("SELECT * FROM game_sessions WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): GameSessionEntity?

    @Query("SELECT * FROM game_sessions WHERE ownerId = :ownerId AND isGuest = :isGuest ORDER BY completedAt DESC, id DESC")
    suspend fun forOwner(ownerId: String, isGuest: Boolean): List<GameSessionEntity>

    @Query("SELECT * FROM game_sessions WHERE ownerId = :ownerId AND isGuest = :isGuest ORDER BY completedAt DESC, id DESC")
    fun observeForOwner(ownerId: String, isGuest: Boolean): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE ownerId = :ownerId AND isGuest = 0 AND syncStatus = 'pending' ORDER BY completedAt, id")
    suspend fun pendingForOwner(ownerId: String): List<GameSessionEntity>

    @Query("UPDATE game_sessions SET syncStatus = 'synced' WHERE id = :id AND ownerId = :ownerId AND isGuest = 0 AND syncStatus = 'pending'")
    suspend fun markSynced(id: String, ownerId: String): Int

    @Query("UPDATE game_sessions SET syncStatus = 'synced' WHERE id = :id AND ownerId = :ownerId AND isGuest = 0")
    suspend fun markConfirmedByHistory(id: String, ownerId: String): Int

    @Query("UPDATE game_sessions SET syncStatus = :status WHERE id = :id AND ownerId = :ownerId AND isGuest = 0 AND syncStatus = 'pending'")
    suspend fun markBlocked(id: String, ownerId: String, status: String): Int

    @Query("UPDATE game_sessions SET syncStatus = :status WHERE ownerId = :ownerId AND isGuest = 0 AND syncStatus = 'pending'")
    suspend fun blockPendingForOwner(ownerId: String, status: String): Int

    @Query("UPDATE game_sessions SET syncStatus = 'pending' WHERE ownerId = :ownerId AND isGuest = 0 AND syncStatus = 'blocked_auth'")
    suspend fun restoreAfterLogin(ownerId: String): Int
}
