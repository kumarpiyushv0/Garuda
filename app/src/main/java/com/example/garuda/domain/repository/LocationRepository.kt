package com.example.garuda.domain.repository

import android.location.Location
import com.example.garuda.domain.model.AppResult

interface LocationRepository {
    suspend fun uploadLocation(location: Location): AppResult<Unit>
}
