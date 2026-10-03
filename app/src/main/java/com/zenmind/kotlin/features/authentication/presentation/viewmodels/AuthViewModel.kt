package com.zenmind.kotlin.features.authentication.presentation.viewmodels

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(
        AuthUiState.CheckingSession
    )

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun restoreSession() {

        viewModelScope.launch {

            _uiState.value = AuthUiState.CheckingSession

            try {

                val user = authRepository.restoreSession()

                if (user != null) {

                    _uiState.value = AuthUiState.Success(
                        user = user
                    )

                } else {

                    _uiState.value = AuthUiState.Idle
                }

            } catch (exception: Exception) {

                _uiState.value = AuthUiState.Idle
            }
        }
    }

    fun login(
        email: String,
        password: String
    ) {

        val validationError = validateLogin(
            email = email,
            password = password
        )

        if (validationError != null) {

            _uiState.value = AuthUiState.Error(
                message = validationError
            )

            return
        }

        viewModelScope.launch {

            _uiState.value = AuthUiState.Loading

            try {

                val response = authRepository.login(
                    email = email.trim(),
                    password = password
                )

                _uiState.value = AuthUiState.Success(
                    user = response.usuario
                )

            } catch (exception: Exception) {

                _uiState.value = AuthUiState.Error(
                    message = exception.message
                        ?: "Ocurrió un error al iniciar sesión."
                )
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        acceptedDataConsent: Boolean
    ) {

        val validationError = validateRegister(
            name = name,
            email = email,
            password = password,
            acceptedDataConsent = acceptedDataConsent
        )

        if (validationError != null) {

            _uiState.value = AuthUiState.Error(
                message = validationError
            )

            return
        }

        viewModelScope.launch {

            _uiState.value = AuthUiState.Loading

            try {

                val response = authRepository.register(
                    email = email.trim(),
                    password = password,
                    nombreVisible = name.trim(),
                    consentimientoDatos = acceptedDataConsent
                )

                _uiState.value = AuthUiState.Success(
                    user = response.usuario
                )

            } catch (exception: Exception) {

                _uiState.value = AuthUiState.Error(
                    message = exception.message
                        ?: "Ocurrió un error al crear la cuenta."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validateLogin(
        email: String,
        password: String
    ): String? {

        if (email.isBlank()) {
            return "Ingresa tu correo electrónico."
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Ingresa un correo electrónico válido."
        }

        if (password.isBlank()) {
            return "Ingresa tu contraseña."
        }

        if (password.length < 8) {
            return "La contraseña debe tener al menos 8 caracteres."
        }

        return null
    }

    private fun validateRegister(
        name: String,
        email: String,
        password: String,
        acceptedDataConsent: Boolean
    ): String? {

        if (name.trim().length < 2) {
            return "El nombre debe tener al menos 2 caracteres."
        }

        if (email.isBlank()) {
            return "Ingresa tu correo electrónico."
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Ingresa un correo electrónico válido."
        }

        if (password.isBlank()) {
            return "Ingresa una contraseña."
        }

        if (password.length < 8) {
            return "La contraseña debe tener al menos 8 caracteres."
        }

        if (!acceptedDataConsent) {
            return "Debes aceptar el tratamiento de datos."
        }

        return null
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState.Idle
    }
}