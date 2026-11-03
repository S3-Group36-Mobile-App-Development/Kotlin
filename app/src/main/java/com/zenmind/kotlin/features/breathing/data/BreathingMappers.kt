package com.zenmind.kotlin.features.breathing.data

import com.zenmind.kotlin.features.breathing.data.local.BreathingExerciseEntity
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.features.breathing.model.BreathingPhase
import com.zenmind.kotlin.features.breathing.model.BreathingStep

fun EjercicioDto.toEntity(): BreathingExerciseEntity = BreathingExerciseEntity(
    id = id.toInt(),
    titulo = titulo,
    descripcion = descripcion,
    duracion = duracion,
    inhalar = patron.inhalar,
    sostener = patron.sostener,
    exhalar = patron.exhalar,
    sostener2 = patron.sostener2,
    ciclos = patron.ciclos ?: 1,
    vibra = vibra
)

fun BreathingExerciseEntity.toExercise(): BreathingExercise {
    val steps = buildList {
        inhalar?.takeIf { it > 0 }?.let { add(BreathingStep(BreathingPhase.Inhale, it)) }
        sostener?.takeIf { it > 0 }?.let { add(BreathingStep(BreathingPhase.Hold, it)) }
        exhalar?.takeIf { it > 0 }?.let { add(BreathingStep(BreathingPhase.Exhale, it)) }
        sostener2?.takeIf { it > 0 }?.let { add(BreathingStep(BreathingPhase.Hold, it)) }
    }

    return BreathingExercise(
        id = id,
        name = titulo,
        description = descripcion ?: "",
        steps = steps,
        cycles = ciclos
    )
}