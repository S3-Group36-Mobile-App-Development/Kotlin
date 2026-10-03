package com.zenmind.kotlin.features.breathing.model

/**
 * Estado de movimiento del teléfono, calculado a partir del acelerómetro.
 * - Still: Teléfono quieto.
 * - Moving: Teléfono en movimiento.
 * Se usa enum porque permite mayor legibilidad (MotionState.Moving) y permite su modificación.
 */
enum class MotionState {
    Still,
    Moving
}