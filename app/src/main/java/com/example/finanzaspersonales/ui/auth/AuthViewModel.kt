package com.example.finanzaspersonales.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.auth.AuthRepository
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl // Temporary direct instantiation
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

// Represents the state of the Login UI
data class LoginUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false // To trigger navigation
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Expose authentication state from repository
    val currentUserState: StateFlow<FirebaseUser?> = repository.currentUserState

    var loginUiState by mutableStateOf(LoginUiState())
        private set // Make the setter private to control state updates

    fun onEmailChange(email: String) {
        loginUiState = loginUiState.copy(emailInput = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        loginUiState = loginUiState.copy(passwordInput = password, errorMessage = null)
    }

    fun signInWithEmailPassword() {
        if (loginUiState.isLoading) return // Prevent multiple clicks

        loginUiState = loginUiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.signInWithEmailPassword(
                email = loginUiState.emailInput.trim(),
                password = loginUiState.passwordInput
            )

            result.fold(
                onSuccess = {
                    // Login successful! Update state to trigger navigation
                    loginUiState = loginUiState.copy(isLoading = false, loginSuccess = true)
                    println("Login Success: ${it.user?.email}") // Log success
                },
                onFailure = { exception ->
                    // Login failed, update state with error message
                    loginUiState = loginUiState.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "An unknown error occurred"
                    )
                     println("Login Failed: ${exception.message}") // Log failure
                }
            )
        }
    }

    fun signUpWithEmailPassword() {
        if (loginUiState.isLoading) return // Prevent multiple clicks

        loginUiState = loginUiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.signUpWithEmailPassword(
                email = loginUiState.emailInput.trim(),
                password = loginUiState.passwordInput
            )

            result.fold(
                onSuccess = {
                    // Sign up successful! Update state.
                    // Decide if you want to automatically sign in or just indicate success.
                    // For now, just mark success and clear loading.
                    loginUiState = loginUiState.copy(isLoading = false, loginSuccess = true) // Reuse loginSuccess for now
                    println("Sign Up Success: ${it.user?.email}") // Log success
                },
                onFailure = { exception ->
                    // Sign up failed, update state with error message
                    loginUiState = loginUiState.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "An unknown error occurred during sign up"
                    )
                    println("Sign Up Failed: ${exception.message}") // Log failure
                }
            )
        }
    }

    // --- Add function to handle Google Sign-In token ---
    fun signInWithGoogleToken(idToken: String?) {
        if (idToken == null) {
            loginUiState = loginUiState.copy(errorMessage = "Google Sign-In failed: No token received")
            return
        }
        if (loginUiState.isLoading) return // Prevent multiple attempts

        loginUiState = loginUiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.signInWithGoogleToken(idToken)
            result.fold(
                onSuccess = {
                    loginUiState = loginUiState.copy(isLoading = false, loginSuccess = true)
                    println("Google Sign-In Success: ${it.user?.email}")
                },
                onFailure = { exception ->
                    loginUiState = loginUiState.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Google Sign-In failed"
                    )
                    println("Google Sign-In Failed: ${exception.message}")
                }
            )
        }
    }

    // Reset login success flag after navigation has happened
    fun resetLoginSuccess() {
        loginUiState = loginUiState.copy(loginSuccess = false)
    }
} 