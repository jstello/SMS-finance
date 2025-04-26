package com.example.finanzaspersonales.ui.transaction_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle
import com.example.finanzaspersonales.ui.providers.ProvidersActivity

// UI model combining transaction data with its resolved category name
data class TransactionUiModel(
    val transaction: TransactionData,
    val categoryName: String?,
    val categoryColor: Int?
)

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Expose list of transactions enriched with category names
    private val _transactionItems = MutableStateFlow<List<TransactionUiModel>>(emptyList())
    val transactionItems: StateFlow<List<TransactionUiModel>> = _transactionItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val providerFilter: String? = savedStateHandle[ProvidersActivity.EXTRA_PROVIDER_FILTER]

    init {
        // Log the value received from SavedStateHandle immediately
        Log.d("TX_LIST_VM_INIT", "Provider filter from SavedStateHandle: '$providerFilter'")
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            val filter = providerFilter
            _isLoading.value = true
            _error.value = null
            Log.d("TX_LIST_VM", "-> loadTransactions called")
            if (filter != null) {
                Log.d("TX_LIST_VM", "   Applying provider filter: '$filter'")
            }
            try {
                val result = transactionRepository.getTransactions()
                Log.d("TX_LIST_VM", "   Original transactions: count=${result.size}, sample providers=${result.take(5).map { it.provider ?: it.contactName }}")
                // Apply provider filter if needed, checking both contactName and provider
                val filtered = if (filter != null) {
                    val trimmedFilter = filter.trim() // Trim filter once
                    if (trimmedFilter.equals("Unknown", ignoreCase = true)) {
                        // Match transactions where both contactName and provider are null/empty, or explicitly "Unknown"
                        result.filter { 
                            (it.contactName == null || it.contactName.isBlank()) && (it.provider == null || it.provider.isBlank()) ||
                            it.contactName?.trim().equals(trimmedFilter, ignoreCase = true) || 
                            it.provider?.trim().equals(trimmedFilter, ignoreCase = true)
                        }
                    } else {
                        // Match if either contactName or provider matches the filter (case-insensitive, trimmed)
                        result.filter { 
                            it.contactName?.trim().equals(trimmedFilter, ignoreCase = true) || 
                            it.provider?.trim().equals(trimmedFilter, ignoreCase = true) 
                        }
                    }
                } else result
                Log.d("TX_LIST_VM", "   Filtered transactions: count=${filtered.size}")

                // Resolve category names and colors by ID
                val categories = categoryRepository.getCategories()
                val categoryMap = categories.associateBy { it.id } // Create map for efficient lookup

                val uiModels = filtered.map { tx ->
                    val category = categoryMap[tx.categoryId]
                    TransactionUiModel(
                        transaction = tx,
                        categoryName = category?.name,
                        categoryColor = category?.color // Get color from the category object
                    )
                }
                _transactionItems.value = uiModels
                Log.d("TX_LIST_VM", "   Successfully loaded ${uiModels.size} UI transaction items.")
            } catch (e: Exception) {
                Log.e("TX_LIST_VM", "   Error loading transactions", e)
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