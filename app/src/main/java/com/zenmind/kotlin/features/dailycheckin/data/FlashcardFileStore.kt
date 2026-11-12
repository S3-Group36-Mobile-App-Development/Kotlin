package com.zenmind.kotlin.features.dailycheckin.data

import com.google.gson.Gson
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard
import java.io.File

class FlashcardFileStore(directory: File) {
    private val file = File(directory, "flashcards.json")
    private val gson = Gson()

    fun save(category: String, flashcard: Flashcard) {
        try {
            val saved = readAll() + (category to flashcard)
            file.writeText(gson.toJson(SavedFlashcards(saved)))
        } catch (e: Exception) {
        }
    }

    fun read(category: String): Flashcard? = readAll()[category]

    private fun readAll(): Map<String, Flashcard> {
        return try {
            if (!file.exists()) return emptyMap()
            gson.fromJson(file.readText(), SavedFlashcards::class.java).byCategory
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private data class SavedFlashcards(val byCategory: Map<String, Flashcard>)
}
