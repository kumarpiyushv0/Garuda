package com.example.garuda.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Delegate to create the DataStore
private val Context.garudaDataStore: DataStore<Preferences> by preferencesDataStore(name = "garuda_preferences")

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    // Get the onboarding state (default false)
    val onboardingCompleted: Flow<Boolean> = context.garudaDataStore.data
        .map { preferences ->
            preferences[onboardingKey] ?: false
        }

    // Save the onboarding state
    suspend fun saveOnboardingState(completed: Boolean) {
        context.garudaDataStore.edit { preferences ->
            preferences[onboardingKey] = completed
        }
    }
}
