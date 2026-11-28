package com.zenmind.kotlin.features.authentication.presentation.viewmodels

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.authentication.data.repositories.AuthException
import com.zenmind.kotlin.features.authentication.data.repositories.AuthFailure
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val isOnline: () -> Boolean = { true },
    private val isValidEmail: (String) -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches() }
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

            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                _uiState.value = AuthUiState.SessionUnavailable(
                    "No se pudo comprobar tu sesión. Comprueba tu conexión e inténtalo de nuevo."
                )
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.SessionUnavailable(
                    "No se pudo comprobar tu sesión. Inténtalo de nuevo."
                )
            }
        }
    }

    fun login(
        email: String,
        password: String
    ) {
        if (_uiState.value is AuthUiState.Loading) return

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

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {

            try {

                val response = authRepository.login(
                    email = email.trim(),
                    password = password
                )

                _uiState.value = AuthUiState.Success(
                    user = response.usuario
                )

            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: AuthException) {
                _uiState.value = AuthUiState.Error(when (exception.reason) {
                    AuthFailure.WRONG_CREDENTIALS -> "Correo o contraseña incorrectos."
                    AuthFailure.INVALID_DATA -> "Revisa los datos ingresados."
                    else -> "El servidor presenta problemas temporales. Inténtalo más tarde."
                })
            } catch (exception: IOException) {
                _uiState.value = AuthUiState.Error(
                    if (isOnline()) "No se pudo contactar al servidor. Inténtalo más tarde."
                    else "Sin conexión a internet. Comprueba tu conexión e inténtalo de nuevo."
                )
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.Error("No se pudo iniciar sesión. Inténtalo de nuevo.")
            }
        }
    }

    /** The credential picker is supplied by the UI, keeping Android context out of this ViewModel. */
    fun loginWithGoogle(getIdToken: suspend () -> String?) {
        if (_uiState.value is AuthUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val idToken = getIdToken()
                if (idToken == null) {
                    _uiState.value = AuthUiState.Idle
                    return@launch
                }

                val response = authRepository.loginWithGoogle(idToken)
                _uiState.value = AuthUiState.Success(user = response.usuario)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.Error(
                    message = "No se pudo continuar con Google. Inténtalo de nuevo."
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
        if (_uiState.value is AuthUiState.Loading) return

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

        if (!isOnline()) {
            _uiState.value = AuthUiState.Error(
                "Sin conexión a internet. Comprueba tu conexión antes de crear la cuenta."
            )
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {

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

            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: AuthException) {
                _uiState.value = when (exception.reason) {
                    AuthFailure.EMAIL_REGISTERED -> AuthUiState.Error(
                        "Ese correo ya tiene una cuenta. Intenta iniciar sesión.", offerLogin = true
                    )
                    AuthFailure.INVALID_DATA -> AuthUiState.Error("Revisa los datos ingresados.")
                    else -> AuthUiState.Error("El servidor presenta problemas temporales. Inténtalo más tarde.")
                }
            } catch (exception: IOException) {
                _uiState.value = AuthUiState.Error(
                    "No pudimos confirmar si tu cuenta se creó. Intenta iniciar sesión o vuelve a intentar el registro.",
                    offerLogin = true
                )
            } catch (exception: Exception) {
                _uiState.value = AuthUiState.Error("No se pudo crear la cuenta. Inténtalo de nuevo.")
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

        if (!isValidEmail(email)) {
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

        if (!isValidEmail(email)) {
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
