package com.example.garuda.data.repository

import android.location.Location
import android.util.Log
import com.example.garuda.data.local.UserManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val userManager: UserManager
) {
    suspend fun uploadLocation(location: Location) {
        val userId = userManager.getOrGenerateUserId()
        val ref = firebaseDatabase.getReference("users/$userId/location")
        
        val locationMap = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )
        
        try {
            ref.setValue(locationMap).await()
            Log.d("LocationRepository", "Location uploaded: $locationMap")
        } catch (e: Exception) {
            Log.e("LocationRepository", "Failed to upload location", e)
        }
    }
}
