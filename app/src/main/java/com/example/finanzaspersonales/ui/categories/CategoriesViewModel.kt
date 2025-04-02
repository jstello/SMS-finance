package com.example.finanzaspersonales.ui.categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toMonth
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toYear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Enum for transaction sort fields
 */
enum class TransactionSortField {
    AMOUNT,
    DATE,
    PROVIDER,
    NONE
}

/**
 * Enum for sort order
 */
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

/**
 * ViewModel for the Categories screen
 */
class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    // State for categories
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories
    
    // State for category spending
    private val _categorySpending = MutableStateFlow<Map<Category, Float>>(emptyMap())
    val categorySpending: StateFlow<Map<Category, Float>> = _categorySpending
    
    // State for selected year and month
    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear
    
    private val _selectedMonth = MutableStateFlow<Int?>(null)
    val selectedMonth: StateFlow<Int?> = _selectedMonth
    
    // State for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // State for category transactions
    private val _categoryTransactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val categoryTransactions: StateFlow<List<TransactionData>> = _categoryTransactions
    
    // State for all transactions
    private val _allTransactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val allTransactions: StateFlow<List<TransactionData>> = _allTransactions
    
    // State for sorting transactions
    private val _sortField = MutableStateFlow(TransactionSortField.NONE)
    val sortField: StateFlow<TransactionSortField> = _sortField
    
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder
    
    // Initialize the ViewModel
    init {
        loadCategories()
        loadCategorySpending()
        loadAllTransactions()
    }
    
    /**
     * Load categories
     */
    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _categories.value = categoryRepository.getCategories()
            _isLoading.value = false
        }
    }
    
    /**
     * Load category spending
     */
    fun loadCategorySpending() {
        viewModelScope.launch {
            _isLoading.value = true
            _categorySpending.value = categoryRepository.getSpendingByCategory(
                year = _selectedYear.value,
                month = _selectedMonth.value
            )
            _isLoading.value = false
        }
    }
    
    /**
     * Load all transactions
     */
    fun loadAllTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Initialize transactions with saved categories
                transactionRepository.initializeTransactions()
                
                // Get all transactions
                _allTransactions.value = transactionRepository.getTransactions()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Save a category (add if new, update if existing)
     */
    fun saveCategory(category: Category) {
        viewModelScope.launch {
            if (_categories.value.any { it.id == category.id }) {
                categoryRepository.updateCategory(category)
            } else {
                categoryRepository.addCategory(category)
            }
            loadCategories()
            loadCategorySpending()
        }
    }
    
    /**
     * Add a new category
     */
    fun addCategory(name: String, color: Int) {
        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color
            )
            categoryRepository.addCategory(newCategory)
            loadCategories()
        }
    }
    
    /**
     * Delete a category
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            loadCategories()
            loadCategorySpending()
        }
    }
    
    /**
     * Update a category
     */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            loadCategories()
            loadCategorySpending()
        }
    }
    
    /**
     * Set the selected year and month
     */
    fun setYearMonth(year: Int?, month: Int?) {
        _selectedYear.value = year
        _selectedMonth.value = month
        loadCategorySpending()
    }
    
    /**
     * Get transactions for a category
     */
    fun loadTransactionsForCategory(
        categoryId: String,
        year: Int? = null,
        month: Int? = null,
        isIncome: Boolean? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("TX_DEBUG", "========== Loading Transactions for Category ==========")
                Log.d("TX_DEBUG", "Category ID: $categoryId, Year: $year, Month: $month, isIncome: $isIncome")
                
                // Get all transactions and apply year/month filter first
                val allTransactions = transactionRepository.getTransactions()
                Log.d("TX_DEBUG", "Total transactions: ${allTransactions.size}")
                
                // Apply same filter logic as in getSpendingByCategory
                val filteredByDate = transactionRepository.filterTransactions(
                    transactions = allTransactions,
                    year = year,
                    month = month,
                    isIncome = isIncome
                )
                Log.d("TX_DEBUG", "After date/type filtering: ${filteredByDate.size}")
                
                // Check if this is the "Other" category
                val category = _categories.value.find { it.id == categoryId }
                val isOtherCategory = category?.name?.equals("Other", ignoreCase = true) ?: false
                Log.d("TX_DEBUG", "Category: ${category?.name}, Is Other Category: $isOtherCategory")
                
                // For debugging, count uncategorized transactions
                val uncategorizedCount = filteredByDate.count { it.categoryId == null }
                Log.d("TX_DEBUG", "Uncategorized transactions after filtering: $uncategorizedCount")
                
                // Get transactions for this category using EXACT same logic as in spending calculation
                val categoryTransactions = if (isOtherCategory) {
                    Log.d("TX_DEBUG", "Using Other category logic")
                    // For Other category, include both:
                    // 1. Explicitly assigned to Other (categoryId == this category's ID)
                    // 2. Null categoryId (uncategorized)
                    // 3. CategoryId that doesn't exist anymore
                    filteredByDate.filter { transaction ->
                        val txCategoryId = transaction.categoryId
                        val isNull = txCategoryId == null
                        val isOther = txCategoryId == categoryId
                        val isMissing = txCategoryId != null && _categories.value.none { it.id == txCategoryId }
                        val include = isNull || isOther || isMissing
                        
                        if (include) {
                            Log.d("TX_DEBUG", "Including transaction: ${transaction.provider}, " +
                                "Amount: ${transaction.amount}, CategoryId: ${txCategoryId ?: "null"}")
                        }
                        
                        include
                    }
                } else {
                    Log.d("TX_DEBUG", "Using regular category logic")
                    // For regular categories, just get transactions with this categoryId
                    filteredByDate.filter { it.categoryId == categoryId }
                }
                
                Log.d("TX_DEBUG", "Found ${categoryTransactions.size} transactions for category")
                
                // Apply sorting
                _categoryTransactions.value = sortTransactions(categoryTransactions)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sort transactions based on current sort field and direction
     */
    private fun sortTransactions(transactions: List<TransactionData>): List<TransactionData> {
        return when (_sortField.value) {
            TransactionSortField.AMOUNT -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    transactions.sortedBy { it.amount }
                } else {
                    transactions.sortedByDescending { it.amount }
                }
            }
            TransactionSortField.DATE -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    transactions.sortedBy { it.date }
                } else {
                    transactions.sortedByDescending { it.date }
                }
            }
            TransactionSortField.PROVIDER -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    transactions.sortedBy { it.provider ?: "" }
                } else {
                    transactions.sortedByDescending { it.provider ?: "" }
                }
            }
            TransactionSortField.NONE -> transactions
        }
    }
    
    /**
     * Update the sort field and order
     * If the same field is selected again, toggle the order
     */
    fun updateSort(field: TransactionSortField) {
        if (_sortField.value == field) {
            // Toggle order if same field
            _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) 
                SortOrder.DESCENDING else SortOrder.ASCENDING
        } else {
            // New field, default to descending
            _sortField.value = field
            _sortOrder.value = SortOrder.DESCENDING
        }
        
        // Re-sort current transactions
        _categoryTransactions.value = sortTransactions(_categoryTransactions.value)
    }
    
    /**
     * Assign category to transaction
     */
    fun assignCategoryToTransaction(transaction: TransactionData, category: Category) {
        viewModelScope.launch {
            // First, assign the category to this specific transaction
            val transactionId = generateTransactionKey(transaction)
            categoryRepository.setCategoryForTransaction(transactionId, category.id)
            
            // Update the transaction in memory
            transaction.categoryId = category.id
            
            // If transaction has a provider, assign same category to all transactions 
            // from this provider
            transaction.provider?.let { provider ->
                if (provider.isNotEmpty()) {
                    assignCategoryToProvider(provider, category.id)
                }
            }
            
            // Reload data to reflect changes
            loadCategorySpending()
            
            // If we're viewing a category's transactions, reload them too
            if (_categoryTransactions.value.isNotEmpty()) {
                val firstTransaction = _categoryTransactions.value.firstOrNull()
                firstTransaction?.categoryId?.let { loadTransactionsForCategory(it) }
            }
        }
    }
    
    /**
     * Assign category to all transactions from the same provider
     */
    private suspend fun assignCategoryToProvider(provider: String, categoryId: String) {
        // Get all transactions
        val allTransactions = transactionRepository.getTransactions()
        
        // Find all transactions with the same provider
        val matchingTransactions = allTransactions.filter { 
            it.provider == provider 
        }
        
        // Assign the category to each matching transaction
        for (transaction in matchingTransactions) {
            val transactionId = generateTransactionKey(transaction)
            categoryRepository.setCategoryForTransaction(transactionId, categoryId)
            
            // Update in-memory transaction data if found
            transaction.categoryId = categoryId
        }
    }
    
    /**
     * Get category for transaction
     */
    suspend fun getCategoryForTransaction(transaction: TransactionData): Category? {
        val transactionId = generateTransactionKey(transaction)
        val categoryId = transaction.categoryId ?: categoryRepository.getCategoryIdForTransaction(transactionId)
        return if (categoryId != null) {
            _categories.value.find { it.id == categoryId }
        } else {
            null
        }
    }
    
    /**
     * Refresh transaction data from SMS
     */
    fun refreshTransactionData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Initialize transactions with saved categories
                transactionRepository.initializeTransactions()
                
                // Refresh SMS data
                transactionRepository.refreshSmsData()
                loadAllTransactions()
                loadCategorySpending()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Generate a unique key for a transaction
     */
    private fun generateTransactionKey(transaction: TransactionData): String {
        val message = transaction.originalMessage
        val input = "${message.address}_${message.body}_${message.dateTime?.time}"
        return UUID.nameUUIDFromBytes(input.toByteArray()).toString()
    }
} 