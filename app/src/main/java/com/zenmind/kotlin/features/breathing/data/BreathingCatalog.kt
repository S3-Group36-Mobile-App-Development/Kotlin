package com.zenmind.kotlin.features.breathing.data

import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.features.breathing.model.BreathingPhase
import com.zenmind.kotlin.features.breathing.model.BreathingStep

/**
 * Construye la lista de ejercicios que el usuario puede elegir.
 */
object BreathingCatalog {

    /** Devuelve todos los ejercicios disponibles. */
    fun exercises(): List<BreathingExercise> = listOf(
        BreathingExercise(
            id = "box",
            name = "Box Breathing",
            description = "Respiración equilibrada 4-4-4-4 para concentrarte.",
            steps = listOf(
                BreathingStep(BreathingPhase.Inhale, 4),
                BreathingStep(BreathingPhase.Hold, 4),
                BreathingStep(BreathingPhase.Exhale, 4),
                BreathingStep(BreathingPhase.Hold, 4)
            ),
            cycles = 4
        ),
        BreathingExercise(
            id = "478",
            name = "4-7-8",
            description = "Exhalación larga para relajarte y dormir mejor.",
            steps = listOf(
                BreathingStep(BreathingPhase.Inhale, 4),
                BreathingStep(BreathingPhase.Hold, 7),
                BreathingStep(BreathingPhase.Exhale, 8)
            ),
            cycles = 4
        ),
        BreathingExercise(
            id = "calm",
            name = "Calm Breathing",
            description = "Respiración suave 4-6 para calmar la ansiedad.",
            steps = listOf(
                BreathingStep(BreathingPhase.Inhale, 4),
                BreathingStep(BreathingPhase.Exhale, 6)
            ),
            cycles = 4
        )
    )
}
