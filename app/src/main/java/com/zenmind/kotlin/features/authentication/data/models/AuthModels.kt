package com.zenmind.kotlin.features.authentication.data.models

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val nombreVisible: String
)

data class UserDto(
    val id: String,
    val email: String,
    val nombreVisible: String,
    val fotoPerfilUrl: String?,
    val institucionId: String?,
    val departamentoId: String?,
    val consentimientoDatos: Boolean,
    val idiomaPreferido: String,
    val modoDaltonismoActivo: Boolean,
    val creadoEn: String
)

data class AuthResponse(
    val usuario: UserDto,
    val accessToken: String,
    val refreshToken: String
)