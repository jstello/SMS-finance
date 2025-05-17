package com.example.finanzaspersonales.data.repository

import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData

// Data class to hold aggregated spending per provider
data class ProviderStat(val provider: String, val total: Float)

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
    suspend fun getTransactions(forceRefresh: Boolean = false): List<TransactionData>
    
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
    
    /**
     * Save transaction to Firestore
     */
    suspend fun saveTransactionToFirestore(transaction: TransactionData): Result<Unit>
    
    /**
     * Get transactions from Firestore
     */
    suspend fun getTransactionsFromFirestore(userId: String): Result<List<TransactionData>>
    
    /**
     * Update transaction in Firestore
     */
    suspend fun updateTransactionInFirestore(transaction: TransactionData): Result<Unit>
    
    /**
     * Delete transaction from Firestore
     */
    suspend fun deleteTransactionFromFirestore(transactionId: String, userId: String): Result<Unit>
    
    /**
     * Perform initial transaction sync
     */
    suspend fun performInitialTransactionSync(userId: String, syncStartDate: Long): Result<Unit>
    
    /**
     * Aggregates transaction amounts by provider within a given date range.
     *
     * @param from Start timestamp (inclusive).
     * @param to End timestamp (inclusive).
     * @return A list of ProviderStat objects, sorted descending by total amount.
     */
    suspend fun getProviderStats(from: Long, to: Long): List<ProviderStat>

    suspend fun getSmsMessages(startTimeMillis: Long, endTimeMillis: Long): List<SmsMessage>
    suspend fun refreshSmsData(lastSyncTimestamp: Long): Result<Unit>
    suspend fun developerClearUserTransactions(userId: String): Result<Unit>

    /**
     * Updates an existing transaction in the cache and Firestore.
     */
    suspend fun updateTransaction(transaction: TransactionData): Result<Unit>
} 