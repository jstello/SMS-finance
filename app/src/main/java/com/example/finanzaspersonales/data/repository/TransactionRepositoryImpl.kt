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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

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
    
    private val db: FirebaseFirestore = Firebase.firestore

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
        Log.d("CAT_ASSIGN_REPO_T", "-> loadTransactionCategoryCache() called [SharedPreferences].")
        // Load the cache from SharedPreferences using the correct method
        try {
            transactionCategoryCache = sharedPrefsManager.loadTransactionCategories().toMutableMap()
        } catch (e: Exception) {
             Log.e("CAT_ASSIGN_REPO_T", "   Error loading transaction category cache from SharedPreferences", e)
            transactionCategoryCache = mutableMapOf() // Reset on error
        }
         Log.d("CAT_ASSIGN_REPO_T", "<- loadTransactionCategoryCache() finished. Loaded ${transactionCategoryCache.size} entries.")
    }
    
    /**
     * Persist the category mappings to SharedPreferences
     */
    private fun saveTransactionCategoryCache() {
        Log.d("CAT_ASSIGN_REPO_T", "-> saveTransactionCategoryCache() called [SharedPreferences]. Saving ${transactionCategoryCache.size} entries.")
        // Save the cache to SharedPreferences using the correct method
        try {
            sharedPrefsManager.saveTransactionCategories(transactionCategoryCache)
             Log.d("CAT_ASSIGN_REPO_T", "<- saveTransactionCategoryCache() finished.")
        } catch (e: Exception) {
             Log.e("CAT_ASSIGN_REPO_T", "   Error saving transaction category cache to SharedPreferences", e)
        }
    }
    
    /**
     * Get all SMS messages
     */
    override suspend fun getAllSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (cachedSmsMessages.isEmpty()) {
            refreshSmsData(0)
        }
        cachedSmsMessages
    }
    
    /**
     * Get transactions extracted from SMS messages
     */
    override suspend fun getTransactions(): List<TransactionData> = withContext(Dispatchers.IO) {
        Log.d("CAT_ASSIGN_REPO_T", "-> getTransactions() called.")
        if (cachedTransactions.isEmpty()) {
            Log.d("CAT_ASSIGN_REPO_T", "   Cache empty, calling refreshSmsData...")
            refreshSmsData(0) // Assuming this populates cachedTransactions
        } else {
            Log.d("CAT_ASSIGN_REPO_T", "   Cache not empty, calling applyCategoryAssignments...")
            applyCategoryAssignments(cachedTransactions) // Apply SharedPreferences categories
        }
         Log.d("CAT_ASSIGN_REPO_T", "<- getTransactions() returning ${cachedTransactions.size} items.")
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
        Log.d("TX_REPO_FILTER", "filterTransactions called with ${transactions.size} initial transactions.")
        Log.d("TX_REPO_FILTER", "Filters - Year: $year, Month: $month, IsIncome: $isIncome")
        
        var filteredList = transactions
        
        // Apply year filter
        if (year != null) {
            val initialSize = filteredList.size
            filteredList = filteredList.filter { it.date.toYear() == year }
            Log.d("TX_REPO_FILTER", "After year filter ($year): ${filteredList.size} transactions (removed ${initialSize - filteredList.size})")
        }
        
        // Apply month filter
        if (month != null) {
            val initialSize = filteredList.size
            // Note: java.util.Date month is 0-indexed, Calendar month is 0-indexed
            // LocalDate monthValue is 1-indexed. Date.toMonth() returns 1-indexed.
            // Compare the 1-indexed month parameter directly with the 1-indexed result of Date.toMonth()
            filteredList = filteredList.filter { it.date.toMonth() == month }
            Log.d("TX_REPO_FILTER", "After month filter ($month): ${filteredList.size} transactions (removed ${initialSize - filteredList.size})")
        }
        
        // Apply income/expense filter
        if (isIncome != null) {
            val initialSize = filteredList.size
            filteredList = filteredList.filter { it.isIncome == isIncome }
            Log.d("TX_REPO_FILTER", "After isIncome filter ($isIncome): ${filteredList.size} transactions (removed ${initialSize - filteredList.size})")
        }
        
        Log.i("TX_REPO_FILTER", "filterTransactions finished. Returning ${filteredList.size} transactions.")
        filteredList
    }
    
    /**
     * Get transaction by ID
     */
    override suspend fun getTransactionById(id: String): TransactionData? = withContext(Dispatchers.Default) {
        // Use the transaction.id (UUID) for lookup, matching the key used for category assignment cache
        cachedTransactions.find { it.id == id }
        // Old implementation using generated key:
        // cachedTransactions.find { generateTransactionKey(it) == id }
    }
    
    /**
     * Refresh SMS data with options to limit by date
     */
    override suspend fun refreshSmsData(limitToRecentMonths: Int) = withContext(Dispatchers.IO) {
        if (!smsDataSource.hasReadSmsPermission()) {
            Log.w("SMS_REFRESH", "SMS permission not granted")
            return@withContext
        }
        
        try {
            Log.d("SMS_REFRESH", "Loading SMS data limited to last $limitToRecentMonths months")
            val messages = smsDataSource.readSmsMessages(
                limitToRecentMonths = limitToRecentMonths,
                maxResults = 500
            )
            
            // Clear cache if we're refreshing
            if (limitToRecentMonths > 0) {
                cachedSmsMessages = emptyList()
                cachedTransactions = emptyList()
            }
            
            cachedSmsMessages = messages
            
            Log.d("SMS_REFRESH", "Processing ${messages.size} SMS messages in chunks")
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
         Log.d("CAT_ASSIGN_REPO_T", "-> applyCategoryAssignments called for ${transactions.size} transactions.") 
        // Load the SharedPreferences cache
        loadTransactionCategoryCache() // Make sure this is loaded before applying
        Log.d("CAT_ASSIGN_REPO_T", "   Loaded transactionCategoryCache (SharedPreferences). Size: ${transactionCategoryCache.size}") 
        var changesApplied = 0
        var checkedCount = 0 
        transactions.forEach { transaction ->
            // Use transaction.id (UUID) as the key, matching how it's saved
            val txId = transaction.id
            if (txId != null) { // Check if transaction has an ID
                 checkedCount++ // Increment checked counter
                 val savedCategoryId = transactionCategoryCache[txId] // Lookup by UUID
                 
                 // ADDED: Log every check, regardless of outcome
                 Log.d("CAT_ASSIGN_REPO_T", "   Loop Check - TxID: $txId, Found in Cache: ${savedCategoryId != null} (SavedValue: $savedCategoryId), Current Tx CatID: ${transaction.categoryId}") 

                 if (savedCategoryId != null) {
                     // Log details when a saved category IS found
                     // Log.d("CAT_ASSIGN_REPO_T", "   Checking TxID: $txId - Current CatID: ${transaction.categoryId} - Saved CatID: $savedCategoryId") // Redundant now
                     if (transaction.categoryId != savedCategoryId) {
                         Log.d("CAT_ASSIGN_REPO_T", "      DIFFERENT! Applying category from SharedPreferences.") 
                         transaction.categoryId = savedCategoryId
                         changesApplied++
                     } else {
                         // Log when they are the same
                         Log.d("CAT_ASSIGN_REPO_T", "      SAME! No change needed.")
                     }
                 } else {
                    // ADDED: Explicit log when ID is checked but not found in cache
                    // Log.d("CAT_ASSIGN_REPO_T", "   Loop Check - TxID: $txId - Not found in SharedPreferences cache (Size: ${transactionCategoryCache.size}).") // Covered by main log above
                 }
            } else { // Log if transaction ID is null
                 Log.w("CAT_ASSIGN_REPO_T", "   Loop Check - Skipping transaction with NULL ID: ${transaction.provider} / ${transaction.amount}")
            }
        }
        Log.d("CAT_ASSIGN_REPO_T", "<- applyCategoryAssignments finished. Checked: $checkedCount, Changes applied: $changesApplied") 
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
        Log.d("CAT_ASSIGN_REPO_T", "-> saveCategoryForTransaction(TxID: $transactionId, CatID: $categoryId) [SharedPreferences]")
        transactionCategoryCache[transactionId] = categoryId
        saveTransactionCategoryCache() // Assuming this writes to SharedPreferences
        Log.d("CAT_ASSIGN_REPO_T", "<- Updated transactionCategoryCache and called saveTransactionCategoryCache()")
    }
    
    /**
     * Assign category to transaction and save the assignment
     */
    override suspend fun assignCategoryToTransaction(
        transactionId: String, 
        categoryId: String
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d("CAT_ASSIGN_REPO_T", "-> assignCategoryToTransaction(TxID: $transactionId, CatID: $categoryId)")
        val transaction = getTransactionById(transactionId) // Find in cache
        Log.d("CAT_ASSIGN_REPO_T", "   Transaction found in cache: ${transaction != null} (Current CatID: ${transaction?.categoryId})")

        if (transaction != null) {
            val originalCatId = transaction.categoryId // Store original for logging
            transaction.categoryId = categoryId // Update object reference in cache
            Log.d("CAT_ASSIGN_REPO_T", "   Updated transaction object in cache (Old CatID: $originalCatId, New CatID: $categoryId)")

            // Log before saving to SharedPreferences
            saveCategoryForTransaction(transactionId, categoryId) // Update SharedPreferences

            // Log before Firestore update
            Log.d("CAT_ASSIGN_REPO_T", "   Calling updateTransactionInFirestore...")
            val updateResult = updateTransactionInFirestore(transaction.copy(categoryId = categoryId)) 

            val success = updateResult.isSuccess
            Log.d("CAT_ASSIGN_REPO_T", "   Firestore update result: ${if(success) "Success" else "Failure"}")
            if (!success) {
                 Log.e("CAT_ASSIGN_REPO_T", "   Firestore update error: ", updateResult.exceptionOrNull()) // Log error details
            }
            Log.d("CAT_ASSIGN_REPO_T", "<- assignCategoryToTransaction returning: $success")
            success // Return success/failure
        } else {
            Log.w("CAT_ASSIGN_REPO_T", "   Transaction with ID $transactionId not found in cache.")
            Log.d("CAT_ASSIGN_REPO_T", "<- assignCategoryToTransaction returning: false (transaction not found)")
            false
        }
    }
    
    /**
     * Generate a unique key for a transaction based on its available data.
     * Note: This key is primarily for local caching/linking. Firestore uses its own document ID.
     */
    private fun generateTransactionKey(transaction: TransactionData): String {
        // Use available fields instead of the removed originalMessage
        val input = "${transaction.date.time}_${transaction.amount}_${transaction.isIncome}_${transaction.provider ?: "N/A"}"
        return UUID.nameUUIDFromBytes(input.toByteArray()).toString()
    }

    // --- Firestore Function Implementations ---

    override suspend fun saveTransactionToFirestore(transaction: TransactionData): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = transaction.userId ?: throw IllegalArgumentException("User ID is required to save transaction")
                // Ensure an ID exists, generate if null (Firestore doesn't need this if using .add())
                val docId = transaction.id ?: UUID.randomUUID().toString()
                val docRef = db.collection("users").document(userId).collection("transactions").document(docId)
                // Use the model potentially updated with the generated ID
                docRef.set(transaction.copy(id = docId, userId = userId)).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FIRESTORE_TX", "Error saving transaction ${transaction.id}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getTransactionsFromFirestore(userId: String): Result<List<TransactionData>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("users").document(userId)
                                .collection("transactions")
                                .get()
                                .await()
                val transactions = snapshot.toObjects<TransactionData>()
                Log.d("FIRESTORE_TX", "Fetched ${transactions.size} transactions for user $userId")
                Result.success(transactions)
            } catch (e: Exception) {
                Log.e("FIRESTORE_TX", "Error fetching transactions for user $userId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateTransactionInFirestore(transaction: TransactionData): Result<Unit> {
       Log.d("CAT_ASSIGN_REPO_T", "-> updateTransactionInFirestore(TxID: ${transaction.id}, CatID: ${transaction.categoryId}) [Firestore]")
       return withContext(Dispatchers.IO) {
            try {
                val userId = transaction.userId ?: throw IllegalArgumentException("User ID is required to update transaction")
                val docId = transaction.id ?: throw IllegalArgumentException("Document ID is required to update transaction")
                
                Log.d("CAT_ASSIGN_REPO_T", "   Executing Firestore set operation...")
                db.collection("users").document(userId)
                  .collection("transactions").document(docId)
                  .set(transaction, SetOptions.merge()) // Merge to avoid overwriting fields unintentionally
                  .await()
                Log.d("CAT_ASSIGN_REPO_T", "<- updateTransactionInFirestore returning: Success")
                Result.success(Unit)
            } catch (e: Exception) {
                 Log.e("CAT_ASSIGN_REPO_T", "<- updateTransactionInFirestore returning: Failure", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteTransactionFromFirestore(transactionId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.collection("users").document(userId)
                  .collection("transactions").document(transactionId)
                  .delete()
                  .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FIRESTORE_TX", "Error deleting transaction $transactionId", e)
                Result.failure(e)
            }
        }
    }

    // --- Sync Function Implementation ---
    override suspend fun performInitialTransactionSync(userId: String, syncStartDate: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                 Log.d("SYNC_TX", "Starting initial transaction sync for user $userId since ${Date(syncStartDate)}")
                 // Note: Using cachedTransactions might not be robust for initial sync.
                // A better approach might involve re-reading SMS or SharedPreferences.
                // For now, we proceed with the current cache.
                if (cachedTransactions.isEmpty()) {
                     Log.w("SYNC_TX", "No cached transactions found to sync.")
                     // Optionally trigger refreshSmsData here if desired?
                    // refreshSmsData(1) // Example: Refresh last month before sync
                    // return Result.failure(IllegalStateException("No local transactions to sync."))
                }
                
                val syncStartTime = Date(syncStartDate)
                val transactionsToSync = cachedTransactions.filter { it.date.after(syncStartTime) }
                
                if (transactionsToSync.isEmpty()) {
                    Log.d("SYNC_TX", "No recent transactions found in cache to sync.")
                    return@withContext Result.success(Unit) // Nothing to sync
                }
                
                Log.d("SYNC_TX", "Found ${transactionsToSync.size} transactions in cache to sync.")
                
                val batch: WriteBatch = db.batch()
                var batchCounter = 0

                transactionsToSync.forEach { transaction ->
                    // Ensure transaction has a valid ID for Firestore and the correct UserId
                    val firestoreId = transaction.id ?: UUID.randomUUID().toString() // Use existing or generate new
                    val transactionWithUser = transaction.copy(id = firestoreId, userId = userId)

                    val docRef = db.collection("users").document(userId)
                                   .collection("transactions").document(firestoreId)
                    batch.set(docRef, transactionWithUser) // Use set to overwrite or create
                    batchCounter++
                    
                    // Firestore batches have a limit (e.g., 500 operations)
                    if (batchCounter % 499 == 0) {
                        batch.commit().await() // Commit the current batch
                        // batch = db.batch() // Start a new batch (reassign is needed)
                         // Re-init batch might be needed depending on SDK version, check docs.
                         Log.d("SYNC_TX", "Committed batch of $batchCounter transactions")
                         // Reset counter if re-initializing batch
                    }
                }
                
                // Commit any remaining operations in the last batch
                if (batchCounter % 499 != 0) { // Check if there are pending ops
                   batch.commit().await()
                   Log.d("SYNC_TX", "Committed final batch of transactions. Total: ${transactionsToSync.size}")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SYNC_TX", "Error during initial transaction sync", e)
                Result.failure(e)
            }
        }
    }
} 