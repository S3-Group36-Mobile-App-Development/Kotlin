package com.zenmind.kotlin.features.dailycheckin.model

data class Mood(
    val id: Long,
    val name: String
) {
    val displayName: String
        get() = when (name) {
            "Feliz" -> "Alegría"
            "Tranquilo" -> "Calma"
            "Ansioso" -> "Ansiedad"
            "Triste" -> "Tristeza"
            "Estresado" -> "Estrés"
            else -> name
        }
}
