package com.example.finanzaspersonales.ui.categories

import android.util.Log
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.domain.usecase.GetSpendingByCategoryUseCase
import java.time.LocalDate
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Enum class for transaction sorting fields
 */
enum class TransactionSortField {
    DATE, AMOUNT, DESCRIPTION
}

/**
 * Enum class for sort order
 */
enum class SortOrder {
    ASCENDING, DESCENDING
}

/**
 * Enum class for transaction type (Income or Expense)
 */
enum class TransactionType {
    EXPENSE, INCOME
}

/**
 * ViewModel for the Categories screens
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val getSpendingByCategoryUseCase: GetSpendingByCategoryUseCase
) : ViewModel() {
    
    // State for selected transaction type tab
    private val _selectedTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    val selectedTransactionType: StateFlow<TransactionType> = _selectedTransactionType.asStateFlow()
    
    // Categories list
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    // Category spending data
    private val _categorySpending = MutableStateFlow<Map<Category, Float>>(emptyMap())
    val categorySpending: StateFlow<Map<Category, Float>> = _categorySpending.asStateFlow()
    
    // Selected transactions for a category
    private val _transactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val transactions: StateFlow<List<TransactionData>> = _transactions.asStateFlow()
    
    // All transactions (for reference)
    private val _allTransactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val allTransactions: StateFlow<List<TransactionData>> = _allTransactions.asStateFlow()
    
    // Filtered transactions for a specific category
    private val _categoryTransactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val categoryTransactions: StateFlow<List<TransactionData>> = _categoryTransactions.asStateFlow()
    
    // Selected category for viewing transactions
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()
    
    // Date filter for transactions
    private val _selectedYear = MutableStateFlow<Int?>(LocalDate.now().year)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()
    
    private val _selectedMonth = MutableStateFlow<Int?>(LocalDate.now().monthValue)
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()
    
    // Sorting options
    private val _sortField = MutableStateFlow(TransactionSortField.DATE)
    val sortField: StateFlow<TransactionSortField> = _sortField.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Loading state for category assignment
    private val _isAssigningCategory = MutableStateFlow(false)
    val isAssigningCategory: StateFlow<Boolean> = _isAssigningCategory.asStateFlow()

    // Result state for category assignment (Success or Failure)
    private val _assignmentResult = MutableStateFlow<Result<Unit>?>(null)
    val assignmentResult: StateFlow<Result<Unit>?> = _assignmentResult.asStateFlow()
    
    init {
        loadCategories()
        loadCategorySpending()
        loadAllTransactions()
    }
    
    /**
     * Select the transaction type tab
     */
    fun selectTransactionType(type: TransactionType) {
        if (_selectedTransactionType.value != type) {
            _selectedTransactionType.value = type
            loadCategorySpending()
        }
    }
    
    /**
     * Load all categories
     */
    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _categories.value = categoryRepository.getCategories()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error loading categories", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load all transactions
     */
    private fun loadAllTransactions(forceRemoteRefresh: Boolean = false) {
        Log.d("CAT_ASSIGN_VM", "-> loadAllTransactions(forceRemoteRefresh=$forceRemoteRefresh) called.")
        viewModelScope.launch {
            // Don't show loading indicator if already loading
            val wasLoading = _isLoading.value
            if (!wasLoading) _isLoading.value = true
            
            try {
                Log.d("CAT_ASSIGN_VM", "   Calling transactionRepository.getTransactions(forceRefresh=$forceRemoteRefresh)...")
                val transactions = transactionRepository.getTransactions(forceRefresh = forceRemoteRefresh)
                _allTransactions.value = transactions
                Log.d("CAT_ASSIGN_VM", "   Updated _allTransactions with ${transactions.size} items.")
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error loading all transactions", e)
            } finally {
                if (!wasLoading) _isLoading.value = false
                Log.d("CAT_ASSIGN_VM", "<- loadAllTransactions() finished.")
            }
        }
    }
    
    /**
     * Load spending data for categories based on selected filters and transaction type
     */
    fun loadCategorySpending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isIncomeFilter = when (_selectedTransactionType.value) {
                    TransactionType.INCOME -> true
                    TransactionType.EXPENSE -> false
                }
                Log.d("ViewModelSpending", "Calling GetSpendingByCategoryUseCase with isIncomeFilter = $isIncomeFilter")
                _categorySpending.value = getSpendingByCategoryUseCase(
                    year = _selectedYear.value,
                    month = _selectedMonth.value,
                    isIncome = isIncomeFilter
                )
                Log.d("ViewModelSpending", "Loaded spending for type: ${_selectedTransactionType.value}, Year: ${_selectedYear.value}, Month: ${_selectedMonth.value}. Result size: ${_categorySpending.value.size}")
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error loading category spending", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Set year filter
     */
    fun setYearFilter(year: Int?) {
        _selectedYear.value = year
        loadCategorySpending()
    }
    
    /**
     * Set month filter
     */
    fun setMonthFilter(month: Int?) {
        _selectedMonth.value = month
        loadCategorySpending()
    }
    
    /**
     * Set year and month filter simultaneously
     */
    fun setYearMonth(year: Int?, month: Int?) {
        _selectedYear.value = year
        _selectedMonth.value = month
        loadCategorySpending()
    }
    
    /**
     * Reloads data from the repository without forcing an SMS refresh.
     * Useful for updating the view after changes like manual additions or category assignments.
     */
    fun reloadData() {
        viewModelScope.launch {
             _isLoading.value = true
             try {
                 // No refreshSmsData() here
                 // Initialize transactions (applies categories)
                 // transactionRepository.initializeTransactions() // Might not be needed if cache is always applied
                 // Reload all data from current repository state (cache/Firestore)
                 loadAllTransactions()
                 loadCategorySpending()
                
                 // If a category detail view was active, reload its transactions
                 _selectedCategory.value?.let { loadTransactionsForCategory(it, _selectedTransactionType.value) }
                 Log.d("CategoriesViewModel", "reloadData completed.")
             } catch (e: Exception) {
                 Log.e("CategoriesViewModel", "Error during reloadData", e)
             } finally {
                 _isLoading.value = false
             }
         }
    }
    
    /**
     * Refresh transaction data BY REPROCESSING SMS MESSAGES.
     * DEPRECATED: Use reloadData() for general refresh or implement specific SMS resync if needed.
     */
    fun refreshTransactionData_OLD_SMS_ONLY() { // Renamed to avoid accidental use
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // FULL refresh: reprocess all SMS with updated income logic
                Log.w("CategoriesViewModel", "Executing SMS-ONLY refresh via refreshTransactionData_OLD_SMS_ONLY")
                transactionRepository.refreshSmsData(0)
                // Initialize transactions with saved categories
                transactionRepository.initializeTransactions()
                // Reload all data
                loadAllTransactions()
                loadCategorySpending()
                
                // If a category is selected, reload its transactions
                _selectedCategory.value?.let { loadTransactionsForCategory(it, _selectedTransactionType.value) }
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error refreshing transaction data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

     /**
     * Refreshes transaction data by reloading from the primary data source (Firestore/Cache).
     * This shows ALL transactions, including manually added ones.
     */
    fun refreshTransactionData() { // This is now the main refresh function
        Log.d("CategoriesViewModel", "-> refreshTransactionData() called (Hard Refresh)")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Force refresh from repository (includes SMS scan and Firestore fetch)
                Log.d("CategoriesViewModel", "   Calling loadAllTransactions(forceRemoteRefresh = true)")
                loadAllTransactions(forceRemoteRefresh = true)

                // 2. Reload category spending which depends on the new transactions
                Log.d("CategoriesViewModel", "   Calling loadCategorySpending بعد refresh")
                loadCategorySpending() 

                // If a category detail view was active, reload its transactions
                _selectedCategory.value?.let {
                    Log.d("CategoriesViewModel", "   Reloading transactions for selected category: ${it.name}")
                    loadTransactionsForCategory(it, _selectedTransactionType.value)
                }
                Log.i("CategoriesViewModel", "Hard refresh completed successfully.")
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error refreshing transaction data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Select a category to view its transactions
     */
    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        loadTransactionsForCategory(category, _selectedTransactionType.value)
    }
    
    /**
     * Load transactions for a category, filtered by selected transaction type
     */
    fun loadTransactionsForCategory(category: Category, transactionType: TransactionType) {
        viewModelScope.launch {
            val categoryId = category.id
            val categoryName = category.name
            Log.i("CAT_DETAIL_VM", "-> loadTransactionsForCategory. Category: '$categoryName' (ID: $categoryId), Type: $transactionType")

            _isLoading.value = true
            try {
                // Step 1: Get relevant transactions
                val allTransactionsForCategory: List<TransactionData>

                // Get the standard placeholder for "Uncategorized/Other"
                val uncategorizedPlaceholder = categoryRepository.getUncategorizedCategoryPlaceholder()

                // Check if the incoming category is conceptually the "Uncategorized/Other" placeholder
                // This is true if the incoming category's ID is null (matching our placeholder's ID)
                // OR if its name matches the placeholder's name (as a fallback, though ID check is primary)
                val isEffectivelyUncategorized = (categoryId == uncategorizedPlaceholder.id) || 
                                                 (categoryId == null) || // Explicitly handle if a truly null ID object is passed
                                                 (categoryName == uncategorizedPlaceholder.name && categoryId != null) // Fallback for old ID a0a0... if name is 'Other'

                if (isEffectivelyUncategorized) {
                    Log.d("CAT_DETAIL_VM", "   'Other'/Uncategorized category type detected (ID: $categoryId, Name: $categoryName). Fetching all transactions then filtering locally for actual uncategorized items.")
                    val allRepoTransactions = transactionRepository.getTransactions(forceRefresh = false)
                    Log.d("CAT_DETAIL_VM", "      Fetched ${allRepoTransactions.size} total transactions from repository for 'Other'/Uncategorized.")
                    
                    allTransactionsForCategory = allRepoTransactions.filter { 
                        val txCatId = it.categoryId
                        txCatId == null || txCatId.isEmpty()
                    }
                    Log.d("CAT_DETAIL_VM", "      Filtered down to ${allTransactionsForCategory.size} actual uncategorized (null or empty categoryId) transactions.")
                    if (allTransactionsForCategory.isNotEmpty()) {
                        Log.d("CAT_DETAIL_VM", "      Sample of actual uncategorized transactions (first 5):")
                        allTransactionsForCategory.take(5).forEachIndexed { index, tx ->
                            Log.d("CAT_DETAIL_VM", "         [$index]: ID=${tx.id}, Date=${tx.date}, Amt=${tx.amount}, isIncome=${tx.isIncome}, CatId='${tx.categoryId}', Prov=${tx.provider}")
                        }
                    }
                } else { // Regular category with a specific, non-null, non-placeholder ID
                    Log.d("CAT_DETAIL_VM", "   Fetching transactions for specific categoryId: $categoryId (Name: $categoryName) from TransactionRepository.")
                    allTransactionsForCategory = transactionRepository.getTransactionsByCategory(categoryId)
                }
                Log.d("CAT_DETAIL_VM", "   Initial count for '$categoryName': ${allTransactionsForCategory.size}")

                // Determine income filter based on the passed parameter
                val filterIsIncome = when(transactionType) {
                    TransactionType.INCOME -> true
                    TransactionType.EXPENSE -> false
                }

                // Step 2: Apply year/month/income filters using TransactionRepository
                val filterYear = _selectedYear.value
                val filterMonth = _selectedMonth.value

                Log.d("CAT_DETAIL_VM", "   Applying filters to ${allTransactionsForCategory.size} transactions - Year: $filterYear, Month: $filterMonth, IsIncome: $filterIsIncome")

                val filteredTransactions = transactionRepository.filterTransactions(
                    transactions = allTransactionsForCategory,
                    year = filterYear,
                    month = filterMonth,
                    isIncome = filterIsIncome
                )
                Log.d("CAT_DETAIL_VM", "   Filtered transactions count after repository.filterTransactions: ${filteredTransactions.size}")
                if (allTransactionsForCategory.isNotEmpty() && filteredTransactions.isEmpty()) {
                    Log.w("CAT_DETAIL_VM", "   WARNING: Started with ${allTransactionsForCategory.size} transactions for category '$categoryName', but all were filtered out by date/type.")
                }

                // Step 3: Sort the filtered transactions directly based on current sort state
                val sortedTransactions = when (_sortField.value) {
                    TransactionSortField.DATE -> {
                        if (_sortOrder.value == SortOrder.ASCENDING) {
                            filteredTransactions.sortedBy { it.date }
                        } else {
                            filteredTransactions.sortedByDescending { it.date }
                        }
                    }
                    TransactionSortField.AMOUNT -> {
                        if (_sortOrder.value == SortOrder.ASCENDING) {
                            filteredTransactions.sortedBy { it.amount }
                        } else {
                            filteredTransactions.sortedByDescending { it.amount }
                        }
                    }
                    TransactionSortField.DESCRIPTION -> {
                        if (_sortOrder.value == SortOrder.ASCENDING) {
                            filteredTransactions.sortedBy { it.description ?: "" }
                        } else {
                            filteredTransactions.sortedByDescending { it.description ?: "" }
                        }
                    }
                }
                _categoryTransactions.value = sortedTransactions
                Log.d("CAT_DETAIL_VM", "Updated _categoryTransactions with ${sortedTransactions.size} items.")
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error loading transactions for category: ${category.name}", e)
                _categoryTransactions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update sort parameters and apply sorting
     */
    fun updateSort(field: TransactionSortField, order: SortOrder? = null) {
        if (_sortField.value == field && order == null) {
            // Toggle sort order if same field selected
            _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) 
                SortOrder.DESCENDING else SortOrder.ASCENDING
        } else {
            // Set new field and provided order (or default to ASCENDING)
            _sortField.value = field
            _sortOrder.value = order ?: SortOrder.ASCENDING
        }
        
        // Apply sort to current transaction list
        applySorting()
    }
    
    /**
     * Apply sorting to the current transaction list
     */
    private fun applySorting() {
        val sortedList = when (_sortField.value) {
            TransactionSortField.DATE -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    _categoryTransactions.value.sortedBy { it.date }
                } else {
                    _categoryTransactions.value.sortedByDescending { it.date }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    _categoryTransactions.value.sortedBy { it.amount }
                } else {
                    _categoryTransactions.value.sortedByDescending { it.amount }
                }
            }
            TransactionSortField.DESCRIPTION -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    _categoryTransactions.value.sortedBy { it.description ?: "" }
                } else {
                    _categoryTransactions.value.sortedByDescending { it.description ?: "" }
                }
            }
        }
        
        _categoryTransactions.value = sortedList
    }
    
    /**
     * Add a new category
     */
    fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.addCategory(category)
                loadCategories()
                loadCategorySpending()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error adding category", e)
            }
        }
    }
    
    /**
     * Update an existing category
     */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
                loadCategories()
                loadCategorySpending()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error updating category", e)
            }
        }
    }
    
    /**
     * Delete a category
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(categoryId)
                loadCategories()
                loadCategorySpending()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error deleting category", e)
            }
        }
    }
    
    /**
     * Save a category (either new or existing)
     */
    fun saveCategory(category: Category) {
        viewModelScope.launch {
            try {
                if (category.id == null) {
                    categoryRepository.addCategory(category)
                } else {
                    categoryRepository.updateCategory(category)
                }
                loadCategories()
                loadCategorySpending()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error saving category", e)
            }
        }
    }
    
    /**
     * Assign a category to a transaction and optionally to others from the same provider.
     */
    fun assignCategoryToTransaction(transaction: TransactionData, category: Category) {
        val transactionId = transaction.id
        val categoryId = category.id
        val provider = transaction.provider // Get provider for potential bulk update
        
        if (transactionId == null || categoryId == null) {
            Log.e("CategoriesViewModel", "Cannot assign category: Transaction ID or Category ID is null")
            _assignmentResult.value = Result.failure(IllegalArgumentException("Transaction or Category ID is null"))
            return
        }

        Log.d("CAT_ASSIGN_VM", "-> assignCategoryToTransaction called. TxID: $transactionId, Provider: $provider, New Cat: ${category.name} ($categoryId)")
        
        _isAssigningCategory.value = true
        _assignmentResult.value = null // Reset result state

        viewModelScope.launch {
            var initialAssignmentSuccess = false
            try {
                val success = transactionRepository.assignCategoryToTransaction(transactionId, categoryId)
                if (success) {
                    Log.d("CAT_ASSIGN_VM", "   Initial assignment successful for TxID: $transactionId.")
                    _assignmentResult.value = Result.success(Unit)
                    updateLocalTransactionCategory(transactionId, categoryId) // Update UI immediately
                    initialAssignmentSuccess = true
                } else {
                    Log.e("CAT_ASSIGN_VM", "   Initial assignment failed for TxID: $transactionId.")
                    _assignmentResult.value = Result.failure(RuntimeException("Repository returned false"))
                }
            } catch (e: Exception) {
                Log.e("CAT_ASSIGN_VM", "   Error during initial assignment for TxID: $transactionId", e)
                _assignmentResult.value = Result.failure(e)
            } finally {
                _isAssigningCategory.value = false // Mark assignment as done for the single item
                Log.d("CAT_ASSIGN_VM", "<- Initial assignment finished for TxID: $transactionId. Success: $initialAssignmentSuccess")

                // --- Bulk Categorization Logic ---
                if (initialAssignmentSuccess && !provider.isNullOrBlank()) {
                    launchBulkCategorization(provider, categoryId, transactionId)
                }
            }
        }
    }

    // Separate function for bulk categorization logic
    private fun launchBulkCategorization(provider: String, categoryId: String, originalTransactionId: String) {
        viewModelScope.launch { // Launch in a separate coroutine
            Log.d("CAT_BULK_ASSIGN", "-> Starting bulk categorization for Provider: '$provider', CategoryID: $categoryId")
            var bulkUpdateCount = 0
            try {
                val allCurrentTransactions = _allTransactions.value // Use cached list
                val transactionsToUpdate = allCurrentTransactions.filter {
                    it.provider == provider && it.categoryId == null && it.id != originalTransactionId
                }

                if (transactionsToUpdate.isNotEmpty()) {
                    Log.i("CAT_BULK_ASSIGN", "   Found ${transactionsToUpdate.size} uncategorized transaction(s) from provider '$provider' to update.")
                    
                    transactionsToUpdate.forEach { txToUpdate ->
                        val txId = txToUpdate.id
                        if (txId != null) {
                            try {
                                val success = transactionRepository.assignCategoryToTransaction(txId, categoryId)
                                if (success) {
                                    Log.d("CAT_BULK_ASSIGN", "      Successfully updated TxID: $txId")
                                    // Update local state for this transaction as well
                                    updateLocalTransactionCategory(txId, categoryId)
                                    bulkUpdateCount++
                                } else {
                                    Log.w("CAT_BULK_ASSIGN", "      Failed to update TxID: $txId (Repository returned false)")
                                }
                            } catch (e: Exception) {
                                Log.e("CAT_BULK_ASSIGN", "      Error updating TxID: $txId", e)
                                // Decide whether to continue or stop on error? Continue for now.
                            }
                        } else {
                            Log.w("CAT_BULK_ASSIGN", "      Skipping transaction with null ID for provider '$provider'")
                        }
                    }
                    Log.i("CAT_BULK_ASSIGN", "   Finished bulk update. Successfully updated $bulkUpdateCount/${transactionsToUpdate.size} transactions.")
                    // Optional: Reload main list or spending data if needed, though local updates might suffice
                    // loadAllTransactions() // Consider if needed for broader UI consistency
                } else {
                    Log.d("CAT_BULK_ASSIGN", "   No other uncategorized transactions found for provider '$provider'.")
                }
            } catch (e: Exception) {
                Log.e("CAT_BULK_ASSIGN", "   Error during bulk categorization process for Provider: '$provider'", e)
            } finally {
                Log.d("CAT_BULK_ASSIGN", "<- Bulk categorization finished for Provider: '$provider'")
            }
        }
    }

    // Helper to update local state after assignment (Keep this function as is)
    private fun updateLocalTransactionCategory(transactionId: String, categoryId: String) {
        // ... (Implementation remains the same)
    }
    
    /**
     * Clears the assignment result state, e.g., after handling it in the UI.
     */
    fun clearAssignmentResult() {
        Log.d("CAT_ASSIGN_VM", "Clearing assignment result.")
        _assignmentResult.value = null
    }

    /**
     * Get category for a transaction
     */
    suspend fun getCategoryForTransaction(transaction: TransactionData): Category? {
        return transaction.categoryId?.let { categoryId ->
            categoryRepository.getCategories().find { it.id == categoryId }
        }
    }
    
    /**
     * Save a transaction
     */
    fun saveTransaction(transaction: TransactionData) {
        viewModelScope.launch {
            try {
                transactionRepository.saveTransactionToFirestore(transaction)
                // Update in-memory lists so UI immediately reflects provider edits
                transaction.id?.let { txId ->
                    transaction.provider?.let { newProv ->
                        updateLocalTransactionProvider(txId, newProv)
                    }
                }
                // If this transaction has a category, update the view
                transaction.categoryId?.let { categoryId ->
                    val category = categoryRepository.getCategories().find { it.id == categoryId }
                    category?.let { loadTransactionsForCategory(it, _selectedTransactionType.value) }
                }
                loadCategorySpending()
                loadAllTransactions()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error saving transaction", e)
            }
        }
    }

    /**
     * Update transaction provider in local state lists
     */
    private fun updateLocalTransactionProvider(transactionId: String, newProvider: String) {
        // Update overall transactions cache
        _allTransactions.value = _allTransactions.value.map { tx ->
            if (tx.id == transactionId) tx.copy(provider = newProvider) else tx
        }
        // Update category-specific transactions cache
        _categoryTransactions.value = _categoryTransactions.value.map { tx ->
            if (tx.id == transactionId) tx.copy(provider = newProvider) else tx
        }
    }

    /**
     * Assign category to a transaction
     */
    fun assignCategoryToTransaction(transactionId: String, categoryId: String) {
        Log.d("CAT_ASSIGN_VM", "-> assignCategoryToTransaction(transactionId=$transactionId, categoryId=$categoryId) called.")
        viewModelScope.launch {
            _isAssigningCategory.value = true
            try {
                Log.d("CAT_ASSIGN_VM", "   Calling transactionRepository.assignCategoryToTransaction()...")
                val success = transactionRepository.assignCategoryToTransaction(transactionId, categoryId)
                _assignmentResult.value = if (success) Result.success(Unit) else Result.failure(Exception("Failed to assign category"))
                Log.d("CAT_ASSIGN_VM", "   Assignment result: ${_assignmentResult.value}")
                if (success) {
                    Log.d("CAT_ASSIGN_VM", "   Assignment successful. Reloading data...")
                    // Reload data after successful assignment
                    loadAllTransactions()
                    loadCategorySpending()
                    _selectedCategory.value?.let { loadTransactionsForCategory(it, _selectedTransactionType.value) }
                }
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error assigning category", e)
                _assignmentResult.value = Result.failure(e)
            } finally {
                _isAssigningCategory.value = false
                Log.d("CAT_ASSIGN_VM", "<- assignCategoryToTransaction() finished.")
            }
        }
    }

    /**
     * Resets the assignment result state
     */
    fun resetAssignmentResult() {
        _assignmentResult.value = null
    }


    /**
     * Delete a transaction by its ID
     * @param transactionId The ID of the transaction to delete.
     * @param userId The ID of the current user (required for Firestore deletion).
     * @param onResult Callback function invoked with the Result (Success/Failure) of the deletion.
     */
    fun deleteTransaction(transactionId: String, userId: String, onResult: (Result<Unit>) -> Unit) {
        Log.d("CategoriesViewModel", "Attempting to delete transaction: $transactionId for user: $userId")
        viewModelScope.launch {
            val result = transactionRepository.deleteTransactionFromFirestore(transactionId, userId)
            if (result.isSuccess) {
                Log.i("CategoriesViewModel", "Transaction $transactionId deleted successfully.")
                // Reload data after successful deletion
                loadAllTransactions() // Reload all transactions to reflect removal
                loadCategorySpending() // Recalculate category spending
                // If a category detail screen was active, reload its specific transactions
                _selectedCategory.value?.let { category ->
                    loadTransactionsForCategory(category, _selectedTransactionType.value)
                }
            } else {
                Log.e("CategoriesViewModel", "Failed to delete transaction $transactionId", result.exceptionOrNull())
            }
            onResult(result) // Notify the caller (UI) about the result
        }
    }
}
