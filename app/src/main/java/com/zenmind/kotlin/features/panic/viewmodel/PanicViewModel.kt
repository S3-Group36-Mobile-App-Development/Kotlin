package com.zenmind.kotlin.features.panic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.network.ConnectivityObserver
import com.zenmind.kotlin.core.telemetry.TelemetryRepository
import com.zenmind.kotlin.features.panic.data.PanicRepository
import com.zenmind.kotlin.features.panic.model.EmergencyContact
import com.zenmind.kotlin.features.panic.model.PanicUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PanicViewModel(
    private val repository: PanicRepository,
    private val connectivity: ConnectivityObserver,
    private val telemetry: TelemetryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PanicUiState(isOffline = !connectivity.isOnline()))
    val uiState: StateFlow<PanicUiState> = _uiState.asStateFlow()

    init {
        track("abierto")
        load()
        observeConnectivity()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val contact = withContext(Dispatchers.IO) { repository.getContact() }
                _uiState.update { it.copy(isLoading = false, contact = contact) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "No se pudo cargar tu contacto de emergencia.") }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch(Dispatchers.Main) {
            var wasOnline: Boolean? = null
            connectivity.onlineFlow.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
                if (online && wasOnline == false) {
                    _uiState.update { it.copy(message = "Conexión recuperada.") }
                }
                wasOnline = online
            }
        }
    }

    fun saveContact(nombre: String, telefono: String, relacion: String, onDone: () -> Unit) {
        if (_uiState.value.isSaving) return
        val cleanName = nombre.trim()
        val cleanPhone = telefono.filter { it.isDigit() || it == '+' }
        val error = when {
            cleanName.isEmpty() -> "Escribe el nombre de tu contacto."
            cleanPhone.count { it.isDigit() } !in MIN_DIGITS..MAX_DIGITS -> "Escribe un teléfono de 10 a 15 dígitos."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(formError = error) }
            return
        }

        val contact = EmergencyContact(cleanName, cleanPhone, relacion.trim().ifEmpty { null })
        _uiState.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) { repository.saveContact(contact) }
                _uiState.update { it.copy(isSaving = false, contact = contact, message = "Contacto de emergencia guardado.") }
                track("contacto_guardado")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, formError = "No se pudo guardar el contacto. Intenta de nuevo.") }
            }
        }
    }

    fun deleteContact() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) { repository.deleteContact() }
                _uiState.update { it.copy(contact = null, message = "Contacto de emergencia eliminado.") }
                track("contacto_eliminado")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "No se pudo eliminar el contacto.") }
            }
        }
    }

    fun onCall() {
        track("llamada")
    }

    fun onCallFailed() {
        _uiState.update { it.copy(message = "No se pudo abrir la aplicación de llamadas.") }
    }

    fun onFormShown() {
        _uiState.update { it.copy(formError = null) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun track(action: String) {
        telemetry.featureUse(SCREEN, mapOf("accion" to action))
    }

    companion object {
        private const val SCREEN = "panico"
        private const val MIN_DIGITS = 10
        private const val MAX_DIGITS = 15

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ZenMindApplication
                PanicViewModel(
                    repository = application.container.panicRepository,
                    connectivity = application.container.connectivityObserver,
                    telemetry = application.container.telemetryRepository
                )
            }
        }
    }
}
