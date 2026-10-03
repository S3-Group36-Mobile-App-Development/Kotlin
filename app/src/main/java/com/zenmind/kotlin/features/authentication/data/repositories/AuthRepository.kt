package com.zenmind.kotlin.features.authentication.data.repositories

import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.UserDto

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): AuthResponse

    suspend fun loginWithGoogle(idToken: String): AuthResponse

    suspend fun register(
        email: String,
        password: String,
        nombreVisible: String,
        consentimientoDatos: Boolean
    ): AuthResponse

    suspend fun restoreSession(): UserDto?

    fun logout()
}
