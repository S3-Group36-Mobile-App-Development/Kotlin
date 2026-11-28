package com.zenmind.kotlin.features.flashcards.model

data class FlashcardCardDto(
    val id: String,
    val temaId: String,
    val titulo: String,
    val contenido: String,
    val fuente: String,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0
)

data class FlashcardTemaDto(
    val id: String,
    val titulo: String,
    val categoria: String,
    val iconType: String,
    val cardsCount: Int = 0,
    val totalLikes: Int = 0,
    val totalDislikes: Int = 0,
    val cards: List<FlashcardCardDto>? = null
)

data class ReaccionRequest(
    val tipo: String // "LIKE" o "DISLIKE"
)

data class ReaccionResponse(
    val id: String,
    val usuarioId: String,
    val flashcardId: String,
    val tipo: String,
    val creadaEn: String,
    val actualizadaEn: String
)

data class UserReactionDto(
    val id: String,
    val usuarioId: String,
    val flashcardId: String,
    val tipo: String, // "LIKE" o "DISLIKE"
    val creadaEn: String,
    val actualizadaEn: String
)

data class AnaliticaCardDto(
    val id: String,
    val titulo: String,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0
)

data class AnaliticaTemaDto(
    val tema: String,
    val categoria: String,
    val totalLikes: Int = 0,
    val totalDislikes: Int = 0,
    val cards: List<AnaliticaCardDto> = emptyList()
)
