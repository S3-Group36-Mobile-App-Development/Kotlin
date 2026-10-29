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
                val streak = repository.getStreak()
                val moods = repository.getMoods()
                val todayMood = repository.getTodayCheckInMood()
                val flashcard = todayMood?.let { repository.getRecommendedFlashcard(it) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        moods = moods,
                        streak = streak,
                        alreadyCheckedInToday = todayMood != null,
                        selectedMoodId = todayMood?.id,
                        recommendedFlashcard = flashcard,
                        message = null
                    )
                }
            } catch (e: NotLoggedInException) {
                _uiState.update { it.copy(isLoading = false, notLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "No se pudo cargar el check-in.") }
            }
        }
    }

    // Tocar el mismo mood otra vez lo deselecciona
    fun onMoodSelected(mood: Mood) {
        _uiState.update {
            it.copy(selectedMoodId = if (it.selectedMoodId == mood.id) null else mood.id)
        }
    }

    // La vista ya mostró el Snackbar
    fun onSavedMessageShown() {
        _uiState.update { it.copy(justSaved = false) }
    }

    // Límite de caracteres (validación de UX; la protección real está en el backend)
    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.alreadyCheckedInToday || state.isSaving) return
        val mood = state.moods.find { it.id == state.selectedMoodId } ?: return

        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            var saved = false

            try {
                repository.saveCheckIn(mood.id, state.note.trim().ifBlank { null })
                saved = true
                // Pasa a la vista de resultado; la card llega después
                _uiState.update {
                    it.copy(isSaving = false, alreadyCheckedInToday = true, isLoadingCard = true, justSaved = true, message = null)
                }
            } catch (e: AlreadyCheckedInException) {
                // Ya había uno hoy (quizá con otro mood): se recarga para mostrar el real
                _uiState.update { it.copy(isSaving = false) }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "No se pudo guardar. Intenta de nuevo.") }
            }

            if (saved) {
                try {
                    val streak = repository.getStreak()
                    val flashcard = repository.getRecommendedFlashcard(mood)
                    _uiState.update { it.copy(streak = streak, recommendedFlashcard = flashcard, isLoadingCard = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoadingCard = false) }
                }
            }
        }
    }

    companion object {
        const val MAX_NOTE_LENGTH = 1000

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ZenMindApplication)
                DailyCheckInViewModel(repository = application.container.dailyCheckInRepository)
            }
        }
    }
}
