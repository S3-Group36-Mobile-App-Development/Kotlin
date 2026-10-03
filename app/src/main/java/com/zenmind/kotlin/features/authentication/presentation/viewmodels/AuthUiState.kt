package com.zenmind.kotlin.features.authentication.presentation.viewmodels

import com.zenmind.kotlin.features.authentication.data.models.UserDto

sealed interface AuthUiState {

    data object Idle : AuthUiState

    data object Loading : AuthUiState

    data class Success(
        val user: UserDto
    ) : AuthUiState

    data class Error(
        val message: String
    ) : AuthUiState
}