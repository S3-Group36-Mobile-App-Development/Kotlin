package com.zenmind.kotlin.features.authentication.data.repositories

import com.zenmind.kotlin.features.authentication.data.models.AuthResponse

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): AuthResponse

    suspend fun register(
        email: String,
        password: String,
        nombreVisible: String
    ): AuthResponse
}