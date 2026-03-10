package com.example.garuda.domain.repository

import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.domain.model.AppResult
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    val allContacts: Flow<List<TrustedContactEntity>>
    
    suspend fun addContact(contact: TrustedContactEntity): AppResult<Unit>
    suspend fun deleteContact(contact: TrustedContactEntity): AppResult<Unit>
    suspend fun syncAllContactsToCloud(): AppResult<Unit>
    suspend fun fetchContactsFromCloud(): AppResult<Unit>
}
