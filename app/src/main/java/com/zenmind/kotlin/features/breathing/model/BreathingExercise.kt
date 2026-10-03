package com.zenmind.kotlin.features.breathing.model

/**
 * Un ejercicio de respiración que el usuario puede elegir.
 * - id: Identificador único que también sirve para enviarlo al backend).
 * - name/description: Lo que ve el usuario en la lista.
 * - steps: La secuencia de fases (viene de BreathingTechnique).
 * - cycles: Cuántas veces se repite esa secuencia completa.
 */
data class BreathingExercise(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<BreathingStep>,
    val cycles: Int
)
