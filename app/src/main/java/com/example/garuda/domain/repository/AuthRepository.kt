package com.example.garuda.domain.repository

import com.example.garuda.domain.model.AppResult

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): AppResult<Unit>
    fun signOut()
    fun isSignedIn(): Boolean
    fun getCurrentUserEmail(): String?
}
