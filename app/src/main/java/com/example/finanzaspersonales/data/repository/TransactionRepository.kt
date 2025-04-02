package com.example.finanzaspersonales.data.repository

import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData

/**
 * Repository interface for handling transactions
 */
interface TransactionRepository {
    
    /**
     * Get all SMS messages
     */
    suspend fun getAllSmsMessages(): List<SmsMessage>
    
    /**
     * Get transactions extracted from SMS messages
     */
    suspend fun getTransactions(): List<TransactionData>
    
    /**
     * Filter transactions by year and month
     */
    suspend fun filterTransactions(
        transactions: List<TransactionData>,
        year: Int? = null,
        month: Int? = null,
        isIncome: Boolean? = null
    ): List<TransactionData>
    
    /**
     * Get transaction by ID
     */
    suspend fun getTransactionById(id: String): TransactionData?
    
    /**
     * Get transactions by category ID
     */
    suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData>
    
    /**
     * Assign category to transaction
     */
    suspend fun assignCategoryToTransaction(transactionId: String, categoryId: String): Boolean
    
    /**
     * Refresh SMS data
     * @param limitToRecentMonths Number of months to limit SMS loading to. Use 0 for all messages.
     */
    suspend fun refreshSmsData(limitToRecentMonths: Int = 1)
    
    /**
     * Initialize transactions with saved categories
     */
    suspend fun initializeTransactions()
} 