package com.example.garuda.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.garuda.domain.repository.AudioRepository
import com.example.garuda.domain.repository.AudioMetadata
import com.example.garuda.domain.repository.AppPreferencesRepository
import com.example.garuda.domain.model.AppResult
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for uploading and managing emergency audio recordings in Firebase Storage
 */
@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage,
    private val appPreferencesRepository: AppPreferencesRepository
) : AudioRepository {
    companion object {
        private const val TAG = "AudioRepository"
        private const val AUDIO_FOLDER = "emergency_recordings"
    }

    /**
     * Upload an audio file to Firebase Storage and save metadata to Database
     */
    override suspend fun uploadAudio(file: File): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val timestamp = System.currentTimeMillis()
            val fileName = "audio_${timestamp}.3gp"
            
            val storageRef = firebaseStorage.reference
                .child(AUDIO_FOLDER)
                .child(userId)
                .child(fileName)
            
            storageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Log.d(TAG, "Audio uploaded successfully: $downloadUrl")
            
            val audioMetadata = mapOf(
                "fileName" to fileName,
                "downloadUrl" to downloadUrl,
                "timestamp" to timestamp,
                "duration" to getAudioDuration(file),
                "sizeBytes" to file.length()
            )
            
            val dbRef = firebaseDatabase.getReference("users/$userId/recordings/$timestamp")
            dbRef.setValue(audioMetadata).await()
            
            Log.d(TAG, "Audio metadata saved to database")
            
            AppResult.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload audio", e)
            AppResult.error("Failed to upload audio", e)
        }
    }

    /**
     * Get all audio recordings for the current user from Firebase Database
     */
    override suspend fun getAllRecordings(): AppResult<List<AudioMetadata>> = withContext(Dispatchers.IO) {
        try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val dbRef = firebaseDatabase.getReference("users/$userId/recordings")
            val snapshot = dbRef.get().await()
            
            val recordings = mutableListOf<AudioMetadata>()
            snapshot.children.forEach { child ->
                try {
                    val metadata = AudioMetadata(
                        id = child.key ?: "",
                        fileName = child.child("fileName").getValue(String::class.java) ?: "",
                        downloadUrl = child.child("downloadUrl").getValue(String::class.java) ?: "",
                        timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                        duration = child.child("duration").getValue(Long::class.java) ?: 0L,
                        sizeBytes = child.child("sizeBytes").getValue(Long::class.java) ?: 0L
                    )
                    recordings.add(metadata)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse recording: ${child.key}", e)
                }
            }
            
            AppResult.success(recordings.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recordings", e)
            AppResult.error("Failed to get recordings", e)
        }
    }

    /**
     * Delete an audio recording from Firebase Storage and Database
     */
    override suspend fun deleteRecording(metadata: AudioMetadata): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            
            val storageRef = firebaseStorage.reference
                .child(AUDIO_FOLDER)
                .child(userId)
                .child(metadata.fileName)
            storageRef.delete().await()
            
            val dbRef = firebaseDatabase.getReference("users/$userId/recordings/${metadata.id}")
            dbRef.removeValue().await()
            
            Log.d(TAG, "Recording deleted: ${metadata.fileName}")
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording", e)
            AppResult.error("Failed to delete recording", e)
        }
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
