package com.example.finanzaspersonales.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toMonth
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toYear
import com.example.finanzaspersonales.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Implementation of the CategoryRepository
 */
class CategoryRepositoryImpl(
    private val context: Context,
    private val sharedPrefsManager: SharedPrefsManager,
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) : CategoryRepository {
    
    private val db: FirebaseFirestore = Firebase.firestore
    
    /**
     * Get all categories, prioritizing Firestore and falling back to SharedPreferences defaults.
     */
    override suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        Log.d("CAT_REPO_AUTH", "Checking user login status for fetching categories...")
        val userId = authRepository.currentUserState.firstOrNull()?.uid
        Log.d("CAT_REPO_AUTH", "User ID obtained from AuthRepository: '$userId'")
        
        if (userId != null) {
            Log.i("CAT_REPO_AUTH", "User is logged in (ID: $userId). Attempting Firestore fetch.")
            Log.d("CAT_REPO", "Attempting to fetch categories from Firestore for user $userId")
            val firestoreResult = getCategoriesFromFirestore(userId)
            firestoreResult.fold(
                onSuccess = { categories ->
                    if (categories.isNotEmpty()) {
                        Log.i("CAT_REPO", "Successfully fetched ${categories.size} categories from Firestore.")
                        // Optionally update SharedPreferences cache here if needed
                        // sharedPrefsManager.saveCategories(categories)
                        return@withContext categories
                    } else {
                        Log.w("CAT_REPO", "Firestore fetch successful but returned empty list. Falling back to SharedPreferences.")
                    }
                },
                onFailure = { exception ->
                    Log.e("CAT_REPO", "Error fetching categories from Firestore. Falling back to SharedPreferences.", exception)
                }
            )
        } else {
            Log.w("CAT_REPO", "User not logged in. Falling back to SharedPreferences defaults.")
        }
        
        // Fallback: Load from SharedPreferences (which includes defaults if empty)
        Log.d("CAT_REPO", "Loading categories from SharedPreferences as fallback.")
        sharedPrefsManager.loadCategories()
    }
    
    /**
     * Add a new category
     */
    override suspend fun addCategory(category: Category) = withContext(Dispatchers.IO) {
        val categories = sharedPrefsManager.loadCategories().toMutableList()
        categories.add(category)
        sharedPrefsManager.saveCategories(categories)
    }
    
    /**
     * Update a category
     */
    override suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        val categories = sharedPrefsManager.loadCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            categories[index] = category
            sharedPrefsManager.saveCategories(categories)
        }
    }
    
    /**
     * Delete a category (Needs update for Firestore & reassignment logic)
     */
    override suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        // TODO: Call deleteCategoryFromFirestore here
        // TODO: Revisit transaction reassignment logic when fetching from Firestore
        val categories = sharedPrefsManager.loadCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) {
            categories.removeAt(index)
            sharedPrefsManager.saveCategories(categories)
            
            // Find "Other" category to reassign any transaction with this category
            val otherCategory = categories.find { it.name == "Other" }
            if (otherCategory?.id != null) { // Ensure Other category and its ID are not null
                // Get transactions with this category and reassign them
                // This part needs significant rework for Firestore
                 Log.w("DELETE_CAT", "Transaction reassignment logic needs update for Firestore")
                // val transactions = transactionRepository.getTransactionsByCategory(categoryId) // This needs to check Firestore
                // for (transaction in transactions) {
                //     // Safely handle nullable IDs before calling
                //     val transactionIdToReassign = transaction.id
                //     val otherCategoryId = otherCategory.id // Already checked non-null above
                //     if (transactionIdToReassign != null) { 
                //         transactionRepository.assignCategoryToTransaction(transactionIdToReassign, otherCategoryId)
                //     }
                // }
            }
            // Example: deleteCategoryFromFirestore(categoryId, getCurrentUserId())
        }
    }
    
    /**
     * Set a category for a transaction
     */
    override suspend fun setCategoryForTransaction(transactionId: String, categoryId: String): Boolean {
        Log.d("CAT_ASSIGN_REPO_C", "-> setCategoryForTransaction(TxID: $transactionId, CatID: $categoryId)")
        val result = withContext(Dispatchers.IO) {
            Log.d("CAT_ASSIGN_REPO_C", "   Calling TransactionRepository.assignCategoryToTransaction...")
            transactionRepository.assignCategoryToTransaction(transactionId, categoryId)
        }
        Log.d("CAT_ASSIGN_REPO_C", "<- setCategoryForTransaction returning: $result")
        return result
    }
    
    /**
     * Get transactions by category
     * Handles 'Other' category specially to include uncategorized items.
     */
    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> = withContext(Dispatchers.Default) {
        Log.d("CAT_REPO", "-> (CategoryRepo) getTransactionsByCategory called for categoryId: $categoryId")
        // Fetch ALL transactions first
        Log.d("CAT_REPO", "   Calling transactionRepository.getTransactions()")
        val allTransactions = transactionRepository.getTransactions()
        Log.d("CAT_REPO", "   Total transactions fetched: ${allTransactions.size}")
        
        // Find the predefined ID for the "Other" category
        val otherCategoryId = SharedPrefsManager.DEFAULT_CATEGORIES.find { it.name == "Other" }?.id
        Log.d("CAT_REPO", "   Requested CatID: $categoryId, Predefined Other CatID: $otherCategoryId")

        // Filter logic based on whether the requested ID is the special "Other" ID
        val categoryTransactions = if (categoryId == otherCategoryId && otherCategoryId != null) {
            // If requesting the specific "Other" category ID
            Log.d("CAT_REPO", "   Filtering for 'Other' category (ID: $categoryId) including null/empty")
            allTransactions.filter { 
                val id = it.categoryId // Capture local immutable variable
                id == categoryId || id == null || id.isEmpty() // Use local variable for checks
            }
        } else {
            // For any other specific category ID
            Log.d("CAT_REPO", "   Filtering for specific category ID: $categoryId")
            allTransactions.filter { it.categoryId == categoryId }
        }

        Log.i("CAT_REPO", "<- (CategoryRepo) Found ${categoryTransactions.size} transactions for requested categoryId: $categoryId. Returning list.")
        categoryTransactions
    }
    
    /**
     * Get spending by category
     */
    override suspend fun getSpendingByCategory(year: Int?, month: Int?): Map<Category, Float> = withContext(Dispatchers.Default) {
        Log.d("CATEGORY_SPENDING", "====== Getting Spending By Category ======")
        Log.d("CATEGORY_SPENDING", "Filter - Year: $year, Month: $month")
        
        val categories = getCategories()
        Log.d("CATEGORY_SPENDING", "Total categories: ${categories.size}")
        
        val transactions = transactionRepository.getTransactions()
        Log.d("CATEGORY_SPENDING", "Total transactions: ${transactions.size}")
        
        // IMPORTANT: Filter transactions by year/month first
        val filteredTransactions = transactionRepository.filterTransactions(
            transactions = transactions,
            year = year,
            month = month,
            isIncome = false // Only expenses
        )
        Log.d("CATEGORY_SPENDING", "Filtered transactions: ${filteredTransactions.size}")
        Log.d("CATEGORY_SPENDING", "Filtered uncategorized transactions: ${filteredTransactions.count { it.categoryId == null }}")
        
        // --- Add Detailed Logging --- 
        Log.d("CATEGORY_SPENDING", "-- Details of filteredTransactions before summation --")
        filteredTransactions.take(10).forEachIndexed { index, tx -> // Log first 10
             Log.d("CATEGORY_SPENDING", "[$index]: Date=${tx.date}, Amt=${tx.amount}, Provider=${tx.provider}, CatId=${tx.categoryId ?: "NULL"}")
        }
        // ---------------------------

        val result = mutableMapOf<Category, Float>()
        
        // Initialize all categories with 0.0f
        categories.forEach { category ->
            result[category] = 0.0f
        }
        
        // Get "Other" category
        val otherCategory = categories.find { it.name == "Other" } ?: categories.last()
        Log.d("CATEGORY_SPENDING", "Other category: ${otherCategory.name} (${otherCategory.id})")
        
        // Log transactions by category
        Log.d("CATEGORY_SPENDING", "----- Transaction Distribution -----")
        categories.forEach { category ->
            val count = filteredTransactions.count { it.categoryId == category.id }
            Log.d("CATEGORY_SPENDING", "Category '${category.name}': $count transactions")
        }
        Log.d("CATEGORY_SPENDING", "Uncategorized: ${filteredTransactions.count { it.categoryId == null }} transactions")
        
        // Add up spending for each transaction
        filteredTransactions.forEach { transaction ->
            val categoryId = transaction.categoryId
            
            if (categoryId != null) {
                // If we have a category for this transaction
                val category = categories.find { it.id == categoryId }
                if (category != null) {
                    val oldAmount = result[category] ?: 0.0f
                    val newAmount = oldAmount + transaction.amount
                    result[category] = newAmount
                    Log.d("CATEGORY_SPENDING", "Added ${transaction.amount} to '${category.name}', total: $newAmount")
                } else {
                    // Category no longer exists, add to Other
                    val oldAmount = result[otherCategory] ?: 0.0f
                    val newAmount = oldAmount + transaction.amount
                    result[otherCategory] = newAmount
                    Log.d("CATEGORY_SPENDING", "Added ${transaction.amount} to '${otherCategory.name}' (missing category), total: $newAmount")
                }
            } else {
                // No category assigned, add to Other
                val oldAmount = result[otherCategory] ?: 0.0f
                val newAmount = oldAmount + transaction.amount
                result[otherCategory] = newAmount
                Log.d("CATEGORY_SPENDING", "Added ${transaction.amount} to '${otherCategory.name}' (uncategorized), total: $newAmount")
            }
        }
        
        // Log final category totals
        Log.d("CATEGORY_SPENDING", "----- Final Category Totals -----")
        result.forEach { (category, amount) ->
            Log.d("CATEGORY_SPENDING", "Category '${category.name}': $amount")
        }
        
        // Return only categories with spending > 0
        val nonZeroResults = result.filter { it.value > 0 }
        Log.d("CATEGORY_SPENDING", "Categories with spending > 0: ${nonZeroResults.size}")
        
        nonZeroResults
    }
    
    /**
     * Get the category ID for a specific transaction
     */
    override suspend fun getCategoryIdForTransaction(transactionId: String): String? = withContext(Dispatchers.IO) {
        val transaction = transactionRepository.getTransactionById(transactionId)
        transaction?.categoryId
    }
    
    /**
     * Generate a unique key for a transaction
     */
    private fun generateTransactionKey(transaction: TransactionData): String {
        val input = "${transaction.date.time}_${transaction.amount}_${transaction.isIncome}_${transaction.provider ?: "N/A"}"
        return java.util.UUID.nameUUIDFromBytes(input.toByteArray()).toString()
    }

    override suspend fun saveCategoryToFirestore(category: Category): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = category.userId ?: throw IllegalArgumentException("User ID is required to save category")
                val docId = category.id ?: UUID.randomUUID().toString()
                val docRef = db.collection("users").document(userId).collection("categories").document(docId)
                docRef.set(category.copy(id = docId, userId = userId)).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FIRESTORE_CAT", "Error saving category ${category.id}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoriesFromFirestore(userId: String): Result<List<Category>> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("users").document(userId)
                                .collection("categories")
                                .get()
                                .await()
                val categories = snapshot.toObjects<Category>()
                 Log.d("FIRESTORE_CAT", "Fetched ${categories.size} categories for user $userId")
                Result.success(categories)
            } catch (e: Exception) {
                Log.e("FIRESTORE_CAT", "Error fetching categories for user $userId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateCategoryInFirestore(category: Category): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = category.userId ?: throw IllegalArgumentException("User ID is required to update category")
                val docId = category.id ?: throw IllegalArgumentException("Document ID is required to update category")
                
                db.collection("users").document(userId)
                  .collection("categories").document(docId)
                  .set(category, SetOptions.merge()).await()
                Result.success(Unit)
            } catch (e: Exception) {
                 Log.e("FIRESTORE_CAT", "Error updating category ${category.id}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteCategoryFromFirestore(categoryId: String, userId: String): Result<Unit> {
         Log.w("FIRESTORE_CAT", "Deletion needs logic to reassign transactions first!")
        return withContext(Dispatchers.IO) {
            try {
                db.collection("users").document(userId)
                   .collection("categories").document(categoryId)
                   .delete()
                   .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FIRESTORE_CAT", "Error deleting category $categoryId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun performInitialCategorySync(userId: String): Result<Unit> {
         return withContext(Dispatchers.IO) {
            try {
                Log.d("SYNC_CAT", "Starting initial category sync for user $userId")
                val localCategories = sharedPrefsManager.loadCategories()

                if (localCategories.isEmpty()) {
                    Log.d("SYNC_CAT", "No local categories found to sync.")
                    return@withContext Result.success(Unit)
                }
                
                Log.d("SYNC_CAT", "Found ${localCategories.size} local categories to sync.")

                val batch: WriteBatch = db.batch()
                localCategories.forEach { category ->
                     val firestoreId = category.id ?: UUID.randomUUID().toString()
                    val categoryWithUser = category.copy(id = firestoreId, userId = userId)
                    
                    val docRef = db.collection("users").document(userId)
                                   .collection("categories").document(firestoreId)
                    batch.set(docRef, categoryWithUser)
                }
                
                batch.commit().await()
                Log.d("SYNC_CAT", "Committed batch of ${localCategories.size} categories.")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SYNC_CAT", "Error during initial category sync", e)
                Result.failure(e)
            }
        }
    }
} 