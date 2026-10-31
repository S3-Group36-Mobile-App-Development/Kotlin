package com.zenmind.kotlin.features.games.viewmodel

import android.os.SystemClock
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.games.data.GameOwner
import com.zenmind.kotlin.features.games.data.GameSessionEntity
import com.zenmind.kotlin.features.games.data.GameSessionRepository
import com.zenmind.kotlin.features.games.data.GameSyncScheduler
import com.zenmind.kotlin.features.games.data.completedSession
import com.zenmind.kotlin.features.games.model.BubbleGame
import com.zenmind.kotlin.features.games.model.GamePhase
import com.zenmind.kotlin.features.games.model.GameState
import com.zenmind.kotlin.features.games.model.GameStatistics
import com.zenmind.kotlin.features.games.model.calculateGameStatistics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SaveStatus { Idle, Saving, Saved, Failed }

data class GameRecordsState(
    val owner: GameOwner? = null,
    val history: List<GameSessionEntity> = emptyList(),
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val message: String? = null,
    val syncing: Boolean = false,
    val statistics: GameStatistics = GameStatistics(),
    val calculatingStatistics: Boolean = true,
    val refreshingHistory: Boolean = false,
    val refreshMessage: String? = null
)

class BubbleGameViewModel(
    private val repository: GameSessionRepository,
    private val applicationScope: CoroutineScope,
    private val applicationContext: Context
) : ViewModel() {
    private val game = BubbleGame(clock = SystemClock::elapsedRealtime)
    private val _uiState = MutableStateFlow(GameState())
    val uiState = _uiState.asStateFlow()
    private val _records = MutableStateFlow(GameRecordsState())
    val records = _records.asStateFlow()
    private var pendingSession: GameSessionEntity? = null
    private var historyJob: Job? = null
    private var syncJob: Job? = null
    private var refreshJob: Job? = null

    init { loadHistory() }

    fun loadHistory() {
        historyJob?.cancel()
        syncJob?.cancel()
        refreshJob?.cancel()
        historyJob = viewModelScope.launch {
            try {
                val owner = repository.currentOwner()
                _records.value = _records.value.copy(owner = owner, message = null, calculatingStatistics = true)
                if (!owner.isGuest) {
                    applicationScope.launch {
                        try { GameSyncScheduler.enqueuePending(applicationContext) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { /* The local history remains available. */ }
                    }
                    syncJob = viewModelScope.launch {
                        GameSyncScheduler.running(applicationContext, owner).collect { running ->
                            _records.value = _records.value.copy(syncing = running)
                        }
                    }
                    refreshJob = viewModelScope.launch {
                        _records.value = _records.value.copy(refreshingHistory = true, refreshMessage = null)
                        try {
                            val updated = repository.refreshFromBackend(owner)
                            if (!updated) _records.value = _records.value.copy(
                                refreshMessage = "Mostrando partidas locales; no se pudo actualizar el servidor.")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            _records.value = _records.value.copy(
                                refreshMessage = "Mostrando partidas locales; no se pudo actualizar el servidor.")
                        } finally {
                            _records.value = _records.value.copy(refreshingHistory = false)
                        }
                    }
                }
                repository.observeHistory(owner).collectLatest { history ->
                    _records.value = _records.value.copy(history = history, calculatingStatistics = true)
                    val statistics = withContext(Dispatchers.Default) {
                        calculateGameStatistics(history, owner)
                    }
                    _records.value = _records.value.copy(statistics = statistics, calculatingStatistics = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _records.value = _records.value.copy(message =
                    e.message ?: "No se pudo abrir el historial local. Inténtalo de nuevo.")
            }
        }
    }

    fun resize(width: Float, height: Float) = update { game.resize(width, height) }

    fun start() {
        if (_records.value.owner == null || _records.value.saveStatus == SaveStatus.Saving ||
            _records.value.saveStatus == SaveStatus.Failed) return
        pendingSession = null
        _records.value = _records.value.copy(saveStatus = SaveStatus.Idle, message = null)
        update { game.start() }
    }

    fun tap(number: Int) {
        val wasFinished = game.state.phase == GamePhase.Finished
        update { game.tap(number) }
        if (!wasFinished && game.state.phase == GamePhase.Finished) {
            val owner = _records.value.owner ?: return
            pendingSession = completedSession(game.state, owner)
            savePending()
        }
    }

    fun retrySave() {
        if (_records.value.saveStatus == SaveStatus.Failed) savePending()
    }

    private fun savePending() {
        val session = pendingSession ?: return
        _records.value = _records.value.copy(saveStatus = SaveStatus.Saving, message = null)
        // The write belongs to the app, not the screen: leaving Home must not cancel it.
        val write = applicationScope.async {
            repository.save(session)
            if (!session.isGuest) {
                try {
                    val owner = GameOwner(session.ownerId, false)
                    if (repository.currentOwner() == owner) {
                        GameSyncScheduler.enqueue(applicationContext, owner, ExistingWorkPolicy.APPEND_OR_REPLACE)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // A failed enqueue cannot undo the completed local write.
                }
            }
        }
        viewModelScope.launch {
            try {
                write.await()
                _records.value = _records.value.copy(saveStatus = SaveStatus.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _records.value = _records.value.copy(saveStatus = SaveStatus.Failed,
                    message = "No se pudo guardar la partida. Reintenta antes de salir.")
            }
        }
    }

    fun tick() = update { game.tick() }
    fun pause() = update { game.pause() }
    fun resume() = update { game.resume() }

    private inline fun update(block: () -> Unit) {
        block()
        _uiState.value = game.state
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZenMindApplication
                BubbleGameViewModel(
                    GameSessionRepository(AppDatabase.getInstance(app).gameSessionDao(), TokenStorage(app)),
                    app.applicationScope,
                    app.applicationContext
                )
            }
        }
    }
}
