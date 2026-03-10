package com.example.garuda.data.repository

import com.example.garuda.domain.model.AppResult
import com.example.garuda.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): AppResult<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            AppResult.success(Unit)
        } catch (e: Exception) {
            AppResult.error(e.message ?: "Authentication failed", e)
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    override fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }
}
