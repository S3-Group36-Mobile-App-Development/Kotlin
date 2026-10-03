package com.zenmind.kotlin.features.dailycheckin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.features.dailycheckin.data.AlreadyCheckedInException
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.data.NotLoggedInException
import com.zenmind.kotlin.features.dailycheckin.model.DailyCheckInUiState
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DailyCheckInViewModel(
    private val repository: DailyCheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyCheckInUiState())
    val uiState: StateFlow<DailyCheckInUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Primero la racha: pide sesión, así si no hay login no se carga nada más
                val streak = repository.getStreak()
                val moods = repository.getMoods()
                val todayMood = repository.getTodayCheckInMood()
                // SMART FEATURE: si ya hizo el check-in hoy, se muestra la flashcard de ese mood
                val flashcard = todayMood?.let { repository.getRecommendedFlashcard(it) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        moods = moods,
                        streak = streak,
                        alreadyCheckedInToday = todayMood != null,
                        selectedMoodId = todayMood?.id,
                        recommendedFlashcard = flashcard,
                        message = if (todayMood != null) "Ya registraste tu check-in de hoy. ¡Vuelve mañana!" else null
                    )
                }
            } catch (e: NotLoggedInException) {
                _uiState.update { it.copy(isLoading = false, notLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "No se pudo cargar el check-in.") }
            }
        }
    }

    fun onMoodSelected(mood: Mood) {
        _uiState.update { it.copy(selectedMoodId = mood.id) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.alreadyCheckedInToday || state.isSaving) return
        val mood = state.moods.find { it.id == state.selectedMoodId } ?: return

        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            var saved = false

            try {
                repository.saveCheckIn(mood.id, state.note.ifBlank { null })
                saved = true
                _uiState.update { it.copy(isSaving = false, alreadyCheckedInToday = true, message = "¡Check-in guardado!") }
            } catch (e: AlreadyCheckedInException) {
                _uiState.update { it.copy(isSaving = false, alreadyCheckedInToday = true, message = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "No se pudo guardar. Intenta de nuevo.") }
            }

            if (saved) {
                try {
                    val streak = repository.getStreak()
                    // SMART FEATURE: flashcard recomendada según el mood que acaba de elegir
                    val flashcard = repository.getRecommendedFlashcard(mood)
                    _uiState.update { it.copy(streak = streak, recommendedFlashcard = flashcard) }
                } catch (e: Exception) {
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ZenMindApplication)
                DailyCheckInViewModel(repository = application.container.dailyCheckInRepository)
            }
        }
    }
}
