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

data class FlashcardDetailUiState(
    val tema: FlashcardTemaDto? = null,
    val userReactions: Map<String, String> = emptyMap(), // flashcardId -> tipo ("LIKE" o "DISLIKE")
    val isLoading: Boolean = true
)

class FlashcardDetailViewModel(
    private val repository: FlashCardsRepository,
    private val temaId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(FlashcardDetailUiState())
    val uiState: StateFlow<FlashcardDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tema = repository.getTemaDetail(temaId)
            val reactions = if (token.isNotBlank()) repository.getReacciones(token) else emptyMap()
            android.util.Log.d("FlashcardDetailVM", "loadDetail loaded tema=${tema?.titulo}, reactionsCount=${reactions.size}")
            _uiState.update {
                it.copy(
                    tema = tema,
                    userReactions = reactions,
                    isLoading = false
                )
            }
        }
    }

    fun reactToCard(cardId: String, tipo: String, token: String) {
        // Actualización optimista inmediata en UI
        _uiState.update { current ->
            val updated = current.userReactions.toMutableMap()
            updated[cardId] = tipo
            current.copy(userReactions = updated)
        }

        viewModelScope.launch {
            val success = if (tipo == "LIKE") {
                repository.likeCard(cardId, token)
            } else {
                repository.dislikeCard(cardId, token)
            }
            android.util.Log.d("FlashcardDetailVM", "reactToCard cardId=$cardId, tipo=$tipo, success=$success")
            if (!success && token.isNotBlank()) {
                val reactions = repository.getReacciones(token)
                _uiState.update { it.copy(userReactions = reactions) }
            }
        }
    }

    fun removeReaction(cardId: String, token: String) {
        // Actualización optimista inmediata en UI
        _uiState.update { current ->
            val updated = current.userReactions.toMutableMap()
            updated.remove(cardId)
            current.copy(userReactions = updated)
        }

        viewModelScope.launch {
            val success = repository.removeReaction(cardId, token)
            android.util.Log.d("FlashcardDetailVM", "removeReaction cardId=$cardId, success=$success")
            if (!success && token.isNotBlank()) {
                val reactions = repository.getReacciones(token)
                _uiState.update { it.copy(userReactions = reactions) }
            }
        }
    }
}
