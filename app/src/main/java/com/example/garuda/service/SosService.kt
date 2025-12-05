package com.example.garuda.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.garuda.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SosService : Service() {

    @Inject
    lateinit var audioManager: com.example.garuda.data.audio.AudioManager

    @Inject
    lateinit var locationClient: com.example.garuda.domain.location.LocationClient

    @Inject
    lateinit var locationRepository: com.example.garuda.data.repository.LocationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "sos_channel"
        val channel = NotificationChannel(
            channelId,
            "SOS Active",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SOS ACTIVE")
            .setContentText("Sharing live location and recording audio...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        
        // Start Audio
        audioManager.startRecording()

        // Start Location Updates
        locationClient.getLocationUpdates(5000L) // 5 seconds interval
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                locationRepository.uploadLocation(location)
            }
            .launchIn(serviceScope)
    }
    
    private fun stopService() {
        audioManager.stopRecording()
        serviceScope.cancel()
        stopSelf()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
