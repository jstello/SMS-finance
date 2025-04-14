package com.example.finanzaspersonales.ui.categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import java.time.LocalDate
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 * ViewModel for the Categories screens
 */
class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
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
    
    init {
        loadCategories()
        loadCategorySpending()
        loadAllTransactions()
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
    private fun loadAllTransactions() {
        Log.d("CAT_ASSIGN_VM", "-> loadAllTransactions() called.")
        viewModelScope.launch {
            // Don't show loading indicator if already loading
            val wasLoading = _isLoading.value
            if (!wasLoading) _isLoading.value = true
            
            try {
                Log.d("CAT_ASSIGN_VM", "   Calling transactionRepository.getTransactions()...")
                val transactions = transactionRepository.getTransactions()
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
     * Load spending data for all categories
     */
    fun loadCategorySpending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _categorySpending.value = categoryRepository.getSpendingByCategory(
                    year = _selectedYear.value,
                    month = _selectedMonth.value
                )
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
     * Refresh transaction data
     */
    fun refreshTransactionData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Refresh SMS data for the most recent month only
                transactionRepository.refreshSmsData(1)
                // Initialize transactions with saved categories
                transactionRepository.initializeTransactions()
                // Reload all data
                loadAllTransactions()
                loadCategorySpending()
                
                // If a category is selected, reload its transactions
                _selectedCategory.value?.let { loadTransactionsForCategory(it) }
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
        loadTransactionsForCategory(category)
    }
    
    /**
     * Load transactions for a category
     */
    fun loadTransactionsForCategory(category: Category) {
        viewModelScope.launch {
            val categoryId = category.id
            Log.i("CAT_DETAIL_VM", "Loading transactions for category: ${category.name} (ID: $categoryId)")
            
            if (categoryId == null) {
                Log.e("CAT_DETAIL_VM", "Category ID is null, cannot load transactions.")
                _transactions.value = emptyList()
                _categoryTransactions.value = emptyList()
                return@launch
            }
            
            _isLoading.value = true
            try {
                // Step 1: Get all transactions for this category ID from the repo
                Log.d("CAT_DETAIL_VM", "Fetching transactions for categoryId: $categoryId from CategoryRepository")
                val allTransactionsForCategory = categoryRepository.getTransactionsByCategory(categoryId)
                Log.d("CAT_DETAIL_VM", "Fetched ${allTransactionsForCategory.size} raw transactions for categoryId: $categoryId")
                
                // Step 2: Apply year/month filters and isIncome=false using TransactionRepository
                val filterYear = _selectedYear.value
                val filterMonth = _selectedMonth.value
                // *** We consistently filter for isIncome = false here for the detail screen ***
                val filterIsIncome = false 
                Log.d("CAT_DETAIL_VM", "Applying filters: Year=$filterYear, Month=$filterMonth, IsIncome=$filterIsIncome")
                
                val filteredTransactions = transactionRepository.filterTransactions(
                    transactions = allTransactionsForCategory,
                    year = filterYear,
                    month = filterMonth,
                    isIncome = filterIsIncome
                )
                Log.d("CAT_DETAIL_VM", "After filtering: ${filteredTransactions.size} transactions remain")
                
                _transactions.value = filteredTransactions // Keep original _transactions update if needed elsewhere
                _categoryTransactions.value = filteredTransactions // Update the specific flow for the detail screen
                Log.i("CAT_DETAIL_VM", "Final transaction list size for UI: ${filteredTransactions.size}")
                
                // Step 3: Apply current sorting
                applySorting()
            } catch (e: Exception) {
                Log.e("CAT_DETAIL_VM", "Error loading transactions for category ${category.name}", e)
                _transactions.value = emptyList()
                _categoryTransactions.value = emptyList()
            } finally {
                _isLoading.value = false
                Log.d("CAT_DETAIL_VM", "Finished loading transactions for category ${category.name}")
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
     * Assign a category to a transaction (for direct transaction and category objects)
     */
    fun assignCategoryToTransaction(transaction: TransactionData, category: Category) {
        val transactionId = transaction.id
        val categoryId = category.id
        Log.d("CAT_ASSIGN_VM", "-> assignCategoryToTransaction(Tx: ${transactionId}, Cat: ${categoryId})")
        if (transactionId == null || categoryId == null) {
            Log.e("CAT_ASSIGN_VM", "<- Aborting: Transaction ID or Category ID is null.")
            return
        }
        viewModelScope.launch { // Runs asynchronously
            try {
                Log.d("CAT_ASSIGN_VM", "   Calling CategoryRepository.setCategoryForTransaction...")
                val success = categoryRepository.setCategoryForTransaction(transactionId, categoryId) 
                Log.d("CAT_ASSIGN_VM", "   CategoryRepository returned: $success")
                
                if (success) {
                    Log.d("CAT_ASSIGN_VM", "   Assignment successful. Triggering data reloads.")
                    loadCategorySpending()
                    loadAllTransactions() 
                    _selectedCategory.value?.let { loadTransactionsForCategory(it) }
                    // Consider adding a success message/state for the UI?
                } else {
                     Log.e("CAT_ASSIGN_VM", "   Assignment failed according to repository.")
                     // Consider adding an error message/state for the UI
                }

            } catch (e: Exception) { 
                 Log.e("CAT_ASSIGN_VM", "   Exception during category assignment.", e)
                 // Consider adding an error message/state for the UI
            } finally {
                Log.d("CAT_ASSIGN_VM", "<- assignCategoryToTransaction(Tx: ${transactionId}, Cat: ${categoryId}) finished.")
            }
        }
    }
    
    /**
     * Assign a category to a transaction by IDs
     */
    fun assignCategoryToTransaction(transactionId: String, categoryId: String) {
         Log.d("CAT_ASSIGN_VM", "-> assignCategoryToTransaction(TxID: ${transactionId}, CatID: ${categoryId})")
        viewModelScope.launch { // Runs asynchronously
             try {
                 Log.d("CAT_ASSIGN_VM", "   Calling CategoryRepository.setCategoryForTransaction...")
                 val success = categoryRepository.setCategoryForTransaction(transactionId, categoryId) 
                 Log.d("CAT_ASSIGN_VM", "   CategoryRepository returned: $success")
                 
                 if (success) {
                     Log.d("CAT_ASSIGN_VM", "   Assignment successful. Triggering data reloads.")
                     loadCategorySpending()
                     loadAllTransactions()
                     _selectedCategory.value?.let { loadTransactionsForCategory(it) }
                     // Consider adding a success message/state for the UI?
                 } else {
                      Log.e("CAT_ASSIGN_VM", "   Assignment failed according to repository.")
                      // Consider adding an error message/state for the UI
                 }
 
             } catch (e: Exception) { 
                  Log.e("CAT_ASSIGN_VM", "   Exception during category assignment by ID.", e)
                  // Consider adding an error message/state for the UI
             } finally {
                 Log.d("CAT_ASSIGN_VM", "<- assignCategoryToTransaction(TxID: ${transactionId}, CatID: ${categoryId}) finished.")
             }
        }
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
                // If this transaction has a category, update the view
                transaction.categoryId?.let { categoryId ->
                    val category = categoryRepository.getCategories().find { it.id == categoryId }
                    category?.let { loadTransactionsForCategory(it) }
                }
                loadCategorySpending()
                loadAllTransactions()
            } catch (e: Exception) {
                Log.e("CategoriesViewModel", "Error saving transaction", e)
            }
        }
    }
}
