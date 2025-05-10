package com.example.finanzaspersonales.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.auth.AuthRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
    // TODO: Potentially inject CategoriesViewModel or a shared refresh trigger later
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showConfirmationDialog = MutableStateFlow(false)
    val showConfirmationDialog: StateFlow<Boolean> = _showConfirmationDialog.asStateFlow()

    // Observe current user state via StateFlow
    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUserState

    // For Snackbar messages or other UI feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun onConfirmReset() {
        _showConfirmationDialog.value = false
        performFullUserResetAndResync()
    }

    fun onDismissDialog() {
        _showConfirmationDialog.value = false
    }

    /**
     * Show the confirmation dialog.
     */
    fun onShowDialog() {
        _showConfirmationDialog.value = true
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun performFullUserResetAndResync() {
        viewModelScope.launch {
            currentUser.value?.uid?.let {
                userId ->
                _isLoading.value = true
                _userMessage.value = null // Clear previous messages
                try {
                    val result = transactionRepository.developerClearUserTransactions(userId)
                    if (result.isSuccess) {
                        // After clearing, trigger a full data refresh/resync
                        // This will re-scan SMS and re-upload to Firestore
                        transactionRepository.getTransactions(forceRefresh = true)
                        _userMessage.value = "Successfully cleared and resynced data."
                    } else {
                        _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to clear user transactions."
                    }
                } catch (e: Exception) {
                    _userMessage.value = "An error occurred: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            } ?: run {
                _userMessage.value = "User not logged in."
            }
        }
    }
} 