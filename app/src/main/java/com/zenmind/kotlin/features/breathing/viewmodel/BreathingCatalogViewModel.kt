package com.zenmind.kotlin.features.breathing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.breathing.data.BreathingRepository
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val exercises: List<BreathingExercise> = emptyList(),
    val favoriteId: Int? = null,
    val isLoading: Boolean = true
)

class BreathingCatalogViewModel (
    private val repository: BreathingRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    fun loadExercises() {
        viewModelScope.launch {
            val exercises = repository.getExercises()
            _uiState.update {
                it.copy(
                    exercises = exercises,
                    favoriteId = repository.getFavorite(),
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite(ejercicioId: Int) {
        val current = _uiState.value.favoriteId
        if ( current == ejercicioId ) {
            repository.clearFavorite()
            _uiState.update { it.copy(favoriteId = null) }
        } else {
            repository.setFavorite(ejercicioId)
            _uiState.update { it.copy(favoriteId = ejercicioId) }
        }
    }
}