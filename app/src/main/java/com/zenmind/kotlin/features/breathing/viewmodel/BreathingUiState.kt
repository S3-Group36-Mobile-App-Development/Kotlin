package com.zenmind.kotlin.features.breathing.viewmodel

import com.zenmind.kotlin.features.breathing.model.BreathingPhase

/**
 * En qué punto está la sesión de respiración.
 * - Idle: Aún no empieza (pantalla lista para arrancar).
 * - Running: Sesión activa.
 * - Paused: Pausada por el usuario o por movimiento.
 * - Finished: Finalizada por el sistema.
 */
enum class SessionStatus {
    Idle,
    Running,
    Paused,
    Finished
}

/**
 * Estado completo que la vista observa para mostrarse.
 *
 * - status
 * - phase: Fase actual (Inhala/Sostén/Exhala), null si está Idle.
 * - secondsLeft: Segundos que faltan en la fase actual.
 * - currentCycle / totalCycles: Progreso de ciclos.
 * - message: Texto opcional para el usuario (ej. "Apoya el teléfono").
 */
data class BreathingUiState(
    val status: SessionStatus = SessionStatus.Idle,
    val phase: BreathingPhase? = null,
    val secondsLeft: Int = 0,
    val currentCycle: Int = 0,
    val totalCycles: Int = 0,
    val message: String? = null
)
