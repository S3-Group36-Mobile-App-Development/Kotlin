package com.zenmind.kotlin.features.flashcards.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_flashcard_reactions")
data class FlashcardReactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val flashcardId: String,
    val tipo: String,
    val synced: Boolean = false
)