package com.example.garuda.domain.manager

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.example.garuda.data.repository.ContactRepository
import com.example.garuda.domain.location.LocationClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Intent
import com.example.garuda.service.SosService

@Singleton
class SosManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val locationClient: LocationClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun triggerSos() {
        Log.d("SosManager", "SOS Triggered!")
        
        // Start Foreground Service
        val intent = Intent(context, SosService::class.java).apply {
            action = SosService.ACTION_START
        }
        context.startForegroundService(intent)

        scope.launch {
            try {
                // Determine location
                val location = locationClient.getLocationUpdates(5000L).first()
                val locationUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                
                // Get Contacts
                val contacts = contactRepository.allContacts.first()
                
                // Send SMS
                val smsManager = context.getSystemService(SmsManager::class.java)
                val message = "HELP! I need help. My location: $locationUrl"
                
                contacts.forEach { contact ->
                    try {
                        smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                        Log.d("SosManager", "SMS sent to ${contact.name}")
                    } catch (e: Exception) {
                        Log.e("SosManager", "Failed to send SMS to ${contact.name}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SosManager", "SOS Failed", e)
            }
        }
    }
}
