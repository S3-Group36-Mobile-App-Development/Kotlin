package com.zenmind.kotlin.features.home.model

//Accesos del homepage
enum class HomeFeature(val label: String, val warm: Boolean) {
    PANIC("Pánico", warm = true),
    BREATHING("Respira", warm = false),
    SUPPORT("Apoyo", warm = false),
    PROTOCOLS("Protocolos", warm = true),
    FLASHCARDS("Flashcards", warm = true),
    GAMES("Juegos", warm = false)
}
