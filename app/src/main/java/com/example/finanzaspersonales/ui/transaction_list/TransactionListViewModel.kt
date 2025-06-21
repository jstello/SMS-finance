package com.example.finanzaspersonales.ui.transaction_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle
import com.example.finanzaspersonales.ui.providers.ProvidersActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Enum for different transaction types
enum class TransactionType { INCOME, EXPENSE, ALL }

// Enum for sorting options
enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

// UI model combining transaction data with its resolved category name
data class TransactionUiModel(
    val transaction: TransactionData,
    val categoryName: String?,
    val categoryColor: Int?
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Expose list of transactions enriched with category names
    private val _transactionItems = MutableStateFlow<List<TransactionUiModel>>(emptyList())
    val transactionItems: StateFlow<List<TransactionUiModel>> = _transactionItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val sortOrder: StateFlow<SortOrder> = savedStateHandle.getStateFlow(KEY_SORT_ORDER, SortOrder.DATE_DESC)

    // Category assignment state
    private val _isAssigningCategory = MutableStateFlow(false)
    val isAssigningCategory: StateFlow<Boolean> = _isAssigningCategory.asStateFlow()

    private val _assignmentResult = MutableStateFlow<Result<Unit>?>(null)
    val assignmentResult: StateFlow<Result<Unit>?> = _assignmentResult.asStateFlow()

    // Categories for selection
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    companion object {
        const val EXTRA_PROVIDER_FILTER = "extra_provider_filter"
        const val EXTRA_FROM_DATE = "extra_from_date"
        const val EXTRA_TO_DATE = "extra_to_date"
        private const val KEY_SORT_ORDER = "key_sort_order"
    }

    val providerFilter: String? = savedStateHandle[ProvidersActivity.EXTRA_PROVIDER_FILTER]
    private val fromDate: Long? = savedStateHandle[ProvidersActivity.EXTRA_FROM_DATE]
    private val toDate: Long? = savedStateHandle[ProvidersActivity.EXTRA_TO_DATE]

    init {
        // Log the value received from SavedStateHandle immediately
        Log.d("TX_LIST_VM_INIT", "Provider filter from SavedStateHandle: '$providerFilter'")
        Log.d("TX_LIST_VM_INIT", "Date range: $fromDate -> $toDate")
        loadTransactions()
        loadCategories()
    }

    fun updateSortOrder(newSortOrder: SortOrder) {
        savedStateHandle[KEY_SORT_ORDER] = newSortOrder
        loadTransactions()
    }

    /**
     * Assign category to a transaction
     */
    fun assignCategoryToTransaction(transactionId: String, categoryId: String) {
        Log.d("TX_LIST_VM", "-> assignCategoryToTransaction(transactionId=$transactionId, categoryId=$categoryId) called.")
        viewModelScope.launch {
            _isAssigningCategory.value = true
            _assignmentResult.value = null
            try {
                Log.d("TX_LIST_VM", "   Calling transactionRepository.assignCategoryToTransaction()...")
                val success = transactionRepository.assignCategoryToTransaction(transactionId, categoryId)
                
                if (success) {
                    Log.d("TX_LIST_VM", "   Category assignment successful.")
                    _assignmentResult.value = Result.success(Unit)
                    // Reload transactions to reflect the category change
                    loadTransactions()
                } else {
                    Log.e("TX_LIST_VM", "   Category assignment failed.")
                    _assignmentResult.value = Result.failure(Exception("Failed to assign category"))
                }
            } catch (e: Exception) {
                Log.e("TX_LIST_VM", "Error assigning category", e)
                _assignmentResult.value = Result.failure(e)
            } finally {
                _isAssigningCategory.value = false
                Log.d("TX_LIST_VM", "<- assignCategoryToTransaction() finished.")
            }
        }
    }

    /**
     * Load categories for selection
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = categoryRepository.getCategories()
                Log.d("TX_LIST_VM", "Categories loaded: ${_categories.value.size}")
            } catch (e: Exception) {
                Log.e("TX_LIST_VM", "Error loading categories", e)
                _categories.value = emptyList()
            }
        }
    }

    /**
     * Clear assignment result
     */
    fun clearAssignmentResult() {
        _assignmentResult.value = null
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("TX_LIST_VM", "-> loadTransactions called with filter: $providerFilter, from: $fromDate, to: $toDate")

            try {
                // Fetch filtered transactions directly from the repository
                var transactions = transactionRepository.getTransactions(
                    forceRefresh = false, // Or true, depending on desired behavior
                    providerName = providerFilter,
                    from = fromDate,
                    to = toDate
                )
                Log.d("TX_LIST_VM", "   Repo returned ${transactions.size} filtered transactions.")

                // Apply sorting
                transactions = when (sortOrder.value) {
                    SortOrder.DATE_DESC -> transactions.sortedByDescending { it.date }
                    SortOrder.DATE_ASC -> transactions.sortedBy { it.date }
                    SortOrder.AMOUNT_DESC -> transactions.sortedByDescending { it.amount }
                    SortOrder.AMOUNT_ASC -> transactions.sortedBy { it.amount }
                }


                // Resolve category names and colors by ID
                val categories = categoryRepository.getCategories()
                val categoryMap = categories.associateBy { it.id } // Create map for efficient lookup

                val uiModels = transactions.map { tx ->
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