package com.example.garuda.domain.usecase.contact

import com.example.garuda.domain.repository.ContactRepository
import javax.inject.Inject

class SyncContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke() {
        try {
            contactRepository.fetchContactsFromCloud()
        } catch (e: Exception) {
            // Log or handle failure
            e.printStackTrace()
        }
    }
}
