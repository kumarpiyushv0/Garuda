package com.example.garuda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.domain.usecase.contact.AddContactUseCase
import com.example.garuda.domain.usecase.contact.GetContactsUseCase
import com.example.garuda.domain.usecase.contact.RemoveContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val removeContactUseCase: RemoveContactUseCase
) : ViewModel() {

    val contacts: StateFlow<List<TrustedContactEntity>> = getContactsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            addContactUseCase(name = name, phoneNumber = phoneNumber)
        }
    }

    fun deleteContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            removeContactUseCase(contact)
        }
    }
}
