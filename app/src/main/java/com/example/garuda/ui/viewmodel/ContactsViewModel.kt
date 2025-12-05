package com.example.garuda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    val contacts: StateFlow<List<TrustedContactEntity>> = contactRepository.allContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            contactRepository.addContact(
                TrustedContactEntity(name = name, phoneNumber = phoneNumber)
            )
        }
    }

    fun deleteContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
        }
    }
}
