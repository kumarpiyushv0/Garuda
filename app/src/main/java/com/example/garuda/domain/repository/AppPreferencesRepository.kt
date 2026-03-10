package com.example.garuda.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val onboardingCompleted: Flow<Boolean>
    val userId: Flow<String?>
    
    suspend fun saveOnboardingState(completed: Boolean)
    suspend fun getOrGenerateUserId(): String
}
