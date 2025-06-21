package com.example.finanzaspersonales.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val sharedPrefsManager: SharedPrefsManager
    // TODO: Potentially inject CategoriesViewModel or a shared refresh trigger later
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showConfirmationDialog = MutableStateFlow(false)
    val showConfirmationDialog: StateFlow<Boolean> = _showConfirmationDialog.asStateFlow()

    // Authentication removed, no current user state

    // For Snackbar messages or other UI feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()
    
    // Forecasted income amount
    private val _forecastedIncomeAmount = MutableStateFlow(0.0f)
    val forecastedIncomeAmount: StateFlow<Float> = _forecastedIncomeAmount.asStateFlow()
    
    init {
        // Load current forecasted income amount
        _forecastedIncomeAmount.value = sharedPrefsManager.getMonthlyForecastedIncome()
    }

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
    
    /**
     * Update the forecasted income amount
     */
    fun updateForecastedIncome(amount: Float) {
        viewModelScope.launch {
            try {
                sharedPrefsManager.saveMonthlyForecastedIncome(amount)
                _forecastedIncomeAmount.value = amount
                _userMessage.value = "Forecasted income updated successfully"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update forecasted income: ${e.message}"
            }
        }
    }

    private fun performFullUserResetAndResync() {
        viewModelScope.launch {
            _isLoading.value = true
            _userMessage.value = null // Clear previous messages
            try {
                // Clear all transactions
                val result = transactionRepository.developerClearUserTransactions("")
                if (result.isSuccess) {
                    // After clearing, trigger a full data refresh/resync
                    // This will re-scan SMS
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
        }
    }
} 