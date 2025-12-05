package com.example.garuda.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.garuda.R
import com.example.garuda.domain.manager.SosManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PowerButtonService : Service() {

    @Inject
    lateinit var sosManager: SosManager

    private val handler = Handler(Looper.getMainLooper())
    private var pressCount = 0
    private val resetRunnable = Runnable { resetCount() }

    // Configuration
    private val requiredPresses = 4
    private val timeWindowMs = 3000L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF -> {
                    onPowerButtonPressed()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PowerButtonService created")
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "power_button_channel"
        val channel = NotificationChannel(
            channelId,
            "Power Button Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Listening for SOS trigger"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Garuda Protection Active")
            .setContentText("Press power button 4 times quickly for SOS")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "PowerButtonService started in foreground")
    }

    private fun onPowerButtonPressed() {
        pressCount++
        Log.d(TAG, "Power button press detected. Count: $pressCount")

        // Reset the timeout
        handler.removeCallbacks(resetRunnable)
        handler.postDelayed(resetRunnable, timeWindowMs)

        if (pressCount >= requiredPresses) {
            Log.d(TAG, "SOS Triggered via Power Button!")
            triggerSos()
            resetCount()
        }
    }

    private fun triggerSos() {
        sosManager.triggerSos()
    }

    private fun resetCount() {
        pressCount = 0
        handler.removeCallbacks(resetRunnable)
        Log.d(TAG, "Press count reset")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        handler.removeCallbacks(resetRunnable)
        Log.d(TAG, "PowerButtonService destroyed")
    }

    companion object {
        const val TAG = "PowerButtonService"
        const val ACTION_START = "ACTION_START_POWER_BUTTON"
        const val ACTION_STOP = "ACTION_STOP_POWER_BUTTON"
        const val NOTIFICATION_ID = 2
    }
}
