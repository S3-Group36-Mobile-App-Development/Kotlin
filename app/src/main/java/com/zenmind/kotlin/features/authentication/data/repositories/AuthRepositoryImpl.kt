package com.zenmind.kotlin.features.authentication.data.repositories

import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.GoogleLoginRequest
import com.zenmind.kotlin.features.authentication.data.models.LoginRequest
import com.zenmind.kotlin.features.authentication.data.models.RegisterRequest
import com.zenmind.kotlin.features.authentication.data.services.AuthApiService
import com.zenmind.kotlin.features.authentication.data.models.RefreshRequest
import com.zenmind.kotlin.features.authentication.data.models.UserDto
import java.io.IOException

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): AuthResponse {
        val response = authApiService.loginWithGoogle(GoogleLoginRequest(idToken))

        if (!response.isSuccessful) {
            throw when (response.code()) {
                401 -> Exception("La cuenta de Google no pudo verificarse. Inténtalo de nuevo.")
                503 -> Exception("El inicio de sesión con Google no está disponible.")
                else -> Exception("No se pudo continuar con Google.")
            }
        }

        val authResponse = response.body()
            ?: throw Exception("La respuesta del servidor está vacía.")
        tokenStorage.saveTokens(
            accessToken = authResponse.accessToken,
            refreshToken = authResponse.refreshToken,
            userId = authResponse.usuario.id
        )
        return authResponse
    }

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
                refreshToken = authResponse.refreshToken,
                userId = authResponse.usuario.id
            )

            return authResponse
        }

        throw AuthException(when (response.code()) {
            401 -> AuthFailure.WRONG_CREDENTIALS
            400, 422 -> AuthFailure.INVALID_DATA
            else -> AuthFailure.SERVER
        })
    }

    override suspend fun register(
        email: String,
        password: String,
        nombreVisible: String,
        consentimientoDatos: Boolean
    ): AuthResponse {

        val response = authApiService.register(
            RegisterRequest(
                email = email,
                password = password,
                nombreVisible = nombreVisible,
                consentimientoDatos = consentimientoDatos
            )
        )

        if (response.isSuccessful) {

            val authResponse = response.body()
                ?: throw Exception(
                    "La respuesta del servidor está vacía."
                )

            tokenStorage.saveTokens(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                userId = authResponse.usuario.id
            )

            return authResponse
        }

        throw AuthException(when (response.code()) {
            409 -> AuthFailure.EMAIL_REGISTERED
            400, 422 -> AuthFailure.INVALID_DATA
            else -> AuthFailure.SERVER
        })
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
            if (refreshResponse.code() == 401) {
                tokenStorage.clearTokens()
                return null
            }
            throw IOException("No se pudo verificar la sesión. Comprueba tu conexión e inténtalo de nuevo.")
        }

        val newTokens = refreshResponse.body()
            ?: throw IOException("El servidor no pudo verificar la sesión. Inténtalo de nuevo.")

        tokenStorage.saveTokens(
            accessToken = newTokens.accessToken,
            refreshToken = newTokens.refreshToken
        )

        val userResponse = authApiService.getCurrentUser(
            authorization = "Bearer ${newTokens.accessToken}"
        )

        if (!userResponse.isSuccessful) {
            if (userResponse.code() == 401) {
                tokenStorage.clearTokens()
                return null
            }
            throw IOException("No se pudo verificar la sesión. Comprueba tu conexión e inténtalo de nuevo.")
        }

        val user = userResponse.body()
            ?: throw IOException("El servidor no pudo verificar la sesión. Inténtalo de nuevo.")
        tokenStorage.saveSessionUserId(user.id)
        return user
    }

    override fun logout() {
        tokenStorage.clearTokens()
    }
}
