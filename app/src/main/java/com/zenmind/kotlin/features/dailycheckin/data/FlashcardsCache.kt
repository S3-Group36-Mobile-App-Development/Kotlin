package com.zenmind.kotlin.features.dailycheckin.data

import androidx.collection.LruCache
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard

object FlashcardsCache {
    private val cache = LruCache<String, List<Flashcard>>(5)

    fun saveFlashcards(category: String, flashcards: List<Flashcard>){
        cache.put(category, flashcards)
    }

    fun getFlashcards(category: String): List<Flashcard>? {
        return cache[category]
    }
}