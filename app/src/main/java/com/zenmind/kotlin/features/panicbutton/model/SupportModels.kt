package com.zenmind.kotlin.features.panicbutton.model

/**
 * Contacto de confianza que el usuario puede llamar o enviarle un mensaje.
 */
data class SupportContact(
    val id: Long = 0,
    val name: String,
    val phone: String
)

/**
 * Línea de ayuda profesional. Solo soporta la acción de llamar.
 */
data class Helpline(
    val name: String,
    val phone: String
)
