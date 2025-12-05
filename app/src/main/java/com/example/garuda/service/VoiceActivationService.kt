package com.example.garuda.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
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
import com.example.garuda.domain.manager.SosManager
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
    lateinit var sosManager: SosManager

    @Inject
    lateinit var hybridAnalyzer: HybridAnalyzer

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var confirmationTimer: CountDownTimer? = null
    private var isAwaitingConfirmation = false
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var previousRecognizedText: String? = null

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
                cancelConfirmation()
                stopListening()
                stopSelf()
            }
            ACTION_CONFIRM_SOS -> {
                cancelConfirmation()
                triggerSos("User confirmed via notification")
            }
            ACTION_CANCEL_SOS -> {
                cancelConfirmation()
                showListeningNotification()
                if (!isListening) startListening()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannels()
        showListeningNotification()
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

    private fun showListeningNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_LISTENING)
            .setContentTitle("Voice SOS Active")
            .setContentText("Say 'Hey Garuda Help!' for emergency")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showConfirmationNotification(seconds: Int, result: EmergencyResult) {
        val confirmIntent = Intent(this, VoiceActivationService::class.java).apply {
            action = ACTION_CONFIRM_SOS
        }
        val confirmPending = PendingIntent.getService(
            this, 0, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, VoiceActivationService::class.java).apply {
            action = ACTION_CANCEL_SOS
        }
        val cancelPending = PendingIntent.getService(
            this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confidencePercent = (result.confidence * 100).toInt()
        val notification = NotificationCompat.Builder(this, CHANNEL_CONFIRMATION)
            .setContentTitle("🚨 Emergency Detected! ($confidencePercent% confident)")
            .setContentText("Triggering in $seconds seconds...")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Reason: ${result.reason}\n\nTriggering SOS in $seconds seconds..."))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.mipmap.ic_launcher, "✓ CONFIRM SOS", confirmPending)
            .addAction(R.mipmap.ic_launcher, "✕ CANCEL", cancelPending)
            .build()

        getSystemService(NotificationManager::class.java).notify(CONFIRMATION_NOTIFICATION_ID, notification)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        isListening = true
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "Started listening")
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                else -> "Error: $error"
            }
            Log.d(TAG, "Recognition error: $msg")
            if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && isListening && !isAwaitingConfirmation) {
                restartListening()
            }
        }

        override fun onResults(results: Bundle?) {
            if (isAwaitingConfirmation) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "Results: $matches")
            matches?.firstOrNull()?.let { processRecognizedText(it) }
            if (isListening && !isAwaitingConfirmation) restartListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "Partial: $matches")
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processRecognizedText(text: String) {
        serviceScope.launch {
            try {
                val input = AnalysisInput(
                    transcribedText = text,
                    previousContext = previousRecognizedText,
                    audioToneIndicators = null
                )
                
                val result = hybridAnalyzer.analyzeEmergencyIntent(input)
                
                Log.d(TAG, "AI Result: isLegit=${result.isLegit}, confidence=${result.confidence}")
                Log.d(TAG, "  reason: ${result.reason}")
                Log.d(TAG, "  urgency: ${result.urgencyLevel}, indicators: ${result.emotionIndicators}")
                
                previousRecognizedText = text
                handleAnalysisResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing text", e)
            }
        }
    }

    private fun handleAnalysisResult(result: EmergencyResult) {
        when {
            result.confidence >= ClassificationThresholds.IMMEDIATE_TRIGGER ||
            result.urgencyLevel == UrgencyLevel.CRITICAL -> {
                triggerSos("AI: ${(result.confidence * 100).toInt()}% - ${result.reason}")
            }
            result.confidence >= ClassificationThresholds.CONFIRMATION_REQUIRED -> {
                showConfirmationWithCountdown(result)
            }
            else -> {
                Log.d(TAG, "Ignored: ${(result.confidence * 100).toInt()}% confidence")
            }
        }
    }

    private fun showConfirmationWithCountdown(result: EmergencyResult) {
        if (isAwaitingConfirmation) return
        isAwaitingConfirmation = true
        speechRecognizer?.cancel()
        
        confirmationTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(ms: Long) {
                showConfirmationNotification((ms / 1000).toInt() + 1, result)
            }
            override fun onFinish() {
                triggerSos("Timeout - ${result.reason}")
            }
        }.start()
    }

    private fun cancelConfirmation() {
        confirmationTimer?.cancel()
        confirmationTimer = null
        isAwaitingConfirmation = false
        getSystemService(NotificationManager::class.java).cancel(CONFIRMATION_NOTIFICATION_ID)
    }

    private fun triggerSos(reason: String) {
        Log.d(TAG, "Voice SOS Triggered! $reason")
        cancelConfirmation()
        isListening = false
        stopListening()
        sosManager.triggerSos()
    }

    private fun restartListening() {
        speechRecognizer?.cancel()
        android.os.Handler(mainLooper).postDelayed({
            if (isListening && !isAwaitingConfirmation) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speechRecognizer?.startListening(intent)
            }
        }, 500)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelConfirmation()
        stopListening()
        serviceScope.cancel()
        Log.d(TAG, "VoiceActivationService destroyed")
    }

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
    }
}
