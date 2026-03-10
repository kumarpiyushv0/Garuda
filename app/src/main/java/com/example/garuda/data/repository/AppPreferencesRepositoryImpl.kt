package com.example.garuda.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.garuda.domain.repository.AppPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Single instance of DataStore for the entire app
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "garuda_app_preferences")

@Singleton
class AppPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppPreferencesRepository {

    private val onboardingKey = booleanPreferencesKey("onboarding_completed")
    private val userIdKey = stringPreferencesKey("user_id")

    override val onboardingCompleted: Flow<Boolean> = context.appDataStore.data
        .map { preferences ->
            preferences[onboardingKey] ?: false
        }

    override val userId: Flow<String?> = context.appDataStore.data
        .map { preferences ->
            preferences[userIdKey]
        }

    override suspend fun saveOnboardingState(completed: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[onboardingKey] = completed
        }
    }

    override suspend fun getOrGenerateUserId(): String {
        var id = ""
        context.appDataStore.edit { preferences ->
            val currentId = preferences[userIdKey]
            if (currentId == null) {
                id = UUID.randomUUID().toString()
                preferences[userIdKey] = id
            } else {
                id = currentId
            }
        }
        return id
    }
}
