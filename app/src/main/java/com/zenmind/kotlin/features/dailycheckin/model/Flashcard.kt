package com.zenmind.kotlin.features.dailycheckin.model

data class Flashcard(
    val category: String,
    val content: String,
    val source: String? = null
)
