package com.zenmind.kotlin.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.network.ConnectivityObserver
import com.zenmind.kotlin.features.dailycheckin.data.DailyCheckInRepository
import com.zenmind.kotlin.features.dailycheckin.model.Streak
import com.zenmind.kotlin.features.profile.data.ProfileRepository
import com.zenmind.kotlin.features.profile.data.local.DepartmentEntity
import com.zenmind.kotlin.features.profile.data.local.InstitutionEntity
import com.zenmind.kotlin.features.profile.data.local.ProfileEntity
import com.zenmind.kotlin.features.profile.data.local.PendingProfileEntity
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ProfileUiState(
    val userId: String? = null,
    val profile: ProfileEntity? = null,
    val guestStreak: Streak? = null,
    val pending: PendingProfileEntity? = null,
    val institutions: List<InstitutionEntity> = emptyList(),
    val departments: List<DepartmentEntity> = emptyList(),
    val pendingDepartments: List<DepartmentEntity> = emptyList(),
    val name: String = "",
    val institutionId: String? = null,
    val departmentId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val isOnline: Boolean = true,
    val needsVerification: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val connectivity: ConnectivityObserver? = null,
    private val dailyCheckInRepository: DailyCheckInRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()
    private var profileJob: Job? = null

    fun isSessionVerified(): Boolean = _uiState.value.userId?.let(repository::isVerified) == true

    init {
        connectivity?.let { observer ->
            viewModelScope.launch {
                observer.onlineFlow.collect { online ->
                    _uiState.update { it.copy(isOnline = online,
                        needsVerification = it.userId != null && !repository.isVerified(it.userId)) }
                }
            }
        }
    }

    fun load(userId: String?) {
        if (_uiState.value.userId == userId && profileJob != null) return
        profileJob?.cancel()
        _uiState.value = ProfileUiState(userId = userId, isLoading = userId != null,
            isOnline = connectivity?.isOnline() ?: true,
            needsVerification = userId != null && !repository.isVerified(userId))
        if (userId == null) {
            viewModelScope.launch {
                try {
                    if (dailyCheckInRepository?.isGuest() == true) {
                        val streak = dailyCheckInRepository.getStreak()
                        if (_uiState.value.userId == null) _uiState.update { it.copy(guestStreak = streak) }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) { }
            }
            return
        }

        profileJob = viewModelScope.launch {
            try {
                combine(repository.observeLocalProfile(userId), repository.observePending(userId)) {
                    profile, pending -> profile to pending
                }.collect { (profile, pending) ->
                    if (_uiState.value.userId != userId) return@collect
                    val institutions = repository.getLocalInstitutions()
                    val departments = profile?.institutionId?.let { repository.getDepartments(it) }.orEmpty()
                    val pendingDepartments = pending?.pendingInstitutionId?.let { repository.getDepartments(it) }.orEmpty()
                    _uiState.update { it.copy(profile = profile, pending = pending, institutions = institutions,
                        departments = if (it.isEditing) it.departments else departments,
                        pendingDepartments = pendingDepartments, isLoading = false,
                        needsVerification = !repository.isVerified(userId)) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "No se pudo abrir el perfil local.") }
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        val userId = state.userId ?: return
        if (state.isRefreshing || state.isSaving) return
        if (!repository.isVerified(userId)) {
            _uiState.update { it.copy(needsVerification = true) }
            showMessage("Verifica tu sesión antes de actualizar el perfil.")
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                repository.refreshProfile(userId)
                if (_uiState.value.userId == userId) showMessage("Perfil actualizado.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_uiState.value.userId == userId) showMessage(errorMessage(exception))
            } finally {
                if (_uiState.value.userId == userId) _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun openEditor() {
        val state = _uiState.value
        val profile = state.profile ?: return
        val pending = state.pending
        val selectedInstitution = pending?.pendingInstitutionId ?: profile.institutionId
        _uiState.update { it.copy(name = pending?.pendingName ?: profile.name, institutionId = selectedInstitution,
            departmentId = if (pending?.pendingInstitutionId != null) pending.pendingDepartmentId else profile.departmentId,
            isEditing = true, message = null) }
        viewModelScope.launch {
            try {
                val institutions = if (_uiState.value.isOnline) repository.getInstitutions()
                    else repository.getLocalInstitutions()
                if (_uiState.value.userId != profile.userId) return@launch
                val departments = _uiState.value.institutionId?.let { repository.getDepartments(it) }.orEmpty()
                if (_uiState.value.userId == profile.userId) {
                    _uiState.update { it.copy(institutions = institutions, departments = departments) }
                    if (institutions.isEmpty() && !_uiState.value.isOnline) {
                        showMessage("Conéctate a internet para cargar las instituciones.")
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_uiState.value.userId == profile.userId) showMessage("No se pudo cargar el catálogo de instituciones.")
            }
        }
    }

    fun changeName(value: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(name = value.take(50), message = null) }
    }

    fun selectInstitution(id: String?) {
        val state = _uiState.value
        if (state.isSaving || (id != null && state.institutions.none { it.id == id })) return
        if (id == state.institutionId) return
        _uiState.update { it.copy(institutionId = id, departmentId = null, departments = emptyList(), message = null) }
        if (id == null) return
        viewModelScope.launch {
            try {
                val departments = repository.getDepartments(id)
                if (_uiState.value.userId == state.userId && _uiState.value.institutionId == id) {
                    _uiState.update { it.copy(departments = departments) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_uiState.value.userId == state.userId) showMessage("No se pudieron cargar los departamentos.")
            }
        }
    }

    fun selectDepartment(id: String?) {
        val state = _uiState.value
        if (state.isSaving || (id != null && state.departments.none { it.id == id && it.institutionId == state.institutionId })) return
        _uiState.update { it.copy(departmentId = id, message = null) }
    }

    fun save(onComplete: () -> Unit) {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.isSaving || state.userId != profile.userId) return
        val name = state.name.trim()
        validateName(name)?.let { showMessage(it); return }
        val institutionChanged = state.institutionId != profile.institutionId || state.departmentId != profile.departmentId
        if (institutionChanged && !validSelection(state)) return
        val nameChanged = name != profile.name
        if (!nameChanged && !institutionChanged && state.pending?.hasChanges != true) {
            showMessage("No hay cambios para guardar.")
            onComplete()
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val queued = repository.savePendingChanges(profile.userId, name, state.institutionId, state.departmentId)
                if (queued && connectivity?.isOnline() != false && repository.isVerified(profile.userId)) {
                    repository.syncPending(profile.userId)
                }
                val remaining = repository.getPending(profile.userId)?.hasChanges == true
                if (_uiState.value.userId == profile.userId) {
                    _uiState.update { it.copy(isEditing = false,
                        needsVerification = !repository.isVerified(profile.userId)) }
                    showMessage(if (remaining) "Cambios guardados en este teléfono. Pendientes de sincronización."
                        else if (queued) "Perfil sincronizado." else "No hay cambios pendientes.")
                    onComplete()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_uiState.value.userId == profile.userId) showMessage(errorMessage(exception))
            } finally {
                if (_uiState.value.userId == profile.userId) _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validSelection(state: ProfileUiState): Boolean {
        val id = state.institutionId
        if (id == null || state.institutions.none { it.id == id }) {
            showMessage("Selecciona una institución válida.")
            return false
        }
        if (state.departmentId != null && state.departments.none {
                it.id == state.departmentId && it.institutionId == id
            }) {
            showMessage("El departamento no pertenece a la institución seleccionada.")
            return false
        }
        return true
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    fun retryPending() {
        val userId = _uiState.value.userId ?: return
        if (_uiState.value.isSaving || _uiState.value.isRefreshing) return
        if (!repository.isVerified(userId)) {
            _uiState.update { it.copy(needsVerification = true) }
            showMessage("Verifica tu sesión antes de sincronizar.")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (repository.getPending(userId)?.hasChanges != true) {
                    showMessage("No hay cambios pendientes.")
                } else {
                    repository.syncPending(userId)
                    if (_uiState.value.userId == userId) {
                        _uiState.update { it.copy(needsVerification = !repository.isVerified(userId)) }
                        showMessage(
                        if (repository.getPending(userId)?.hasChanges == true) "Quedan cambios pendientes. Inténtalo de nuevo."
                        else "Perfil sincronizado."
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_uiState.value.userId == userId) showMessage(errorMessage(exception))
            } finally {
                if (_uiState.value.userId == userId) _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun cancelEdit() {
        val profile = _uiState.value.profile
        _uiState.update { it.copy(isEditing = false) }
        val institutionId = profile?.institutionId ?: return
        viewModelScope.launch {
            try {
                val departments = repository.getDepartments(institutionId)
                if (_uiState.value.userId == profile.userId && !_uiState.value.isEditing) {
                    _uiState.update { it.copy(departments = departments) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_uiState.value.userId == profile.userId) showMessage("No se pudieron cargar los departamentos.")
            }
        }
    }

    private fun showMessage(message: String) { _uiState.update { it.copy(message = message) } }

    private fun errorMessage(exception: Exception): String = when (exception) {
        is IOException -> "No pudimos confirmar el cambio. Comprueba tu conexión y vuelve a intentarlo."
        is HttpException -> when (exception.code()) {
            401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
            400, 404, 422 -> "Revisa los datos seleccionados e inténtalo de nuevo."
            else -> "El servidor no está disponible. Inténtalo más tarde."
        }
        else -> "No se pudo completar la operación. Inténtalo de nuevo."
    }

    companion object {
        fun validateName(name: String): String? {
            val trimmed = name.trim()
            if (trimmed.length !in 2..50) return "El nombre debe tener entre 2 y 50 caracteres."
            if (!trimmed.any { it.isLetter() } || !trimmed.all {
                    it.isLetter() || it == ' ' || it == '-' || it == '\'' || it == '’'
                }) return "Usa letras, espacios, guiones o apóstrofos en el nombre."
            return null
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZenMindApplication
                ProfileViewModel(app.container.profileRepository, app.container.connectivityObserver,
                    app.container.dailyCheckInRepository)
            }
        }
    }
}
