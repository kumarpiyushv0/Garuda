package com.example.garuda.data.repository

import android.util.Log
import com.example.garuda.domain.repository.AppPreferencesRepository
import com.example.garuda.data.local.dao.ContactDao
import com.example.garuda.data.local.entity.TrustedContactEntity
import com.example.garuda.domain.model.AppResult
import com.example.garuda.domain.repository.ContactRepository
import com.example.garuda.di.ApplicationScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing trusted contacts with local Room database and Firebase sync
 */
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val firebaseDatabase: FirebaseDatabase,
    private val appPreferencesRepository: AppPreferencesRepository,
    @ApplicationScope private val scope: CoroutineScope
) : ContactRepository {
    companion object {
        private const val TAG = "ContactRepository"
    }

    override val allContacts: Flow<List<TrustedContactEntity>> = contactDao.getAllContacts()

    /**
     * Add a contact to local database and sync to Firebase
     */
    override suspend fun addContact(contact: TrustedContactEntity): AppResult<Unit> {
        return try {
            val insertedId = contactDao.insertContact(contact)
            val contactWithId = contact.copy(id = insertedId.toInt())
            syncContactToCloud(contactWithId)
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add contact: ${contact.name}", e)
            AppResult.error("Failed to add contact", e)
        }
    }

    override suspend fun deleteContact(contact: TrustedContactEntity): AppResult<Unit> {
        return try {
            contactDao.deleteContact(contact)
            deleteContactFromCloud(contact)
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete contact: ${contact.name}", e)
            AppResult.error("Failed to delete contact", e)
        }
    }

    /**
     * Sync a single contact to Firebase
     */
    private suspend fun syncContactToCloud(contact: TrustedContactEntity) {
        try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val contactRef = firebaseDatabase.getReference("users/$userId/contacts/${contact.id}")
            
            val contactMap = mapOf(
                "id" to contact.id,
                "name" to contact.name,
                "phoneNumber" to contact.phoneNumber,
                "timestamp" to System.currentTimeMillis()
            )
            
            contactRef.setValue(contactMap).await()
            Log.d(TAG, "Contact synced to cloud: ${contact.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync contact to cloud: ${contact.name}", e)
        }
    }

    /**
     * Delete contact from Firebase
     */
    private suspend fun deleteContactFromCloud(contact: TrustedContactEntity) {
        try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val contactRef = firebaseDatabase.getReference("users/$userId/contacts/${contact.id}")
            contactRef.removeValue().await()
            Log.d(TAG, "Contact deleted from cloud: ${contact.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete contact from cloud: ${contact.name}", e)
        }
    }

    /**
     * Sync all local contacts to Firebase (useful for initial sync)
     */
    override suspend fun syncAllContactsToCloud(): AppResult<Unit> {
        return try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val contactsRef = firebaseDatabase.getReference("users/$userId/contacts")
            val localContacts = contactDao.getAllContactsSync()
            
            val contactsMap = mutableMapOf<String, Any>()
            localContacts.forEach { contact ->
                contactsMap[contact.id.toString()] = mapOf(
                    "id" to contact.id,
                    "name" to contact.name,
                    "phoneNumber" to contact.phoneNumber,
                    "timestamp" to System.currentTimeMillis()
                )
            }
            
            contactsRef.setValue(contactsMap).await()
            Log.d(TAG, "All contacts synced to cloud: ${localContacts.size} contacts")
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync all contacts to cloud", e)
            AppResult.error("Failed to sync contacts", e)
        }
    }

    /**
     * Fetch contacts from Firebase and update local database
     */
    override suspend fun fetchContactsFromCloud(): AppResult<Unit> {
        return try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val contactsRef = firebaseDatabase.getReference("users/$userId/contacts")
            val snapshot = contactsRef.get().await()
            
            snapshot.children.forEach { child ->
                try {
                    val name = child.child("name").getValue(String::class.java) ?: return@forEach
                    val phoneNumber = child.child("phoneNumber").getValue(String::class.java) ?: return@forEach
                    val id = child.child("id").getValue(Int::class.java) ?: 0
                    
                    val contact = TrustedContactEntity(
                        id = id,
                        name = name,
                        phoneNumber = phoneNumber
                    )
                    contactDao.insertContact(contact)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse contact from cloud: ${child.key}", e)
                }
            }
            
            Log.d(TAG, "Contacts fetched from cloud: ${snapshot.childrenCount} contacts")
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch contacts from cloud", e)
            AppResult.error("Failed to fetch contacts", e)
        }
    }
}
