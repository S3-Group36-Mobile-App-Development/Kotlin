package com.zenmind.kotlin.features.breathing.model

/**
 * Un ejercicio de respiración que el usuario puede elegir.
 * - id
 * - name/description
 * - steps: La secuencia de fases (viene de BreathingTechnique).
 * - cycles
 */
data class BreathingExercise(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<BreathingStep>,
    val cycles: Int
)
