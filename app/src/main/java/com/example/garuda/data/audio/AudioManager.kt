package com.example.garuda.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording() {
        if (recorder != null) return

        val outputFile = File(context.cacheDir, "emergency_audio_${System.currentTimeMillis()}.3gp")
        currentFile = outputFile

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                start()
                Log.d("AudioManager", "Recording started: ${outputFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("AudioManager", "prepare() failed", e)
            }
        }
    }

    fun stopRecording() {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e("AudioManager", "stop() failed", e)
            }
        }
        recorder = null
        Log.d("AudioManager", "Recording stopped. File saved: ${currentFile?.absolutePath}")
        // Here we would trigger upload logic (e.g. uploadToFirebase(currentFile))
    }
}
