package com.zenmind.kotlin.features.breathing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.breathing.data.AccelerometerMotionSource
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.features.breathing.model.MotionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.zenmind.kotlin.features.breathing.data.BreathingRepository

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

    init {
        observeMotion()
    }

    // Escucha el sensor y reacciona segun el estado de la sesion.
    private fun observeMotion() {
        val source = motionSource ?: return
        viewModelScope.launch {
            source.motion().collect { motion ->
                when (_uiState.value.status) {
                    SessionStatus.Idle -> {
                        _uiState.update {
                            it.copy(
                                message = if (motion == MotionState.Moving)
                                    "Apoya el telefono o sientate" else null
                            )
                        }
                    }
                    SessionStatus.Running -> {
                        if (motion == MotionState.Moving) {
                            scheduleMovementPause()
                        } else {
                            movementPauseJob?.cancel()
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    /** Si el movimiento dura 3 segundos seguidos, pausa la sesion. */
    private fun scheduleMovementPause() {
        if (movementPauseJob?.isActive == true) return
        movementPauseJob = viewModelScope.launch {
            delay(3000)
            pause("Movimiento detectado, sesion pausada")
        }
    }

    fun start(exercise: BreathingExercise) {
        this.exercise = exercise
        timerJob?.cancel()
        _uiState.value = BreathingUiState(
            status = SessionStatus.Running,
            totalCycles = exercise.cycles
        )
        runSession()
    }

    private fun runSession() {
        val ex = exercise ?: return
        timerJob = viewModelScope.launch {
            for (cycle in 1..ex.cycles) {
                _uiState.update { it.copy(currentCycle = cycle) }
                for (step in ex.steps) {
                    _uiState.update { it.copy(phase = step.phase, secondsLeft = step.seconds) }
                    for (second in step.seconds downTo 1) {
                        _uiState.update { it.copy(secondsLeft = second) }
                        delay(1000)
                    }
                }
            }
            _uiState.update {
                it.copy(status = SessionStatus.Finished, phase = null, secondsLeft = 0)
            }
            repository?.let { repo ->
                launch { repo.guardarSesion(ejercicioId = 1) }
            }
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
