package com.zenmind.kotlin.features.panic.model

data class PanicUiState(
    val isLoading: Boolean = true,
    val contact: EmergencyContact? = null,
    val isOffline: Boolean = false,
    val isSaving: Boolean = false,
    val formError: String? = null,
    val message: String? = null
)
