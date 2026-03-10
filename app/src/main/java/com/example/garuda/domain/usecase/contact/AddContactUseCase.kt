package com.example.garuda.domain.usecase.contact

import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.domain.repository.ContactRepository
import javax.inject.Inject

class AddContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(name: String, phoneNumber: String): Result<Unit> {
        return try {
            val contact = TrustedContactEntity(name = name, phoneNumber = phoneNumber)
            contactRepository.addContact(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
