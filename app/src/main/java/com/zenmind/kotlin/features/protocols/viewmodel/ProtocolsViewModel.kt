package com.zenmind.kotlin.features.protocols.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.network.ConnectivityObserver
import com.zenmind.kotlin.features.protocols.data.ProtocolRepository
import com.zenmind.kotlin.features.protocols.model.ProtocolsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProtocolsViewModel(
    private val repository: ProtocolRepository,
    private val connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtocolsUiState())
    val uiState: StateFlow<ProtocolsUiState> = _uiState.asStateFlow()

    init {
        load()
        observeConnectivity()
    }

    fun load() {
        val guest = repository.isGuest()
        _uiState.update { it.copy(isGuest = guest) }
        if (guest) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                val remote = async(Dispatchers.IO) {
                    val sent = repository.syncPending()
                    val refreshed = repository.refreshProtocols()
                    sent to refreshed
                }

                val local = withContext(Dispatchers.IO) { repository.getLocalProtocols() }
                _uiState.update { it.copy(protocols = local, isLoading = false) }

                val (sent, refreshed) = remote.await()
                val updated = withContext(Dispatchers.IO) { repository.getLocalProtocols() }
                _uiState.update {
                    it.copy(
                        protocols = updated,
                        isOffline = !refreshed,
                        message = if (sent > 0) "Se enviaron $sent protocolos pendientes." else it.message
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "No se pudieron cargar los protocolos.") }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch(Dispatchers.Main) {
            var wasOnline: Boolean? = null
            connectivity.onlineFlow.collect { online ->
                if (!repository.isGuest()) {
                    _uiState.update { it.copy(isOffline = !online) }
                }
                if (online && wasOnline == false) load()
                wasOnline = online
            }
        }
    }

    fun openProtocol(id: Long) {
        _uiState.update { it.copy(selected = null, isLoadingDetail = true) }
        viewModelScope.launch(Dispatchers.Main) {
            val protocol = withContext(Dispatchers.IO) { repository.getProtocol(id) }
            _uiState.update { it.copy(selected = protocol, isLoadingDetail = false) }
        }
    }

    fun createProtocol(titulo: String, pasos: List<String>, onDone: () -> Unit) {
        val cleanTitle = titulo.trim()
        val cleanSteps = pasos.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanTitle.isEmpty() || cleanSteps.isEmpty() || _uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val sent = withContext(Dispatchers.IO) { repository.createProtocol(cleanTitle, cleanSteps) }
                val updated = withContext(Dispatchers.IO) { repository.getLocalProtocols() }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        protocols = updated,
                        message = if (sent) "Protocolo creado." else "Protocolo guardado en el teléfono. Se enviará cuando vuelvas a tener conexión."
                    )
                }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "No se pudo crear el protocolo. Intenta de nuevo.") }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ZenMindApplication
                ProtocolsViewModel(
                    repository = application.container.protocolRepository,
                    connectivity = application.container.connectivityObserver
                )
            }
        }
    }
}
