package com.example.garuda.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.garuda.data.repository.AudioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio recording for emergency situations.
 * Automatically uploads recordings to Firebase Storage when stopped.
 */
@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRepository: AudioRepository
) {
    companion object {
        private const val TAG = "AudioManager"
    }

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startRecording() {
        if (recorder != null) return

        val outputFile = File(context.cacheDir, "emergency_audio_${System.currentTimeMillis()}.3gp")
        currentFile = outputFile

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                start()
                Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed", e)
            }
        }
    }

    fun stopRecording() {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "stop() failed", e)
            }
        }
        recorder = null
        
        val file = currentFile
        if (file != null && file.exists()) {
            Log.d(TAG, "Recording stopped. File saved: ${file.absolutePath}")
            
            // Upload to Firebase Storage in background
            scope.launch {
                try {
                    val result = audioRepository.uploadAudio(file)
                    result.onSuccess { url ->
                        Log.d(TAG, "Recording uploaded successfully: $url")
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to upload recording", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Upload error", e)
                }
            }
        }
        
        currentFile = null
    }

    /**
     * Get the current recording file (if recording is in progress)
     */
    fun getCurrentFile(): File? = currentFile

    /**
     * Check if recording is currently in progress
     */
    fun isRecording(): Boolean = recorder != null
}
