package com.zenmind.kotlin

import com.zenmind.kotlin.features.authentication.data.models.AuthResponse
import com.zenmind.kotlin.features.authentication.data.models.UserDto
import com.zenmind.kotlin.features.authentication.data.repositories.AuthException
import com.zenmind.kotlin.features.authentication.data.repositories.AuthFailure
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepository
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthUiState
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModel
import com.zenmind.kotlin.rules.TestDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterAuthViewModelTest {
    @get:Rule val dispatcher = TestDispatcherRule()

    private val user = UserDto("1", "ana@example.com", "Ana", null, null, null, true, "es", false, "2026-01-01")
    private val response = AuthResponse(user, "access", "refresh")

    @Test
    fun offlineBeforeSendingDoesNotCallRepositoryAndCanRetry() = runTest {
        val repository = FakeRepository(response)
        var online = false
        val vm = newViewModel(repository) { online }

        vm.register("Ana", user.email, "password123", true)
        assertEquals(0, repository.registerCalls)
        assertTrue((vm.uiState.value as AuthUiState.Error).message.contains("Sin conexión"))

        online = true
        vm.register("Ana", user.email, "password123", true)
        assertEquals(1, repository.registerCalls)
        assertEquals(AuthUiState.Success(user), vm.uiState.value)
    }

    @Test
    fun lostResponseDoesNotClaimSuccessAndOffersLoginOrRetry() = runTest {
        val repository = FakeRepository(response)
        repository.registerError = IOException("Unable to resolve host internal.server")
        val vm = newViewModel(repository)

        vm.register("Ana", user.email, "password123", true)
        val error = vm.uiState.value as AuthUiState.Error
        assertTrue(error.offerLogin)
        assertTrue(error.message.contains("No pudimos confirmar"))
        assertFalse(error.message.contains("internal.server"))

        repository.registerError = null
        vm.register("Ana", user.email, "password123", true)
        assertEquals(2, repository.registerCalls)
        assertEquals(AuthUiState.Success(user), vm.uiState.value)
    }

    @Test
    fun conflictOffersLoginAndInvalidDataDoesNot() = runTest {
        val repository = FakeRepository(response)
        val vm = newViewModel(repository)
        repository.registerError = AuthException(AuthFailure.EMAIL_REGISTERED)

        vm.register("Ana", user.email, "password123", true)
        assertTrue((vm.uiState.value as AuthUiState.Error).offerLogin)

        repository.registerError = AuthException(AuthFailure.INVALID_DATA)
        vm.register("Ana", user.email, "password123", true)
        val error = vm.uiState.value as AuthUiState.Error
        assertFalse(error.offerLogin)
        assertTrue(error.message.contains("datos"))
    }

    @Test
    fun invalidEmailNeverSendsAndServerFailureIsDistinguishedFromUnknownResult() = runTest {
        val repository = FakeRepository(response)
        val vm = newViewModel(repository)

        vm.register("Ana", "invalid-email", "password123", true)
        assertEquals(0, repository.registerCalls)
        assertTrue((vm.uiState.value as AuthUiState.Error).message.contains("correo electrónico válido"))

        repository.registerError = AuthException(AuthFailure.SERVER)
        vm.register("Ana", user.email, "password123", true)
        val error = vm.uiState.value as AuthUiState.Error
        assertEquals(1, repository.registerCalls)
        assertFalse(error.offerLogin)
        assertTrue(error.message.contains("servidor"))
    }

    @Test
    fun repeatedTapsDuringPendingRequestSendOnlyOnce() = runTest {
        val repository = FakeRepository(response)
        val pending = CompletableDeferred<Unit>()
        repository.waitBeforeResponse = pending
        val vm = newViewModel(repository)

        vm.register("Ana", user.email, "password123", true)
        vm.register("Ana", user.email, "password123", true)
        assertEquals(1, repository.registerCalls)
        assertEquals(AuthUiState.Loading, vm.uiState.value)

        pending.complete(Unit)
        assertEquals(AuthUiState.Success(user), vm.uiState.value)
    }

    @Test
    fun loginMapsCredentialsAndNetworkErrorsWithoutExposingException() = runTest {
        val repository = FakeRepository(response)
        val vm = newViewModel(repository) { false }
        repository.loginError = AuthException(AuthFailure.WRONG_CREDENTIALS)

        vm.login(user.email, "password123")
        assertEquals("Correo o contraseña incorrectos.", (vm.uiState.value as AuthUiState.Error).message)

        repository.loginError = IOException("Unable to resolve host internal.server")
        vm.login(user.email, "password123")
        val error = vm.uiState.value as AuthUiState.Error
        assertTrue(error.message.contains("Sin conexión"))
        assertFalse(error.message.contains("internal.server"))
    }

    private fun newViewModel(repository: AuthRepository, isOnline: () -> Boolean = { true }) =
        AuthViewModel(repository, isOnline, isValidEmail = { it == user.email })

    private class FakeRepository(private val response: AuthResponse) : AuthRepository {
        var registerCalls = 0
        var registerError: Exception? = null
        var loginError: Exception? = null
        var waitBeforeResponse: CompletableDeferred<Unit>? = null

        override suspend fun register(email: String, password: String, nombreVisible: String, consentimientoDatos: Boolean): AuthResponse {
            registerCalls++
            waitBeforeResponse?.await()
            registerError?.let { throw it }
            return response
        }

        override suspend fun login(email: String, password: String): AuthResponse {
            loginError?.let { throw it }
            return response
        }

        override suspend fun loginWithGoogle(idToken: String): AuthResponse = response
        override suspend fun restoreSession(): UserDto? = null
        override fun logout() = Unit
    }
}
