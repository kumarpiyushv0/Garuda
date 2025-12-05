package com.example.garuda.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val USER_ID_KEY = stringPreferencesKey("user_id")

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID_KEY]
        }

    suspend fun getOrGenerateUserId(): String {
        var id = ""
        context.dataStore.edit { preferences ->
            val currentId = preferences[USER_ID_KEY]
            if (currentId == null) {
                id = UUID.randomUUID().toString()
                preferences[USER_ID_KEY] = id
            } else {
                id = currentId
            }
        }
        return id
    }
}
