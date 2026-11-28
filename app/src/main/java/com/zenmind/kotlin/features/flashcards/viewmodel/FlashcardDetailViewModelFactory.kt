package com.zenmind.kotlin.features.flashcards.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.features.flashcards.data.FlashCardsRepository

class FlashcardDetailViewModelFactory(
    private val context: Context,
    private val temaId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        val repository = FlashCardsRepository(
            dao = db.flashcardDao(),
            context = appContext
        )
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(FlashcardDetailViewModel::class.java) ->
                FlashcardDetailViewModel(repository, temaId) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
