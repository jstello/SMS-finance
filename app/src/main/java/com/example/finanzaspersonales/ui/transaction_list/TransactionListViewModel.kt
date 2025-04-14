package com.example.finanzaspersonales.ui.transaction_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionListViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val transactions: StateFlow<List<TransactionData>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("TransactionListVM", "Loading all transactions...")
            try {
                val allTransactions = transactionRepository.getTransactions()
                _transactions.value = allTransactions
                Log.d("TransactionListVM", "Successfully loaded ${allTransactions.size} transactions.")
            } catch (e: Exception) {
                Log.e("TransactionListVM", "Error loading transactions", e)
                _error.value = "Failed to load transactions: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // TODO: Add function to handle assigning a category
    fun assignCategory(transactionId: String, categoryId: String) {
        viewModelScope.launch {
            Log.d("TransactionListVM", "Attempting to assign category $categoryId to transaction $transactionId")
            val success = transactionRepository.assignCategoryToTransaction(transactionId, categoryId)
            if (success) {
                Log.i("TransactionListVM", "Category assigned successfully. Reloading transactions...")
                // Reload or update the specific transaction in the list
                loadTransactions() // Simple reload for now
            } else {
                Log.e("TransactionListVM", "Failed to assign category.")
                _error.value = "Failed to assign category."
                // Consider showing a more specific error if the repository provides one
            }
        }
    }
} 