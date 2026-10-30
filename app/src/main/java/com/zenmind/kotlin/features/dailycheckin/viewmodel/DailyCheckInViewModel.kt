package com.zenmind.kotlin.features.dailycheckin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.telemetry.TelemetryRepository
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.data.NotLoggedInException
import com.zenmind.kotlin.features.dailycheckin.model.DailyCheckInUiState
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.model.SaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DailyCheckInViewModel(
    private val repository: DailyCheckInRepository,
    private val telemetry: TelemetryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyCheckInUiState())
    val uiState: StateFlow<DailyCheckInUiState> = _uiState.asStateFlow()

    init {
        track("abierto")
        load()
    }

    private fun track(action: String, extra: Map<String, String> = emptyMap()) {
        telemetry?.featureUse(SCREEN, mapOf("accion" to action) + extra)
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val streak = repository.getStreak()
                val moods = repository.getMoods()
                val todayMood = repository.getTodayCheckInMood()
                val flashcard = todayMood?.let { repository.getRecommendedFlashcard(it) }
                if (todayMood != null) track("ya_registrado", mapOf("mood" to todayMood.name))
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

    fun onMoodSelected(mood: Mood) {
        _uiState.update {
            it.copy(selectedMoodId = if (it.selectedMoodId == mood.id) null else mood.id)
        }
    }

    fun onSavedMessageShown() {
        _uiState.update { it.copy(savedMessage = null) }
    }

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
                val result = repository.saveCheckIn(mood, state.note.trim().ifBlank { null })

                val message = when (result) {
                    SaveResult.SYNCED -> "Check-in guardado."
                    SaveResult.PENDING -> "Check-in guardado en el teléfono. Se enviará cuando vuelvas a tener conexión."
                    SaveResult.GUEST -> "Check-in guardado solo en el teléfono como invitado."
                    SaveResult.ALREADY_REGISTERED -> "Ya ingresaste tu Check-in de hoy."
                }

                if (result == SaveResult.ALREADY_REGISTERED) {
                    _uiState.update { it.copy(isSaving = false, savedMessage = message) }
                    load()
                    return@launch
                }

                saved = true
                track(
                    "guardado",
                    mapOf(
                        "mood" to mood.name,
                        "con_nota" to state.note.isNotBlank().toString(),
                        "resultado" to result.name.lowercase()
                    )
                )
                _uiState.update {
                    it.copy(isSaving = false, alreadyCheckedInToday = true, isLoadingCard = true, savedMessage = message, message = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "No se pudo guardar. Intenta de nuevo.") }
            }

            if (saved) {
                try {
                    val streak = repository.getStreak()
                    track(
                        "racha",
                        mapOf("racha_actual" to streak.current.toString(), "mejor_racha" to streak.longest.toString())
                    )
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
        private const val SCREEN = "checkin"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ZenMindApplication)
                DailyCheckInViewModel(
                    repository = application.container.dailyCheckInRepository,
                    telemetry = application.container.telemetryRepository
                )
            }
        }
    }
}
