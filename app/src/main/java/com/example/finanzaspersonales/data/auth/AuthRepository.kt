package com.example.finanzaspersonales.data.auth

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow // Use StateFlow for auth state

interface AuthRepository {
    // Flow to observe the current user state
    val currentUserState: StateFlow<FirebaseUser?>

    // Function to get the current user synchronously (use sparingly)
    val currentUser: FirebaseUser?

    // Function to sign in with email and password
    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthResult> // Use Result for clear success/failure

    // Function to sign out
    suspend fun signOut(): Result<Unit>

    // Function to sign in with Google token
    suspend fun signInWithGoogleToken(idToken: String): Result<AuthResult>

    // Function to sign up with email and password
    suspend fun signUpWithEmailPassword(email: String, password: String): Result<AuthResult>

    // (We'll add registration and Google Sign-In later)
} 