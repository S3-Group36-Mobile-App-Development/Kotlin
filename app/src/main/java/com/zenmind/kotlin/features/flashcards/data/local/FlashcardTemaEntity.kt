package com.zenmind.kotlin.features.flashcards.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcard_temas")
data class FlashcardTemaEntity(
    @PrimaryKey
    val id: String,
    val titulo: String,
    val categoria: String,
    val iconType: String,
    val cardsCount: Int,
    val totalLikes: Int,
    val totalDislikes: Int
)
