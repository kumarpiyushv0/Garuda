package com.example.garuda.data.repository

import android.location.Location
import android.util.Log
import com.example.garuda.domain.model.AppResult
import com.example.garuda.domain.repository.AppPreferencesRepository
import com.example.garuda.domain.repository.LocationRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val appPreferencesRepository: AppPreferencesRepository
) : LocationRepository {
    override suspend fun uploadLocation(location: Location): AppResult<Unit> {
        return try {
            val userId = appPreferencesRepository.getOrGenerateUserId()
            val locationRef = database.getReference("users/$userId/location")
        
            val locationMap = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to System.currentTimeMillis()
            )
        
            locationRef.setValue(locationMap).await()
            Log.d("LocationRepository", "Location uploaded: $locationMap")
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e("LocationRepository", "Failed to upload location", e)
            AppResult.error("Failed to upload location", e)
        }
    }
}
