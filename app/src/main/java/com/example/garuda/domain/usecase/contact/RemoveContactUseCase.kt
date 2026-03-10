package com.example.garuda.domain.usecase.contact

import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.domain.repository.ContactRepository
import javax.inject.Inject

class RemoveContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(contact: TrustedContactEntity): Result<Unit> {
        return try {
            contactRepository.deleteContact(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
