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
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.finanzaspersonales.data.auth.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Implementation of the TransactionRepository
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsDataSource: SmsDataSource,
    private val extractTransactionDataUseCase: ExtractTransactionDataUseCase,
    private val categoryAssignmentUseCase: CategoryAssignmentUseCase,
    private val sharedPrefsManager: SharedPrefsManager,
    private val authRepository: AuthRepository
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
            // Correctly load from sharedPrefsManager
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
            // Uncomment the save operation
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
        // Debug authentication state before Firestore fetch
        Log.d("FIRESTORE_TX", "AuthRepository.currentUser (sync) = ${authRepository.currentUser?.uid}")
        val debugUserId = authRepository.currentUserState.firstOrNull()?.uid
        Log.d("FIRESTORE_TX", "AuthRepository.currentUserState.firstOrNull() = $debugUserId")
        Log.d("FIRESTORE_TX", "Checking cache before Firestore fetch. cachedTransactions.isEmpty() = ${cachedTransactions.isEmpty()}, size = ${cachedTransactions.size}")
        // Attempt to load saved transactions from Firestore on first run
        if (cachedTransactions.isEmpty()) {
            val userId = debugUserId
            if (userId != null) {
                Log.d("FIRESTORE_TX", "Attempting to fetch transactions from Firestore for user $userId")
                val remoteResult = getTransactionsFromFirestore(userId)
                if (remoteResult.isSuccess) {
                    val remoteList = remoteResult.getOrNull().orEmpty()
                    Log.d("FIRESTORE_TX", "Fetched ${remoteList.size} remote transactions")
                    // Debug: verify provider for the recently updated transaction
                    remoteList.firstOrNull { it.id == "61c254cb-0930-3b5a-aa52-f267a559b122" }?.let { tx ->
                        Log.d("FIRESTORE_TX", "Debug fetch: TxID=${tx.id}, provider='${tx.provider}'")
                    } ?: Log.w("FIRESTORE_TX", "Debug fetch: TxID=61c254cb-0930-3b5a-aa52-f267a559b122 not found in remoteList")
                    cachedTransactions = remoteList
                } else {
                    Log.e("FIRESTORE_TX", "Failed to fetch transactions from Firestore", remoteResult.exceptionOrNull())
                }
            } else {
                Log.d("FIRESTORE_TX", "No authenticated user, skipping Firestore fetch")
            }
        }
        
        if (cachedTransactions.isEmpty()) {
            Log.d("CAT_ASSIGN_REPO_T", "   Cache empty, calling refreshSmsData...")
            refreshSmsData(0) // This populates cachedTransactions
            // **Crucial Fix:** Explicitly apply assignments AFTER refreshing data for the first time
            Log.d("CAT_ASSIGN_REPO_T", "   Applying category assignments after initial refresh...")
            cachedTransactions = applyCategoryAssignments(cachedTransactions)
        } else {
            Log.d("CAT_ASSIGN_REPO_T", "   Cache not empty, applying category assignments...")
            // Apply SharedPreferences categories and update the cache with the result
            cachedTransactions = applyCategoryAssignments(cachedTransactions)
        }
        
        // Ensure provider field is populated from contactName if null
        val updatedTransactions = cachedTransactions.map { transaction ->
            // Debug: Check provider state for the specific transaction *before* the map logic
            if (transaction.id == "61c254cb-0930-3b5a-aa52-f267a559b122") {
                Log.d("FIRESTORE_TX", "Provider Check (Before Map): TxID=${transaction.id}, provider=${transaction.provider}, contactName=${transaction.contactName}")
            }
            if (transaction.provider == null && transaction.contactName != null) {
                Log.d("TX_REPO_GET_TX", "Updating provider from contactName for TxID: ${transaction.id} - Provider: ${transaction.contactName}")
                transaction.copy(provider = transaction.contactName)
            } else {
                transaction
            }.also {
                // Debug: Check provider state *after* the map logic
                if (it.id == "61c254cb-0930-3b5a-aa52-f267a559b122") {
                    Log.d("FIRESTORE_TX", "Provider Check (After Map): TxID=${it.id}, provider=${it.provider}")
                }
            }
        }

        // Update the cache with the potentially modified list
        cachedTransactions = updatedTransactions

        // Final check before returning from repository
        cachedTransactions.firstOrNull { it.id == "61c254cb-0930-3b5a-aa52-f267a559b122" }?.let {
            Log.d("FIRESTORE_TX", "Final Repo Return Check: TxID=${it.id}, provider=${it.provider}")
        }

        Log.d("CAT_ASSIGN_REPO_T", "<- getTransactions() returning ${cachedTransactions.size} items.")
        cachedTransactions // Return the potentially updated cached list
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
            
            // On a full refresh (limit 0), clear both SMS and transaction caches
            if (limitToRecentMonths == 0) {
                Log.d("SMS_REFRESH", "Full refresh: clearing SMS and transaction caches")
                cachedSmsMessages = emptyList()
                cachedTransactions = emptyList()
            } else {
                Log.d("SMS_REFRESH", "Partial refresh: preserving existing transaction cache and only merging new SMS")
            }
            
            // Always update the SMS message cache
            cachedSmsMessages = messages
            
            Log.d("SMS_REFRESH", "Processing ${messages.size} SMS messages in chunks")
            messages.chunked(50).forEach { chunk ->
                processChunk(chunk)
            }
            
            // Apply category assignments to all transactions after processing
            cachedTransactions = applyCategoryAssignments(cachedTransactions)
            
        } catch (e: Exception) {
            Log.e("SMS_REFRESH", "Error processing SMS", e)
        }
    }
    
    private suspend fun processChunk(chunk: List<SmsMessage>) {
        withContext(Dispatchers.Default) {
            Log.d("SMS_CHUNK_IN", "Processing chunk of ${chunk.size} messages.")
            val processedTransactions = chunk.mapNotNull { sms ->
                Log.v("SMS_PROCESS_ITEM", "Attempting to process SMS body: ${sms.body?.take(100)}...") // Log start of processing
                try {
                    // Extract transaction data from SMS (might have a random ID)
                    val extractedTransaction = extractTransactionDataUseCase.execute(listOf(sms)).firstOrNull()
                    if (extractedTransaction != null) {
                        // Generate a stable ID based on content
                        val stableId = generateTransactionKey(extractedTransaction)
                        // Log the generated ID
                        Log.d("STABLE_ID_GEN", "Generated stable ID: $stableId for Tx Date: ${extractedTransaction.date}, Amt: ${extractedTransaction.amount}")
                        Log.v("SMS_PROCESS_ITEM", "Successfully processed SMS. Tx ID: $stableId")
                        // Return a copy with the stable ID
                        extractedTransaction.copy(id = stableId)
                    } else {
                        Log.w("SMS_PROCESS_ITEM", "Extraction returned null for SMS body: ${sms.body?.take(100)}...")
                        null // Skip if extraction failed
                    }
                } catch (e: Exception) {
                    Log.e("SMS_PROCESSING", "Error processing SMS: ${sms.body}", e)
                    Log.e("SMS_PROCESS_ITEM", "EXCEPTION during processing SMS body: ${sms.body?.take(100)}...")
                    null
                }
            }
            Log.d("SMS_CHUNK_OUT", "Chunk resulted in ${processedTransactions.size} processed transactions.")

            // Update the main cache on the main thread
            withContext(Dispatchers.Main) {
                // Deduplicate the processed transactions from this chunk based on ID
                val uniqueChunkTransactions = processedTransactions.distinctBy { it.id }
                if (uniqueChunkTransactions.size < processedTransactions.size) {
                    Log.w("SMS_DUPLICATE_CHUNK", "Removed ${processedTransactions.size - uniqueChunkTransactions.size} duplicates within the same processed SMS chunk.")
                }

                // Now merge these unique chunk transactions with the main cache
                val existingTransactionsById = cachedTransactions.associateBy { it.id }
                val transactionsToMerge = mutableListOf<TransactionData>()

                uniqueChunkTransactions.forEach { smsTx ->
                    val existingTx = existingTransactionsById[smsTx.id]

                    if (existingTx == null) {
                        // New unique transaction from SMS chunk
                        transactionsToMerge.add(smsTx)
                    } else {
                        // Transaction already exists in cache. Decide which version to keep.
                        // Keep existing if it has a non-null provider, otherwise take the SMS version.
                        val versionToKeep = if (existingTx.provider != null) existingTx else smsTx
                        transactionsToMerge.add(versionToKeep)
                        if (versionToKeep.id == "61c254cb-0930-3b5a-aa52-f267a559b122") { // Debug log
                             Log.d("SMS_MERGE_DECISION_V2", "TxID=${versionToKeep.id}, Kept Version Provider: ${versionToKeep.provider}, Source: ${if (existingTx.provider != null) "ExistingCache" else "SMSChunk"}")
                        }
                    }
                }

                // Update the cache: Filter out existing ones that were replaced, then add the merged ones.
                val finalCachedList = cachedTransactions.filterNot { existingTx ->
                    transactionsToMerge.any { mergedTx -> mergedTx.id == existingTx.id }
                } + transactionsToMerge

                cachedTransactions = finalCachedList.distinctBy { it.id } // Final safety distinct check

                Log.d("SMS_PROCESSING", "Processed chunk: Merged ${uniqueChunkTransactions.size} unique SMS tx. Final cache size: ${cachedTransactions.size}")
            }
        }
    }
    
    /**
     * Apply category assignments (from SharedPreferences cache) to transactions
     * and return the modified list.
     */
    private suspend fun applyCategoryAssignments(transactions: List<TransactionData>): List<TransactionData> { // Return type changed
         Log.d("CAT_ASSIGN_REPO_T", "-> applyCategoryAssignments called for ${transactions.size} transactions.") 
        // Load the SharedPreferences cache
        loadTransactionCategoryCache() // Make sure this is loaded before applying
        Log.d("CAT_ASSIGN_REPO_T", "   Loaded transactionCategoryCache (SharedPreferences). Size: ${transactionCategoryCache.size}")
        // Log first 5 entries of the cache for inspection
        transactionCategoryCache.entries.take(5).forEachIndexed { index, entry ->
            Log.d("CAT_ASSIGN_CACHE_INSPECT", "   Cache[$index]: Key=${entry.key}, Value=${entry.value}")
        }
        var changesMade = false // Flag to track if any changes were made
        
        // Use map to create a new list with potentially updated items
        val updatedList = transactions.map { transaction ->
            val txId = transaction.id // This is the NEW stable ID (Date_Amount_IsIncome)
            var logPrefix = "   ApplyCheck [TxID: $txId]"
            if (txId != null) {
                 val savedCategoryId = transactionCategoryCache[txId] // Lookup using NEW stable ID
                 val lookupResult = if (savedCategoryId != null) "FOUND (CatID: $savedCategoryId)" else "NOT FOUND"
                 Log.d("CAT_ASSIGN_REPO_T", "$logPrefix - Lookup in cache: $lookupResult | Current Tx CatID: ${transaction.categoryId}")

                 if (savedCategoryId != null && transaction.categoryId != savedCategoryId) {
                     // If a saved category exists and it's different from the current one
                     Log.i("CAT_ASSIGN_REPO_T", "$logPrefix - Applying saved category: Changing from '${transaction.categoryId}' to '$savedCategoryId'")
                     changesMade = true
                     transaction.copy(categoryId = savedCategoryId) // Return a copied object with the new categoryId
                 } else {
                     // Log why no change is made
                     if (savedCategoryId == null) {
                         // Log.d("CAT_ASSIGN_REPO_T", "$logPrefix - No change: Category not found in cache.")
                     } else { // savedCategoryId == transaction.categoryId
                         // Log.d("CAT_ASSIGN_REPO_T", "$logPrefix - No change: Transaction already has the correct category.")
                     }
                     transaction // No change needed, return original object
                 }
            } else {
                 Log.w("CAT_ASSIGN_REPO_T", "$logPrefix - Skipping application: Transaction ID is null.")
                 transaction // No ID, return original object
            }
        }
        Log.i("CAT_ASSIGN_REPO_T", "<- applyCategoryAssignments finished. Changes made: $changesMade. Returning list of size ${updatedList.size}.")
        return updatedList // Return the new list (may or may not be different from the original)
    }
    
    /**
     * Get transactions by category ID (Simplified - expects caller to handle special logic)
     */
    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> = 
        withContext(Dispatchers.Default) {
            Log.d("TX_REPO_GET_BY_CAT", "(TransactionRepo) getTransactionsByCategory called for ID: $categoryId - Performing simple filter.")
            val transactions = getTransactions()
            val result = transactions.filter { it.categoryId == categoryId }
            Log.d("TX_REPO_GET_BY_CAT", "(TransactionRepo) Returning ${result.size} transactions strictly matching ID: $categoryId")
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
        
        // 1. Get the current User ID
        val userId = authRepository.currentUserState.firstOrNull()?.uid
        if (userId == null) {
            Log.e("CAT_ASSIGN_REPO_T", "   Cannot assign category: User not logged in.")
            return@withContext false // Cannot update Firestore without user ID
        }
        Log.d("CAT_ASSIGN_REPO_T", "   Current User ID: $userId")
        
        // 2. Find transaction in local cache
        val transaction = getTransactionById(transactionId) // Find in cache
        Log.d("CAT_ASSIGN_REPO_T", "   Transaction found in cache: ${transaction != null} (Current CatID: ${transaction?.categoryId})")

        if (transaction != null) {
            // 3. Create the updated object WITH UserId for Firestore
            val updatedTransactionForFirestore = transaction.copy(categoryId = categoryId, userId = userId)
            Log.d("CAT_ASSIGN_REPO_T", "   Prepared transaction for Firestore with CatID: ${updatedTransactionForFirestore.categoryId}, UserID: ${updatedTransactionForFirestore.userId}")
            
            // 4. Update local cache reference's categoryId
            // Note: Directly modifying cached objects can be risky if immutability is expected.
            transaction.categoryId = categoryId
            // Remove direct modification of userId on cached object
            // transaction.userId = userId 
            Log.d("CAT_ASSIGN_REPO_T", "   Updated local cached transaction object categoryId reference.")

            // 5. Save mapping to SharedPreferences
            saveCategoryForTransaction(transactionId, categoryId)

            // 6. Update Firestore using the copy that includes the userId
            Log.d("CAT_ASSIGN_REPO_T", "   Calling updateTransactionInFirestore...")
            val updateResult = updateTransactionInFirestore(updatedTransactionForFirestore) 

            val success = updateResult.isSuccess
            Log.d("CAT_ASSIGN_REPO_T", "   Firestore update result: ${if(success) "Success" else "Failure"}")
            if (!success) {
                 Log.e("CAT_ASSIGN_REPO_T", "   Firestore update error: ", updateResult.exceptionOrNull()) // Log error details
            }
            Log.d("CAT_ASSIGN_REPO_T", "<- assignCategoryToTransaction returning: $success")
            success // Return success/failure based on Firestore update
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
        // Use only immutable fields (date, amount, isIncome) to ensure ID stability
        val input = "${transaction.date.time}_${transaction.amount}_${transaction.isIncome}"
        return UUID.nameUUIDFromBytes(input.toByteArray()).toString()
    }

    // --- Firestore Function Implementations ---

    override suspend fun saveTransactionToFirestore(transaction: TransactionData): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Use passed userId or fall back to current authenticated user
                val userId = transaction.userId
                    ?: authRepository.currentUserState.firstOrNull()?.uid
                    ?: throw IllegalArgumentException("User ID is required to save transaction")
                // Ensure an ID exists, generate if null (Firestore doesn't need this if using .add())
                val docId = transaction.id ?: UUID.randomUUID().toString()
                val docRef = db.collection("users").document(userId).collection("transactions").document(docId)
                // Prepare the transaction object with the correct ID and userId
                val savedTransaction = transaction.copy(id = docId, userId = userId)
                // Persist to Firestore
                docRef.set(savedTransaction).await()
                // Log success of saving transaction with provider name
                Log.d("FIRESTORE_TX", "Saved transaction provider=${savedTransaction.provider} for TxID=${savedTransaction.id}")
                // Update in-memory cache so future reads reflect the updated provider
                cachedTransactions = cachedTransactions.map { existing ->
                    if (existing.id == savedTransaction.id) savedTransaction else existing
                }
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
                // Instrumentation: inspect raw snapshot documents
                val docs = snapshot.documents
                Log.d("FIRESTORE_TX", "Snapshot docs count: ${docs.size}")
                Log.d("FIRESTORE_TX", "Snapshot doc IDs (first 5): ${docs.take(5).map { it.id }}")
                docs.firstOrNull { it.id == "61c254cb-0930-3b5a-aa52-f267a559b122" }?.let { doc ->
                    Log.d("FIRESTORE_TX", "Firestore raw data for TXID=61c254cb-0930-3b5a-aa52-f267a559b122: ${doc.data}")
                } ?: Log.d("FIRESTORE_TX", "No Firestore doc for TXID=61c254cb-0930-3b5a-aa52-f267a559b122 in snapshot")
                // Direct fetch of the transaction document to verify stored fields
                val txId = "61c254cb-0930-3b5a-aa52-f267a559b122"
                val txDocRef = db.collection("users").document(userId)
                    .collection("transactions").document(txId)
                val txSnap = txDocRef.get().await()
                Log.d("FIRESTORE_TX", "Direct get() for TXID=$txId => data=${txSnap.data}")
                val transactions = snapshot.toObjects<TransactionData>()
                // Debug: check provider for the recently updated TxID
                transactions.firstOrNull { it.id == "61c254cb-0930-3b5a-aa52-f267a559b122" }?.let { tx ->
                    Log.d("FIRESTORE_TX", "getTransactionsFromFirestore Debug: TxID=${tx.id}, provider=${tx.provider}")
                } ?: Log.d("FIRESTORE_TX", "getTransactionsFromFirestore Debug: TxID 61c254cb-0930-3b5a-aa52-f267a559b122 not found in Firestore snapshot")
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

    /**
     * Aggregates transaction amounts by provider within a given date range.
     */
    override suspend fun getProviderStats(from: Long, to: Long): List<ProviderStat> = withContext(Dispatchers.IO) {
        Log.d("PROVIDER_STATS", "-> getProviderStats called. From: $from, To: $to")
        val transactions = getTransactions() // Ensure transactions are loaded and categories applied
        Log.d("PROVIDER_STATS", "   Total transactions fetched: ${transactions.size}")

        val filteredTransactions = transactions.filter {
            it.date.time in from..to && !it.isIncome // Filter by date range and only expenses
        }
        Log.d("PROVIDER_STATS", "   Filtered transactions (date & expenses): ${filteredTransactions.size}")

        if (filteredTransactions.isEmpty()) {
            Log.d("PROVIDER_STATS", "   No relevant transactions found in range.")
            return@withContext emptyList()
        }

        // Group by contactName if available, else by provider, handle nulls
        val stats = filteredTransactions
            .groupBy { transaction ->
                transaction.contactName ?: transaction.provider ?: "Unknown"
            }
            .map { (provider, list) ->
                // Sum amounts (assuming amount is always positive for expenses)
                val totalAmount = list.sumOf { it.amount.toDouble() }.toFloat()
                ProviderStat(provider = provider, total = totalAmount)
            }
            .sortedByDescending { it.total } // Sort by total spending, highest first
        
        Log.d("PROVIDER_STATS", "<- getProviderStats returning ${stats.size} stats.")
        stats
    }
} 