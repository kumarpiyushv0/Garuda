package com.example.garuda.data.repository

import com.example.garuda.data.local.dao.ContactDao
import com.example.garuda.data.local.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao
) {
    val allContacts: Flow<List<TrustedContactEntity>> = contactDao.getAllContacts()

    suspend fun addContact(contact: TrustedContactEntity) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: TrustedContactEntity) {
        contactDao.deleteContact(contact)
    }
}
