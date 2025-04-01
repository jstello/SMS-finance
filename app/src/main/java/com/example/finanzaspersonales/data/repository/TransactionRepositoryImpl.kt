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
    suspend fun initializeTransactions() = withContext(Dispatchers.IO) {
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
        transactions.filter { transaction ->
            val matchesYear = year?.let { transaction.date.toYear() == it } ?: true
            val matchesMonth = month?.let { transaction.date.toMonth() == it } ?: true
            val matchesType = isIncome?.let { transaction.isIncome == it } ?: true
            
            matchesYear && matchesMonth && matchesType
        }
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
            
            // Simple approach: Check if this is the "Other" category by name
            val isOtherCategory = category?.name?.equals("Other", ignoreCase = true) ?: false
            
            // Comprehensive debug logging
            android.util.Log.d("TransactionRepo", "Getting transactions for category: ${category?.name} (id: $categoryId)")
            android.util.Log.d("TransactionRepo", "Is Other category: $isOtherCategory")
            android.util.Log.d("TransactionRepo", "Total transactions: ${transactions.size}")
            android.util.Log.d("TransactionRepo", "Transactions with null category: ${transactions.count { it.categoryId == null }}")
            android.util.Log.d("TransactionRepo", "Transactions with this category ID: ${transactions.count { it.categoryId == categoryId }}")
            
            // Get all transactions for this category
            val result = if (isOtherCategory) {
                // For "Other" category, also include transactions with null categoryId
                val filteredList = transactions.filter { 
                    it.categoryId == null || it.categoryId == categoryId 
                }
                android.util.Log.d("TransactionRepo", "Special handling for Other category - included ${filteredList.size} transactions")
                filteredList
            } else {
                // Normal filtering for other categories
                transactions.filter { it.categoryId == categoryId }
            }
            
            android.util.Log.d("TransactionRepo", "Final result: ${result.size} transactions")
            
            // If still empty but this is Other category, just return all uncategorized as a fallback
            if (result.isEmpty() && isOtherCategory) {
                // Emergency fallback - just get all null category items
                val fallback = transactions.filter { it.categoryId == null }
                android.util.Log.d("TransactionRepo", "Using fallback for Other - found ${fallback.size} uncategorized transactions")
                return@withContext fallback
            }
            
            result
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