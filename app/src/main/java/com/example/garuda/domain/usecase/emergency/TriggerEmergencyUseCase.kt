package com.example.garuda.domain.usecase.emergency

import com.example.garuda.domain.manager.SystemEmergencyManager
import com.example.garuda.domain.repository.ContactRepository
import com.example.garuda.domain.location.LocationClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case responsible for orchestrating an emergency SOS event.
 * Triggers background recording, gets location, and sends SMS alerts.
 */
class TriggerEmergencyUseCase @Inject constructor(
    private val systemEmergencyManager: SystemEmergencyManager,
    private val contactRepository: ContactRepository,
    private val locationClient: LocationClient
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            // Start Foreground Service via the abstraction
            systemEmergencyManager.startEmergencyService()

            // Determine location
            val location = locationClient.getLocationUpdates(5000L).first()
            val locationUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            
            // Get Contacts
            val contacts = contactRepository.allContacts.first()
            
            // Send SMS via the abstraction
            val message = "HELP! I need help. My location: $locationUrl"
            
            contacts.forEach { contact ->
                systemEmergencyManager.sendSms(contact.phoneNumber, message)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
