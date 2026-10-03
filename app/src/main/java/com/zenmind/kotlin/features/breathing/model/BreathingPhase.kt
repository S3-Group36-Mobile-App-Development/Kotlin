package com.zenmind.kotlin.features.breathing.model

/**
 * Las fases posibles de un ejercicio de respiración.
 * - Inhale: Inhalar (el círculo crece)
 * - Hold: Sostener el aire (el círculo se queda igual)
 * - Exhale: Exhalar (el círculo se encoge)
 * El 'label' es el texto que se le muestra al usuario en pantalla.
 */
enum class BreathingPhase(val label: String) {
    Inhale("Inhala"),
    Hold("Sostén"),
    Exhale("Exhala")
}
