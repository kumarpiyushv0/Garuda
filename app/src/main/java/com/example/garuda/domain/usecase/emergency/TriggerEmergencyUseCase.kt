package com.example.garuda.domain.usecase.emergency

import com.example.garuda.domain.manager.SystemEmergencyManager
import com.example.garuda.domain.repository.ContactRepository
import com.example.garuda.domain.location.LocationClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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

            // Determine location with a strict 5-second timeout
            // We don't want a lack of GPS lock to prevent the SOS message from sending
            var location = try {
                withTimeoutOrNull(5000L) {
                    locationClient.getLocationUpdates(1000L).first()
                }
            } catch (e: Exception) {
                // Ignore location errors (like missing permissions) to ensure SMS still sends
                null
            }

            // Fallback to last known location if active fetch times out
            if (location == null) {
                try {
                    location = locationClient.getLastKnownLocation()
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val locationString = if (location != null) {
                "My location: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else {
                "Location unavailable (GPS timeout/off)."
            }
            
            // Get Contacts
            val contacts = contactRepository.allContacts.first()
            
            // Send SMS via the abstraction
            val message = "HELP! I need help. $locationString"
            
            contacts.forEach { contact ->
                systemEmergencyManager.sendSms(contact.phoneNumber, message)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
