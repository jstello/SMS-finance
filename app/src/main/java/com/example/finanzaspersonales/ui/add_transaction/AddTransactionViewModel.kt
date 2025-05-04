package com.example.finanzaspersonales.ui.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
    // TODO: Inject dependencies if using Hilt
) : ViewModel() {

    // State for categories list
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // State for saving operation
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * Load all available categories.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = categoryRepository.getCategories()
            } catch (e: Exception) {
                Log.e("AddTransactionVM", "Error loading categories", e)
                // Handle error, maybe show a message to the user
            }
        }
    }

    /**
     * Creates and saves a new transaction manually.
     */
    fun addManualTransaction(
        amount: Float,
        description: String,
        date: Date,
        isIncome: Boolean,
        categoryId: String? // Nullable if no category selected
    ) {
        if (_isSaving.value) return // Prevent multiple save attempts

        _isSaving.value = true
        _saveResult.value = null // Reset previous result

        viewModelScope.launch {
            try {
                val newTransaction = TransactionData(
                    id = null, // Firestore will generate ID, repository handles null ID case
                    userId = null, // Repository will fetch/add this
                    date = date,
                    amount = amount,
                    isIncome = isIncome,
                    description = description,
                    provider = description, // Use description as provider for manual entries? Or keep null?
                    contactName = null, // No associated contact
                    accountInfo = null, // No associated account info
                    categoryId = categoryId
                )

                Log.d("AddTransactionVM", "Attempting to save transaction: $newTransaction")
                val result = transactionRepository.saveTransactionToFirestore(newTransaction)
                _saveResult.value = result

                if(result.isSuccess) {
                    Log.i("AddTransactionVM", "Transaction saved successfully.")
                    // Optional: Refresh other relevant data sources if needed
                } else {
                     Log.e("AddTransactionVM", "Failed to save transaction", result.exceptionOrNull())
                }

            } catch (e: Exception) {
                Log.e("AddTransactionVM", "Error adding manual transaction", e)
                _saveResult.value = Result.failure(e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Resets the save result state, e.g., after the UI has reacted to it.
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }
} 