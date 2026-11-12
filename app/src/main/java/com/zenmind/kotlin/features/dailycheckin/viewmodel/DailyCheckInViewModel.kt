package com.zenmind.kotlin.features.dailycheckin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.network.ConnectivityObserver
import com.zenmind.kotlin.core.telemetry.TelemetryRepository
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.model.DailyCheckInUiState
import com.zenmind.kotlin.features.dailycheckin.model.Flashcard
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.model.SaveResult
import com.zenmind.kotlin.features.dailycheckin.model.Streak
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyCheckInViewModel(
    private val repository: DailyCheckInRepository,
    private val telemetry: TelemetryRepository? = null,
    private val connectivity: ConnectivityObserver? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyCheckInUiState())
    val uiState: StateFlow<DailyCheckInUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var draftJob: Job? = null

    init {
        track("abierto")
        load()
        observeConnectivity()
        observeSync()
    }

    private fun observeSync() {
        viewModelScope.launch(Dispatchers.Main) {
            repository.syncEvents.collect {
                _uiState.update { it.copy(pendingSync = false, savedMessage = "Tu check-in pendiente se sincronizó.") }
            }
        }
    }

    private fun observeConnectivity() {
        val observer = connectivity ?: return
        viewModelScope.launch(Dispatchers.Main) {
            var wasOnline: Boolean? = null
            observer.onlineFlow.collect { online ->
                val guest = withContext(ioDispatcher) { repository.isGuest() }
                if (!guest) {
                    _uiState.update { it.copy(isOffline = !online) }
                }
                if (online && wasOnline == false) {
                    load(showSpinner = false)
                }
                wasOnline = online
            }
        }
    }

    private fun track(action: String, extra: Map<String, String> = emptyMap()) {
        telemetry?.featureUse(SCREEN, mapOf("accion" to action) + extra)
    }

    fun load(showSpinner: Boolean = true) {
        if (showSpinner) _uiState.update { it.copy(isLoading = true) }

        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                val local = readLocal()
                showLocal(local)

                val remote = fetchRemote()
                showRemote(remote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isLoadingCard = false, message = "No se pudo cargar el check-in.")
                }
            }
        }
    }

    private suspend fun readLocal(): LocalData = withContext(ioDispatcher) {
        val guest = repository.isGuest()
        val todayMood = repository.getLocalTodayMood()
        LocalData(
            guest = guest,
            moods = repository.getLocalMoods(),
            todayMood = todayMood,
            streak = repository.getLocalStreak(),
            flashcard = todayMood?.let { repository.getCachedFlashcard(it) },
            draft = repository.getDraft()
        )
    }

    private fun showLocal(local: LocalData) {
        val today = local.todayMood
        val cardExpected = today != null && repository.hasRecommendation(today)
        _uiState.update {
            val flashcard = local.flashcard ?: it.recommendedFlashcard
            it.copy(
                isLoading = false,
                isGuest = local.guest,
                moods = local.moods,
                streak = local.streak ?: it.streak,
                alreadyCheckedInToday = today != null,
                selectedMoodId = selectedIdFor(it, local.moods, today),
                note = it.note.ifEmpty { local.draft },
                recommendedFlashcard = flashcard,
                cardExpected = cardExpected,
                isLoadingCard = cardExpected && flashcard == null,
                message = null
            )
        }
    }

    private suspend fun fetchRemote(): RemoteData = withContext(ioDispatcher) {
        repository.syncPending()

        val moods = async { repository.getMoods() }
        val todayMood = async { repository.getTodayCheckInMood() }
        val streak = async { repository.getStreak() }

        val today = todayMood.await()
        RemoteData(
            moods = moods.await(),
            todayMood = today,
            streak = streak.await(),
            flashcard = today?.let { repository.getRecommendedFlashcard(it) },
            pending = repository.isTodayPending()
        )
    }

    private fun showRemote(remote: RemoteData) {
        val today = remote.todayMood
        _uiState.update {
            it.copy(
                moods = remote.moods,
                streak = remote.streak ?: it.streak,
                alreadyCheckedInToday = today != null,
                selectedMoodId = selectedIdFor(it, remote.moods, today),
                pendingSync = remote.pending,
                recommendedFlashcard = remote.flashcard ?: it.recommendedFlashcard,
                cardExpected = today != null && repository.hasRecommendation(today),
                isLoadingCard = false
            )
        }
        if (today != null) track("ya_registrado", mapOf("mood" to today.name))
    }

    private fun selectedIdFor(state: DailyCheckInUiState, moods: List<Mood>, today: Mood?): Long? {
        val name = today?.name ?: state.moods.find { it.id == state.selectedMoodId }?.name
        return moods.find { it.name == name }?.id ?: today?.id
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
        val text = note.take(MAX_NOTE_LENGTH)
        _uiState.update { it.copy(note = text) }
        draftJob?.cancel()
        draftJob = viewModelScope.launch(ioDispatcher) {
            delay(DRAFT_DELAY_MS)
            repository.saveDraft(text)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.alreadyCheckedInToday || state.isSaving) return
        val mood = state.moods.find { it.id == state.selectedMoodId } ?: return
        val note = state.note.trim().ifBlank { null }

        loadJob?.cancel()
        _uiState.update { it.copy(isSaving = true, message = null) }

        viewModelScope.launch(Dispatchers.Main) {
            val result = try {
                withContext(ioDispatcher) { repository.saveCheckIn(mood, note) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "No se pudo guardar. Intenta de nuevo.") }
                return@launch
            }

            val message = when (result) {
                SaveResult.SYNCED -> "Check-in guardado."
                SaveResult.PENDING -> "Check-in guardado en el teléfono. Se enviará cuando vuelvas a tener conexión."
                SaveResult.GUEST -> "Check-in guardado solo en el teléfono como invitado."
                SaveResult.LOCAL_ONLY -> "Tu cuenta no tiene activado guardar tus datos en el servidor; tu check-in quedó solo en este teléfono."
                SaveResult.ALREADY_REGISTERED -> "Ya ingresaste tu Check-in de hoy."
            }

            if (result == SaveResult.ALREADY_REGISTERED) {
                _uiState.update { it.copy(isSaving = false, savedMessage = message) }
                load()
                return@launch
            }

            track(
                "guardado",
                mapOf(
                    "mood" to mood.name,
                    "con_nota" to (note != null).toString(),
                    "resultado" to result.name.lowercase()
                )
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    alreadyCheckedInToday = true,
                    pendingSync = result == SaveResult.PENDING,
                    isLoadingCard = true,
                    cardExpected = repository.hasRecommendation(mood),
                    savedMessage = message,
                    message = null
                )
            }

            try {
                val (streak, flashcard) = withContext(ioDispatcher) {
                    val streakJob = async { repository.getStreak() }
                    val cardJob = async { repository.getRecommendedFlashcard(mood) }
                    streakJob.await() to cardJob.await()
                }

                if (streak != null) {
                    track(
                        "racha",
                        mapOf("racha_actual" to streak.current.toString(), "mejor_racha" to streak.longest.toString())
                    )
                }
                _uiState.update {
                    it.copy(
                        streak = streak ?: it.streak,
                        recommendedFlashcard = flashcard,
                        isLoadingCard = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCard = false) }
            }
        }
    }

    private data class LocalData(
        val guest: Boolean,
        val moods: List<Mood>,
        val todayMood: Mood?,
        val streak: Streak?,
        val flashcard: Flashcard?,
        val draft: String
    )

    private data class RemoteData(
        val moods: List<Mood>,
        val todayMood: Mood?,
        val streak: Streak?,
        val flashcard: Flashcard?,
        val pending: Boolean
    )

    companion object {
        const val MAX_NOTE_LENGTH = 1000
        const val DRAFT_DELAY_MS = 500L
        private const val SCREEN = "checkin"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ZenMindApplication)
                DailyCheckInViewModel(
                    repository = application.container.dailyCheckInRepository,
                    telemetry = application.container.telemetryRepository,
                    connectivity = application.container.connectivityObserver
                )
            }
        }
    }
}
