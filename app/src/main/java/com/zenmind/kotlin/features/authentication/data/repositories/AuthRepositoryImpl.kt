package com.zenmind.kotlin.features.authentication.data.repositories

import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.LoginRequest
import com.zenmind.kotlin.features.authentication.data.models.RegisterRequest
import com.zenmind.kotlin.features.authentication.data.services.AuthApiService

class AuthRepositoryImpl(
    private val authApiService: AuthApiService
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthResponse {

        val response = authApiService.login(
            LoginRequest(
                email = email,
                password = password
            )
        )

        if (response.isSuccessful) {
            return response.body()
                ?: throw Exception("La respuesta del servidor está vacía.")
        }

        throw when (response.code()) {
            401 -> Exception("Correo o contraseña incorrectos.")
            else -> Exception("No se pudo iniciar sesión.")
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        nombreVisible: String
    ): AuthResponse {

        val response = authApiService.register(
            RegisterRequest(
                email = email,
                password = password,
                nombreVisible = nombreVisible
            )
        )

        if (response.isSuccessful) {
            return response.body()
                ?: throw Exception("La respuesta del servidor está vacía.")
        }

        throw when (response.code()) {
            409 -> Exception("Ya existe una cuenta con ese correo.")
            else -> Exception("No se pudo crear la cuenta.")
        }
    }
}