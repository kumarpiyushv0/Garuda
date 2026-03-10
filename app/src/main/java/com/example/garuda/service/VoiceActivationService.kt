package com.example.garuda.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.garuda.R
import com.example.garuda.domain.classifier.AnalysisInput
import com.example.garuda.domain.classifier.ClassificationThresholds
import com.example.garuda.domain.classifier.EmergencyResult
import com.example.garuda.domain.classifier.HybridAnalyzer
import com.example.garuda.domain.classifier.UrgencyLevel
import com.example.garuda.domain.usecase.emergency.TriggerEmergencyUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VoiceActivationService : Service() {

    @Inject
    lateinit var triggerEmergencyUseCase: TriggerEmergencyUseCase

    @Inject
    lateinit var hybridAnalyzer: HybridAnalyzer

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var confirmationTimer: CountDownTimer? = null
    private var isWaitingForConfirmation = false
    private var currentUrgencyLevel = UrgencyLevel.UNKNOWN
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        const val TAG = "VoiceActivationService"
        const val ACTION_START = "ACTION_START_VOICE"
        const val ACTION_STOP = "ACTION_STOP_VOICE"
        const val ACTION_CONFIRM_SOS = "ACTION_CONFIRM_SOS"
        const val ACTION_CANCEL_SOS = "ACTION_CANCEL_SOS"
        const val NOTIFICATION_ID = 3
        const val CONFIRMATION_NOTIFICATION_ID = 4
        const val CHANNEL_LISTENING = "voice_activation_channel"
        const val CHANNEL_CONFIRMATION = "voice_confirmation_channel"
        private const val CONFIRMATION_TIMEOUT_MS = 10000L // 10 seconds to confirm or cancel
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceActivationService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startListening()
            }
            ACTION_STOP -> {
                cancelSosTrigger()
                stopListening()
                stopSelf()
            }
            ACTION_CONFIRM_SOS -> {
                Log.d(TAG, "SOS confirmed from notification")
                triggerFinalSos()
            }
            ACTION_CANCEL_SOS -> {
                cancelSosTrigger()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, createListeningNotification())
        Log.d(TAG, "VoiceActivationService started in foreground")
    }

    private fun createNotificationChannels() {
        val listeningChannel = NotificationChannel(
            CHANNEL_LISTENING, "Voice Activation", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Listening for SOS voice command" }

        val confirmChannel = NotificationChannel(
            CHANNEL_CONFIRMATION, "SOS Confirmation", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Confirm SOS trigger" }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(listeningChannel)
        manager.createNotificationChannel(confirmChannel)
    }

    private fun createListeningNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_LISTENING)
            .setContentTitle("Voice SOS Active")
            .setContentText("Say 'Hey Garuda Help!' for emergency")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    private fun startListening() {
        if (isListening) return
        
        Handler(Looper.getMainLooper()).post {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                Log.d(TAG, "Started listening")
            } catch (e: Exception) {
                Log.e(TAG, "Speech recognizer error", e)
                isListening = false
            }
        }
    }

    private fun stopListening() {
        Handler(Looper.getMainLooper()).post {
            try {
                speechRecognizer?.stopListening()
                isListening = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recognizer", e)
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            // Restart listening immediately if not waiting for confirmation
            if (!isWaitingForConfirmation) {
                isListening = false
                startListening()
            }
        }
        override fun onError(error: Int) {
            Log.e(TAG, "Recognition error: $error")
            isListening = false
            
            // Don't restart if it's a microphone error or we're waiting for confirmation
            if (error != SpeechRecognizer.ERROR_AUDIO && !isWaitingForConfirmation) {
                // Short delay before restarting to prevent rapid looping on errors
                Handler(Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                Log.d(TAG, "Recognized text: $text")
                analyzeSpeech(text)
            }
            
            // Restart if we haven't triggered an SOS flow
            if (!isWaitingForConfirmation) {
                isListening = false
                startListening()
            }
        }

        override fun onPartialResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                // Could do real-time analysis here for faster response
                // but for now, wait for full results
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun analyzeSpeech(text: String) {
        serviceScope.launch {
            try {
                // If we're already waiting for confirmation, check if they said "cancel"
                if (isWaitingForConfirmation) {
                    val cancelResult = hybridAnalyzer.analyzeEmergencyIntent(
                        AnalysisInput(text, "Previous intent detected. Waiting for confirmation or cancellation.")
                    )
                    
                    // If they specifically say "cancel", "stop", "false alarm"
                    val isCancel = text.lowercase().contains("cancel") || 
                                  text.lowercase().contains("stop") ||
                                  cancelResult.emotionIndicators.any { it.startsWith("false_positive", ignoreCase = true) }
                                  
                    if (isCancel) {
                        Log.d(TAG, "SOS cancelled by voice command")
                        cancelSosTrigger()
                        return@launch
                    }
                    
                    // If they confirm or say more distress words, trigger immediately
                    if (cancelResult.isLegit) {
                        Log.d(TAG, "SOS confirmed by voice command")
                        triggerFinalSos()
                        return@launch
                    }
                } else {
                    // Normal analysis flow
                    val input = AnalysisInput(text)
                    val result = hybridAnalyzer.analyzeEmergencyIntent(input)
                    
                    Log.d(TAG, "Analysis result: Legit=${result.isLegit}, Confidence=${result.confidence}, Level=${result.urgencyLevel}")
                    
                    if (result.isLegit) {
                        handleEmergencyIntent(result)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing speech", e)
            }
        }
    }

    private fun handleEmergencyIntent(result: EmergencyResult) {
        currentUrgencyLevel = result.urgencyLevel
        
        when (result.urgencyLevel) {
            UrgencyLevel.CRITICAL -> {
                // High confidence + critical urgency = Trigger immediately, no confirmation
                Log.e(TAG, "CRITICAL EMERGENCY DETECTED! Triggering SOS immediately.")
                triggerFinalSos()
            }
            else -> {
                // Medium/High confidence but not critical = Ask for confirmation
                Log.w(TAG, "Possible emergency detected. Asking for confirmation.")
                startConfirmationTimer()
            }
        }
    }

    private fun startConfirmationTimer() {
        isWaitingForConfirmation = true
        
        // Update notification to show warning and cancel button
        updateNotificationToWarning()
        
        // Start 10 second countdown
        confirmationTimer = object : CountDownTimer(CONFIRMATION_TIMEOUT_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                Log.d(TAG, "SOS triggering in $secondsLeft seconds unless cancelled")
                // Could add audio feedback here (e.g., beep)
            }

            override fun onFinish() {
                // If timer finishes without cancellation, trigger SOS
                Log.w(TAG, "Confirmation timer expired. Triggering SOS!")
                triggerFinalSos()
            }
        }.start()
        
        // Keep listening to see if they say "cancel" or "yes help"
        startListening()
    }

    private fun cancelSosTrigger() {
        isWaitingForConfirmation = false
        currentUrgencyLevel = UrgencyLevel.UNKNOWN
        
        confirmationTimer?.cancel()
        confirmationTimer = null
        
        // Reset notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        Log.d(TAG, "SOS trigger cancelled. Returning to normal listening mode.")
        
        // Ensure we're still listening
        startListening()
    }

    private fun triggerFinalSos() {
        // Stop timer if it was running
        confirmationTimer?.cancel()
        confirmationTimer = null
        isWaitingForConfirmation = false
        
        // Trigger the actual SOS flow
        serviceScope.launch {
            triggerEmergencyUseCase()
        }
        
        // Update notification to indicate SOS is active
        updateNotificationToActive()
        
        // We can stop listening now, as the SOS service will take over recording audio
        stopListening()
    }

    private fun updateNotificationToWarning() {
        val notification = NotificationCompat.Builder(this, CHANNEL_CONFIRMATION)
            .setContentTitle("🚨 Emergency Detected!")
            .setContentText("Triggering SOS in ${CONFIRMATION_TIMEOUT_MS / 1000} seconds...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(CONFIRMATION_NOTIFICATION_ID, notification)
    }

    private fun updateNotificationToActive() {
        val notification = NotificationCompat.Builder(this, CHANNEL_CONFIRMATION)
            .setContentTitle("🚨 SOS Triggered!")
            .setContentText("Alerts sent to contacts and location sharing active.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
        getSystemService(NotificationManager::class.java).notify(CONFIRMATION_NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        return createListeningNotification()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        confirmationTimer?.cancel()
        speechRecognizer?.destroy()
        serviceScope.cancel()
        Log.d(TAG, "VoiceActivationService destroyed")
    }
}
