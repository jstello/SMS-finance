package com.example.finanzaspersonales.data.auth

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepositoryImpl : AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    // Provide the current user state as a StateFlow
    override val currentUserState: StateFlow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser).isSuccess // Offer the latest user state
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { // Remove listener when Flow is cancelled
            auth.removeAuthStateListener(authStateListener)
        }
    }.stateIn( // Convert to StateFlow
        scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO), // Use an appropriate scope
        started = SharingStarted.WhileSubscribed(5000), // Keep alive 5s after last subscriber
        initialValue = auth.currentUser // Start with the current value
    )

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) { // Perform network call on IO thread
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun signOut(): Result<Unit> {
       return withContext(Dispatchers.IO) {
            try {
                auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun signInWithGoogleToken(idToken: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 