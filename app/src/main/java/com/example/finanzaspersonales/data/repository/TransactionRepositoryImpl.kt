package com.example.finanzaspersonales.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toMonth
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import android.util.Log

/**
 * Implementation of the TransactionRepository
 */
class TransactionRepositoryImpl(
    private val context: Context,
    private val smsDataSource: SmsDataSource,
    private val extractTransactionDataUseCase: ExtractTransactionDataUseCase,
    private val categoryAssignmentUseCase: CategoryAssignmentUseCase,
    private val sharedPrefsManager: SharedPrefsManager
) : TransactionRepository {
    
    private var cachedSmsMessages: List<SmsMessage> = emptyList()
    private var cachedTransactions: List<TransactionData> = emptyList()
    
    // Transaction-category mappings cache to avoid frequent disk reads
    private var transactionCategoryCache: MutableMap<String, String> = mutableMapOf()
    
    init {
        // Load the transaction category mappings into memory
        loadTransactionCategoryCache()
    }
    
    /**
     * Initialize transactions with saved categories without refreshing SMS
     * This is useful to restore categories when the app starts
     */
    override suspend fun initializeTransactions() = withContext(Dispatchers.IO) {
        if (cachedTransactions.isNotEmpty()) {
            // Apply category assignments to already loaded transactions
            applyCategoryAssignments(cachedTransactions)
        }
    }
    
    /**
     * Load the transaction to category mappings from SharedPrefsManager into memory
     */
    private fun loadTransactionCategoryCache() {
        transactionCategoryCache = sharedPrefsManager.loadTransactionCategories().toMutableMap()
    }
    
    /**
     * Persist the category mappings to SharedPreferences
     */
    private fun saveTransactionCategoryCache() {
        sharedPrefsManager.saveTransactionCategories(transactionCategoryCache)
    }
    
    /**
     * Get all SMS messages
     */
    override suspend fun getAllSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (cachedSmsMessages.isEmpty()) {
            refreshSmsData()
        }
        cachedSmsMessages
    }
    
    /**
     * Get transactions extracted from SMS messages
     */
    override suspend fun getTransactions(): List<TransactionData> = withContext(Dispatchers.IO) {
        if (cachedTransactions.isEmpty()) {
            refreshSmsData()
        } else {
            // Even if transactions are cached, ensure they have category assignments
            applyCategoryAssignments(cachedTransactions)
        }
        cachedTransactions
    }
    
    /**
     * Filter transactions by year, month, and type
     */
    override suspend fun filterTransactions(
        transactions: List<TransactionData>,
        year: Int?,
        month: Int?,
        isIncome: Boolean?
    ): List<TransactionData> = withContext(Dispatchers.Default) {
        // Add debug logging
        Log.d("TX_FILTER", "========== Filtering Transactions ==========")
        Log.d("TX_FILTER", "Year: $year, Month: $month, isIncome: $isIncome")
        Log.d("TX_FILTER", "Total transactions before filtering: ${transactions.size}")
        
        // Count income vs expense transactions
        val incomeCount = transactions.count { it.isIncome }
        val expenseCount = transactions.count { !it.isIncome }
        Log.d("TX_FILTER", "Income transactions: $incomeCount, Expense transactions: $expenseCount")
        
        // Filter transactions
        val filteredTransactions = transactions.filter { transaction ->
            val txYear = transaction.date.toYear()
            val txMonth = transaction.date.toMonth()
            
            val matchesYear = year?.let { txYear == it } ?: true
            val matchesMonth = month?.let { txMonth == it } ?: true
            
            // IMPORTANT: Default is to show expenses (isIncome == false)
            // When isIncome is null, show all transactions
            val matchesType = if (isIncome == null) {
                true // If isIncome is null, include all transactions
            } else {
                transaction.isIncome == isIncome // Otherwise filter by type
            }
            
            val matches = matchesYear && matchesMonth && matchesType
            matches
        }
        
        // Log filtered transactions
        val filteredIncomeCount = filteredTransactions.count { it.isIncome }
        val filteredExpenseCount = filteredTransactions.count { !it.isIncome }
        Log.d("TX_FILTER", "Filtered Income: $filteredIncomeCount, Filtered Expense: $filteredExpenseCount")
        Log.d("TX_FILTER", "Total transactions after filtering: ${filteredTransactions.size}")
        
        filteredTransactions
    }
    
    /**
     * Get transaction by ID
     */
    override suspend fun getTransactionById(id: String): TransactionData? = withContext(Dispatchers.Default) {
        cachedTransactions.find { generateTransactionKey(it) == id }
    }
    
    /**
     * Refresh SMS data
     */
    override suspend fun refreshSmsData() = withContext(Dispatchers.IO) {
        if (!smsDataSource.hasReadSmsPermission()) {
            Log.w("SMS_REFRESH", "SMS permission not granted")
            return@withContext
        }
        
        try {
            val messages = smsDataSource.readSmsMessages()
            messages.chunked(50).forEach { chunk ->
                processChunk(chunk)
            }
            
            // Apply category assignments to all transactions after processing
            applyCategoryAssignments(cachedTransactions)
            
        } catch (e: Exception) {
            Log.e("SMS_REFRESH", "Error processing SMS", e)
        }
    }
    
    private suspend fun processChunk(chunk: List<SmsMessage>) {
        withContext(Dispatchers.Default) {
            chunk.mapNotNull { sms ->
                try {
                    // Extract transaction data from SMS
                    extractTransactionDataUseCase.execute(listOf(sms)).firstOrNull()
                } catch (e: Exception) {
                    Log.e("SMS_PROCESSING", "Error processing SMS: ${sms.body}", e)
                    null
                }
            }.also { transactions ->
                withContext(Dispatchers.Main) {
                    // Merge with existing transactions
                    cachedTransactions = (cachedTransactions + transactions).distinctBy { 
                        generateTransactionKey(it) 
                    }
                }
            }
        }
    }
    
    /**
     * Apply category assignments to transactions
     */
    private suspend fun applyCategoryAssignments(transactions: List<TransactionData>) {
        transactions.forEach { transaction ->
            val transactionId = generateTransactionKey(transaction)
            
            // First check if we have a saved category assignment
            val savedCategoryId = getSavedCategoryForTransaction(transactionId)
            
            if (savedCategoryId != null) {
                transaction.categoryId = savedCategoryId
            } else {
                // Auto-categorize transactions without saved categories
                val assignedCategory = categoryAssignmentUseCase.assignCategoryToTransaction(transaction)
                if (assignedCategory != null) {
                    transaction.categoryId = assignedCategory.id
                    // Save this auto-assignment
                    saveCategoryForTransaction(transactionId, assignedCategory.id)
                }
            }
        }
    }
    
    /**
     * Get transactions by category ID
     */
    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> = 
        withContext(Dispatchers.Default) {
            val transactions = getTransactions()
            val categories = sharedPrefsManager.loadCategories()
            
            // Get the requested category
            val category = categories.find { it.id == categoryId }
            
            // Check if this is the "Other" category by name
            val isOtherCategory = category?.name?.equals("Other", ignoreCase = true) ?: false
            
            // Enhanced debug logging
            Log.d("CATEGORY_TRANSACTIONS", "========== Category Transaction Debug ==========")
            Log.d("CATEGORY_TRANSACTIONS", "Category: ${category?.name} (id: $categoryId)")
            Log.d("CATEGORY_TRANSACTIONS", "Is Other category: $isOtherCategory")
            Log.d("CATEGORY_TRANSACTIONS", "Total transactions available: ${transactions.size}")
            
            // Handle "Other" category differently from regular categories
            if (isOtherCategory) {
                // For "Other" category, include both:
                // 1. Transactions explicitly assigned to the Other category
                // 2. Transactions with null categoryId (uncategorized)
                Log.d("CATEGORY_TRANSACTIONS", "Special handling for Other category")
                
                // Count uncategorized transactions
                val uncategorizedCount = transactions.count { it.categoryId == null }
                Log.d("CATEGORY_TRANSACTIONS", "Uncategorized transactions: $uncategorizedCount")
                
                // Count transactions explicitly assigned to Other
                val otherCount = transactions.count { it.categoryId == categoryId }
                Log.d("CATEGORY_TRANSACTIONS", "Explicitly Other transactions: $otherCount")
                
                // Create result including both uncategorized and explicitly Other
                val result = transactions.filter { 
                    it.categoryId == null || it.categoryId == categoryId 
                }
                
                Log.d("CATEGORY_TRANSACTIONS", "Combined Other category transactions: ${result.size}")
                
                // Sample some transactions for debugging
                result.take(5).forEach { tx ->
                    Log.d("CATEGORY_TRANSACTIONS", "Sample Other tx: ${tx.provider}, Amount: ${tx.amount}, " +
                      "Date: ${tx.date}, CategoryId: ${tx.categoryId ?: "null"}")
                }
                
                return@withContext result
            } else {
                // Normal filtering for other categories
                val result = transactions.filter { it.categoryId == categoryId }
                Log.d("CATEGORY_TRANSACTIONS", "Regular category transactions: ${result.size}")
                return@withContext result
            }
        }
    
    /**
     * Get the saved category ID for a transaction
     */
    private fun getSavedCategoryForTransaction(transactionId: String): String? {
        return transactionCategoryCache[transactionId]
    }
    
    /**
     * Save category assignment for a transaction
     */
    private fun saveCategoryForTransaction(transactionId: String, categoryId: String) {
        transactionCategoryCache[transactionId] = categoryId
        saveTransactionCategoryCache()
    }
    
    /**
     * Assign category to transaction and save the assignment
     */
    override suspend fun assignCategoryToTransaction(
        transactionId: String, 
        categoryId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val transaction = getTransactionById(transactionId)
        if (transaction != null) {
            transaction.categoryId = categoryId
            saveCategoryForTransaction(transactionId, categoryId)
            true
        } else {
            false
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