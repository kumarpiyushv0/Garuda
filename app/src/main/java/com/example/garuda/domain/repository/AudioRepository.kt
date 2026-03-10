package com.example.garuda.domain.repository

import com.example.garuda.domain.model.AppResult
import java.io.File

interface AudioRepository {
    suspend fun uploadAudio(file: File): AppResult<String>
    suspend fun getAllRecordings(): AppResult<List<AudioMetadata>>
    suspend fun deleteRecording(metadata: AudioMetadata): AppResult<Unit>
}

/**
 * Data class for audio recording metadata
 */
data class AudioMetadata(
    val id: String,
    val fileName: String,
    val downloadUrl: String,
    val timestamp: Long,
    val duration: Long,
    val sizeBytes: Long
)
