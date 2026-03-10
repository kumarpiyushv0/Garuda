package com.example.garuda.data.manager

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.example.garuda.domain.manager.SystemEmergencyManager
import com.example.garuda.service.SosService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemEmergencyManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemEmergencyManager {

    companion object {
        private const val TAG = "SystemEmergencyManager"
    }

    override fun startEmergencyService() {
        try {
            val intent = Intent(context, SosService::class.java).apply {
                action = SosService.ACTION_START
            }
            context.startForegroundService(intent)
            Log.d(TAG, "Emergency service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start emergency service", e)
        }
    }

    override fun sendSms(phoneNumber: String, message: String): Result<Unit> {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            if (smsManager != null) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "SMS requested to be sent to $phoneNumber")
                Result.success(Unit)
            } else {
                Log.e(TAG, "SmsManager is null")
                Result.failure(IllegalStateException("SmsManager not available"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            Result.failure(e)
        }
    }
}
