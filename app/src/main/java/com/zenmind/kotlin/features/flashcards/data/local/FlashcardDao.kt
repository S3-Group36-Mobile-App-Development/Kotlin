package com.zenmind.kotlin.features.flashcards.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FlashcardDao {

    @Query("SELECT * FROM flashcard_temas")
    suspend fun getAllTemas(): List<FlashcardTemaEntity>

    @Upsert
    suspend fun upsertTemas(temas: List<FlashcardTemaEntity>)

    @Query("SELECT * FROM flashcard_cards WHERE temaId = :temaId")
    suspend fun getCardsForTema(temaId: String): List<FlashcardCardEntity>

    @Upsert
    suspend fun upsertCards(cards: List<FlashcardCardEntity>)

    @Insert
    suspend fun insertReaction(reaction: FlashcardReactionEntity): Long

    @Query("SELECT * FROM pending_flashcard_reactions WHERE synced = 0")
    suspend fun getPendingReactions(): List<FlashcardReactionEntity>

    @Query("UPDATE pending_flashcard_reactions SET synced = 1 WHERE id = :id")
    suspend fun markReactionSynced(id: Long)
}
