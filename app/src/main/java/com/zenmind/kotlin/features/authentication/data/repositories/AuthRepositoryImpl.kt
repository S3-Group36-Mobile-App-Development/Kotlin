package com.zenmind.kotlin.features.authentication.data.repositories

import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.LoginRequest
import com.zenmind.kotlin.features.authentication.data.models.RegisterRequest
import com.zenmind.kotlin.features.authentication.data.services.AuthApiService
import com.zenmind.kotlin.features.authentication.data.models.RefreshRequest
import com.zenmind.kotlin.features.authentication.data.models.UserDto

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage
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

            val authResponse = response.body()
                ?: throw Exception(
                    "La respuesta del servidor está vacía."
                )

            tokenStorage.saveTokens(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )

            return authResponse
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

            val authResponse = response.body()
                ?: throw Exception(
                    "La respuesta del servidor está vacía."
                )

            tokenStorage.saveTokens(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )

            return authResponse
        }

        throw when (response.code()) {
            409 -> Exception("Ya existe una cuenta con ese correo.")
            else -> Exception("No se pudo crear la cuenta.")
        }
    }

    override suspend fun restoreSession(): UserDto? {

        val refreshToken = tokenStorage.getRefreshToken()
            ?: return null

        val refreshResponse = authApiService.refresh(
            RefreshRequest(
                refreshToken = refreshToken
            )
        )

        if (!refreshResponse.isSuccessful) {
            tokenStorage.clearTokens()
            return null
        }

        val newTokens = refreshResponse.body()
            ?: run {
                tokenStorage.clearTokens()
                return null
            }

        tokenStorage.saveTokens(
            accessToken = newTokens.accessToken,
            refreshToken = newTokens.refreshToken
        )

        val userResponse = authApiService.getCurrentUser(
            authorization = "Bearer ${newTokens.accessToken}"
        )

        if (!userResponse.isSuccessful) {
            tokenStorage.clearTokens()
            return null
        }

        return userResponse.body()
    }

    override fun logout() {
        tokenStorage.clearTokens()
    }
}