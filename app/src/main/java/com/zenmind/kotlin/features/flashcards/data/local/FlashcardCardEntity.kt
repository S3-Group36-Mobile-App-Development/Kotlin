package com.zenmind.kotlin.features.flashcards.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcard_cards")
data class FlashcardCardEntity(
    @PrimaryKey
    val id: String,
    val temaId: String,
    val titulo: String,
    val contenido: String,
    val fuente: String,
    val likeCount: Int,
    val dislikeCount: Int
)
