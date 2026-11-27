package com.zenmind.kotlin.features.breathing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource
import com.zenmind.kotlin.features.breathing.data.BreathingRepository
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.features.breathing.model.MotionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Recorre las fases (inhala/sosten/exhala) segundo a segundo y
 * repite el ejercicio durante la cantidad de ciclos indicada.
 *
 * Si recibe un sensor de movimiento, adapta la sesion: Avisa que
 * apoye el telefono antes de empezar, o pausa si hay movimiento
 * sostenido durante la sesion.
 */
class BreathingViewModel(
    private val motionSource: AccelerometerMotionSource? = null,
    private val repository: BreathingRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private var exercise: BreathingExercise? = null
    private var timerJob: Job? = null
    private var movementPauseJob: Job? = null
    private var currentEjercicioId: Int = 1

    init {
        observeMotion()
    }

    // Escucha el sensor y reacciona segun el estado de la sesion.
    private fun observeMotion() {
        val source = motionSource ?: return
        viewModelScope.launch {
            source.motion().collect { motion ->
                _uiState.update {
                    it.copy(
                        message = if (motion == MotionState.Moving)
                            "Apoya el teléfono o siéntate" else null
                    )
                }
            }
        }
    }

    fun start(exercise: BreathingExercise) {
        this.exercise = exercise
        this.currentEjercicioId = exercise.id
        timerJob?.cancel()
        _uiState.value = BreathingUiState(
            status = SessionStatus.Running,
            totalCycles = exercise.cycles
        )
        runSession()
    }

    /* Corrutina temporizador */
    private fun runSession() {
        val ex = exercise ?: return
        timerJob = viewModelScope.launch {
            for (cycle in 1..ex.cycles) {
                _uiState.update { it.copy(currentCycle = cycle) }
                for (step in ex.steps) {
                    _uiState.update { it.copy(phase = step.phase, secondsLeft = step.seconds) }
                    for (second in step.seconds downTo 1) {
                        _uiState.update { it.copy(secondsLeft = second, message = null) }
                        delay(1000)
                    }
                }
            }
            _uiState.update {
                it.copy(status = SessionStatus.Finished, phase = null, secondsLeft = 0, message = "¡Sesión completada!.")
            }
            // La sesion termino, registra el uso (local + encola sync).
            android.util.Log.d("BreathingSync", "Sesion terminada, guardando (repo=${repository != null})")
            repository?.guardarSesion(ejercicioId = currentEjercicioId)
            android.util.Log.d("BreathingSync", "guardarSesion llamado")
        }
    }

    fun pause(message: String? = null) {
        if (_uiState.value.status != SessionStatus.Running) return
        timerJob?.cancel()
        _uiState.update { it.copy(status = SessionStatus.Paused, message = message) }
    }

    fun resume() {
        if (_uiState.value.status != SessionStatus.Paused) return
        _uiState.update { it.copy(status = SessionStatus.Running, message = null) }
        runSession()
    }

    fun finish() {
        timerJob?.cancel()
        movementPauseJob?.cancel()
        _uiState.update {
            it.copy(status = SessionStatus.Finished, phase = null, secondsLeft = 0, message = null)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        movementPauseJob?.cancel()
        super.onCleared()
    }
}
