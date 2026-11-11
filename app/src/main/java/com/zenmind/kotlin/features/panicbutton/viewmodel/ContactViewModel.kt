package com.zenmind.kotlin.features.panicbutton.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepository
import com.zenmind.kotlin.features.panicbutton.data.ContactRepository
import com.zenmind.kotlin.features.panicbutton.model.SupportContact
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel (
    private val repository: ContactRepository? = null
): ViewModel() {

    companion object {
        const val MAX_NAME = 30
        const val MAX_NUMBER = 10
        const val MIN_NUMBER = 7
    }

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    val contacts: StateFlow<List<SupportContact>> = (repository?.getContacts() ?: emptyFlow())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _call = MutableSharedFlow<String>()
    val call = _call.asSharedFlow()

    fun onNameChange(text: String) {
        _name.value = text.take(MAX_NAME)
    }

    fun onTelChange(text: String) {
        _phone.value = text.filter { it.isDigit() }.take(MAX_NUMBER)
    }

    fun save(contact: SupportContact) {
        if (contact.name.isBlank() || contact.phone.length < MIN_NUMBER) return

        viewModelScope.launch {
            repository?.save(contact)
        }
    }

    fun onEmergencyPress(contact: SupportContact) {
        viewModelScope.launch { _call.emit(contact.phone) }
    }
}