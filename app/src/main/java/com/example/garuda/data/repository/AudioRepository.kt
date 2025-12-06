package com.example.garuda.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.garuda.data.local.UserManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for uploading and managing emergency audio recordings in Firebase Storage
 */
@Singleton
class AudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseStorage: FirebaseStorage,
    private val firebaseDatabase: FirebaseDatabase,
    private val userManager: UserManager
) {
    companion object {
        private const val TAG = "AudioRepository"
        private const val AUDIO_FOLDER = "emergency_recordings"
    }

    /**
     * Upload an audio file to Firebase Storage and save metadata to Database
     */
    suspend fun uploadAudio(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = userManager.getOrGenerateUserId()
            val timestamp = System.currentTimeMillis()
            val fileName = "audio_${timestamp}.3gp"
            
            // Upload to Firebase Storage
            val storageRef = firebaseStorage.reference
                .child(AUDIO_FOLDER)
                .child(userId)
                .child(fileName)
            
            val uploadTask = storageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Log.d(TAG, "Audio uploaded successfully: $downloadUrl")
            
            // Save metadata to Firebase Database
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
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload audio", e)
            Result.failure(e)
        }
    }

    /**
     * Get all audio recordings for the current user from Firebase Database
     */
    suspend fun getAllRecordings(): List<AudioMetadata> = withContext(Dispatchers.IO) {
        try {
            val userId = userManager.getOrGenerateUserId()
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
            
            recordings.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recordings", e)
            emptyList()
        }
    }

    /**
     * Delete an audio recording from Firebase Storage and Database
     */
    suspend fun deleteRecording(metadata: AudioMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = userManager.getOrGenerateUserId()
            
            // Delete from Storage
            val storageRef = firebaseStorage.reference
                .child(AUDIO_FOLDER)
                .child(userId)
                .child(metadata.fileName)
            storageRef.delete().await()
            
            // Delete from Database
            val dbRef = firebaseDatabase.getReference("users/$userId/recordings/${metadata.id}")
            dbRef.removeValue().await()
            
            Log.d(TAG, "Recording deleted: ${metadata.fileName}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording", e)
            Result.failure(e)
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
