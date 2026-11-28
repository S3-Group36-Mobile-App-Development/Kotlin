package com.zenmind.kotlin.features.authentication.presentation.viewmodels

import com.zenmind.kotlin.features.authentication.data.models.UserDto

sealed interface AuthUiState {

    data object CheckingSession : AuthUiState

    data class SessionUnavailable(val message: String) : AuthUiState

    data class OfflineSession(val userId: String) : AuthUiState

    data object SessionInvalidated : AuthUiState

    data object Idle : AuthUiState

    data object Loading : AuthUiState

    data class Success(
        val user: UserDto
    ) : AuthUiState

    data class Error(
        val message: String,
        val offerLogin: Boolean = false
    ) : AuthUiState
}
