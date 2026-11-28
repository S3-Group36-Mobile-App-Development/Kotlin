package com.zenmind.kotlin.features.home.model

import com.zenmind.kotlin.R

//Accesos del homepage
enum class HomeFeature(val label: String, val warm: Boolean, val image: Int) {
    PANIC("Pánico", true, R.drawable.panic_button),
    BREATHING("Respira", false, R.drawable.breathing_button),
    SUPPORT("Apoyo", false, R.drawable.support_button),
    PROTOCOLS("Protocolos", true, R.drawable.protocols_button),
    FLASHCARDS("Flashcards", true, R.drawable.flashcards_button),
    GAMES("Juegos", false, R.drawable.games_button)
}
