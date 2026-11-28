package com.zenmind.kotlin.features.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.flashcards.data.FlashCardsRepository
import com.zenmind.kotlin.features.flashcards.model.FlashcardTemaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardsUiState(
    val temas: List<FlashcardTemaDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class FlashCardsViewModel(
    private val repository: FlashCardsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    init {
        loadTemas("INFORMATIVAS")
    }

    fun loadTemas(categoria: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val temas = repository.getTemas(categoria)
                _uiState.update {
                    it.copy(
                        temas = temas,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}
