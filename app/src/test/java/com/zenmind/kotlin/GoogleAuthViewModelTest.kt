package com.zenmind.kotlin

import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.UserDto
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepository
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthUiState
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModel
import com.zenmind.kotlin.rules.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoogleAuthViewModelTest {
    @get:Rule val dispatcher = TestDispatcherRule()

    private val user = UserDto(
        id = "1",
        email = "test@example.com",
        nombreVisible = "Test",
        fotoPerfilUrl = null,
        institucionId = null,
        departamentoId = null,
        consentimientoDatos = false,
        idiomaPreferido = "es",
        modoDaltonismoActivo = false,
        creadoEn = "2026-10-01T00:00:00Z"
    )

    @Test
    fun googleSignIn_successUsesTokenAndOpensSession() = runTest {
        val repository = FakeAuthRepository(AuthResponse(user, "access", "refresh"))
        val viewModel = AuthViewModel(repository)

        viewModel.loginWithGoogle { "google-id-token" }

        assertEquals("google-id-token", repository.receivedIdToken)
        assertEquals(AuthUiState.Success(user), viewModel.uiState.value)
    }

    @Test
    fun googleSignIn_pickerCancelledDoesNotCallBackend() = runTest {
        val repository = FakeAuthRepository(AuthResponse(user, "access", "refresh"))
        val viewModel = AuthViewModel(repository)

        viewModel.loginWithGoogle { null }

        assertEquals(null, repository.receivedIdToken)
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun googleSignIn_backendFailureShowsErrorWithoutSession() = runTest {
        val repository = FakeAuthRepository(AuthResponse(user, "access", "refresh"))
        repository.googleError = IllegalStateException("Token de Google inválido")
        val viewModel = AuthViewModel(repository)

        viewModel.loginWithGoogle { "expired-token" }

        assertEquals("expired-token", repository.receivedIdToken)
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
    }

    private class FakeAuthRepository(private val response: AuthResponse) : AuthRepository {
        var receivedIdToken: String? = null
        var googleError: Exception? = null

        override suspend fun loginWithGoogle(idToken: String): AuthResponse {
            receivedIdToken = idToken
            googleError?.let { throw it }
            return response
        }

        override suspend fun login(email: String, password: String): AuthResponse = response

        override suspend fun register(
            email: String,
            password: String,
            nombreVisible: String,
            consentimientoDatos: Boolean
        ): AuthResponse = response

        override suspend fun restoreSession(): UserDto? = null

        override fun logout() = Unit
    }
}
