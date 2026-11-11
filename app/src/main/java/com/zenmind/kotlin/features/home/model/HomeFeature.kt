package com.zenmind.kotlin.features.home.model

import com.zenmind.kotlin.R

//Accesos del homepage
enum class HomeFeature(val label: String, val warm: Boolean, val image: Int) {
    BREATHING("Respira", false, R.drawable.breathing_button),
    GAMES("Juegos", true, R.drawable.protocols_button)
}
