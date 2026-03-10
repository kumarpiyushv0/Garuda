package com.example.garuda.domain.usecase.contact

import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(): Flow<List<TrustedContactEntity>> {
        return contactRepository.allContacts
    }
}
