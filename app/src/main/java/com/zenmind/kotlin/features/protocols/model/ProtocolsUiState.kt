package com.zenmind.kotlin.features.protocols.model

data class ProtocolsUiState(
    val isLoading: Boolean = true,
    val isGuest: Boolean = false,
    val isOffline: Boolean = false,
    val protocols: List<Protocol> = emptyList(),
    val selected: Protocol? = null,
    val isLoadingDetail: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)
