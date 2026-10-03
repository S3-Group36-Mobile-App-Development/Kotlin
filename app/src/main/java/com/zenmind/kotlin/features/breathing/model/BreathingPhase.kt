package com.zenmind.kotlin.features.breathing.model

/**
 * Las fases posibles de un ejercicio de respiración.
 * - Inhale
 * - Hold
 * - Exhale
 * El 'label' es el texto que se le muestra al usuario en pantalla.
 */
enum class BreathingPhase(val label: String) {
    Inhale("Inhala"),
    Hold("Sostén"),
    Exhale("Exhala")
}
