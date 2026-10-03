package com.zenmind.kotlin.features.breathing.model

/**
 * Un paso de un ejercicio: estar en una fase durante cierta cantidad de segundos.
 */
data class BreathingStep(
    val phase: BreathingPhase,
    val seconds: Int
)
