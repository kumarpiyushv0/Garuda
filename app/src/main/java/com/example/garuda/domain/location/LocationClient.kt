package com.example.garuda.domain.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Location>
    suspend fun getLastKnownLocation(): Location?
    class LocationException(message: String) : Exception(message)
}
