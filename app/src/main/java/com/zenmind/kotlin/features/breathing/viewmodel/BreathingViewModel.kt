package com.zenmind.kotlin.features.breathing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Recorre las fases (inhala/sostén/exhala) segundo a segundo y
 * repite el ejercicio durante la cantidad de ciclos indicada.
 *
 * La vista solo observa 'uiState' y llama a start/pause/resume/finish.
 */
class BreathingViewModel : ViewModel() { //Se usa ViewModel porque se necesita persistencia

    private val _uiState = MutableStateFlow(BreathingUiState()) // Editable
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow() // Solo lectura

    // El ejercicio actual y la corutina (gestión tareas en segundo plano) que lleva el tiempo.
    private var exercise: BreathingExercise? = null
    private var timerJob: Job? = null

    /** Arranca una sesión nueva con el ejercicio dado. */
    fun start(exercise: BreathingExercise) {
        this.exercise = exercise
        timerJob?.cancel()
        _uiState.value = BreathingUiState(
            status = SessionStatus.Running,
            totalCycles = exercise.cycles
        )
        runSession()
    }

    /** Lanza la corutina que recorre ciclos y fases. */
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
            // Terminaron todos los ciclos.
            _uiState.update {
                it.copy(status = SessionStatus.Finished, phase = null, secondsLeft = 0)
            }
        }
    }

    /** Pausa la sesión y recuerda el conteo. */
    fun pause(message: String? = null) {
        if (_uiState.value.status != SessionStatus.Running) {
            return
        }
        timerJob?.cancel()
        _uiState.update {
            it.copy(status = SessionStatus.Paused, message = message)
        }
    }

    /** Reanudar. */
    fun resume() {
        if (_uiState.value.status != SessionStatus.Paused) {
            return
        }
        _uiState.update {
            it.copy(status = SessionStatus.Running, message = null)
        }
        runSession()
    }

    /** Termina la sesión manualmente. */
    fun finish() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(status = SessionStatus.Finished, phase = null, secondsLeft = 0, message = null)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
