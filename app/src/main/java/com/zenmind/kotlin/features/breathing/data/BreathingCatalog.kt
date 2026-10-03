package com.zenmind.kotlin.features.breathing.data

import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.features.breathing.model.BreathingPhase
import com.zenmind.kotlin.features.breathing.model.BreathingStep

/**
 * Fuente del ejercicio de respiracion guiada.
 */
object BreathingCatalog {

    /** El ejercicio de respiracion guiada por defecto. */
    fun default(): BreathingExercise = BreathingExercise(
        id = "guided",
        name = "Respiracion guiada",
        description = "Encuentra tu ritmo y equilibra tu sistema.",
        steps = listOf(
            BreathingStep(BreathingPhase.Inhale, 4),
            BreathingStep(BreathingPhase.Hold, 4),
            BreathingStep(BreathingPhase.Exhale, 4),
            BreathingStep(BreathingPhase.Hold, 4)
        ),
        cycles = 10
    )
}
