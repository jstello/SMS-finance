package com.example.finanzaspersonales.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toMonth
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementation of the TransactionRepository
 */
class TransactionRepositoryImpl(
    private val context: Context,
    private val smsDataSource: SmsDataSource,
    private val extractTransactionDataUseCase: ExtractTransactionDataUseCase,
    private val categoryAssignmentUseCase: CategoryAssignmentUseCase
) : TransactionRepository {
    
    private var cachedSmsMessages: List<SmsMessage> = emptyList()
    private var cachedTransactions: List<TransactionData> = emptyList()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "transaction_categories", Context.MODE_PRIVATE
    )
    
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
        cachedSmsMessages = smsDataSource.readSmsMessages()
        cachedTransactions = extractTransactionDataUseCase.execute(cachedSmsMessages)
        
        // Apply saved category assignments and auto-categorize new transactions
        applyCategoryAssignments(cachedTransactions)
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
            getTransactions().filter { it.categoryId == categoryId }
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
     * Get the saved category ID for a transaction
     */
    private fun getSavedCategoryForTransaction(transactionId: String): String? {
        return sharedPreferences.getString(transactionId, null)
    }
    
    /**
     * Save category assignment for a transaction
     */
    private fun saveCategoryForTransaction(transactionId: String, categoryId: String) {
        sharedPreferences.edit().putString(transactionId, categoryId).apply()
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