package com.zenmind.kotlin.features.flashcards.data

import com.zenmind.kotlin.features.flashcards.data.local.FlashcardCardEntity
import com.zenmind.kotlin.features.flashcards.data.local.FlashcardTemaEntity
import com.zenmind.kotlin.features.flashcards.model.FlashcardCardDto
import com.zenmind.kotlin.features.flashcards.model.FlashcardTemaDto

fun FlashcardTemaDto.toEntity(): FlashcardTemaEntity = FlashcardTemaEntity(
    id = id,
    titulo = titulo,
    categoria = categoria,
    iconType = iconType,
    cardsCount = cardsCount,
    totalLikes = totalLikes,
    totalDislikes = totalDislikes
)

fun FlashcardTemaEntity.toDto(): FlashcardTemaDto = FlashcardTemaDto(
    id = id,
    titulo = titulo,
    categoria = categoria,
    iconType = iconType,
    cardsCount = cardsCount,
    totalLikes = totalLikes,
    totalDislikes = totalDislikes,
    cards = null
)

fun FlashcardCardDto.toEntity(): FlashcardCardEntity = FlashcardCardEntity(
    id = id,
    temaId = temaId,
    titulo = titulo,
    contenido = contenido,
    fuente = fuente,
    likeCount = likeCount,
    dislikeCount = dislikeCount
)

fun FlashcardCardEntity.toDto(): FlashcardCardDto = FlashcardCardDto(
    id = id,
    temaId = temaId,
    titulo = titulo,
    contenido = contenido,
    fuente = fuente,
    likeCount = likeCount,
    dislikeCount = dislikeCount
)
