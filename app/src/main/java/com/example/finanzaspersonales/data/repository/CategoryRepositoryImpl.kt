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
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Implementation of the CategoryRepository
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPrefsManager: SharedPrefsManager,
    private val authRepository: AuthRepository
) : CategoryRepository {
    
    private val db: FirebaseFirestore = Firebase.firestore
    
    /**
     * Get all categories, prioritizing Firestore and falling back to SharedPreferences defaults.
     */
    override suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        Log.d("CAT_REPO_AUTH", "Checking user login status for fetching categories...")
        val userId = authRepository.currentUserState.firstOrNull()?.uid
        Log.d("CAT_REPO", "getCategories() called. userId=$userId")
        
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
        
        // Fallback: Load from SharedPreferences
        Log.d("CAT_REPO", "Loading categories from SharedPreferences as fallback.")
        val localCategories = sharedPrefsManager.loadCategories()

        // Final Fallback: Return default categories if both Firestore and SharedPreferences are empty
        if (localCategories.isEmpty()) {
            Log.w("CAT_REPO", "No categories found in SharedPreferences. Returning default categories.")
            return@withContext SharedPrefsManager.DEFAULT_CATEGORIES
        }

        Log.d("CAT_REPO", "Returning ${localCategories.size} categories from SharedPreferences.")
        return@withContext localCategories
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
        // TODO: Revisit transaction reassignment logic when fetching from Firestore - MOVED TO USE CASE
        val categories = sharedPrefsManager.loadCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) {
            categories.removeAt(index)
            sharedPrefsManager.saveCategories(categories)
            
            // Reassignment logic removed - should be handled by a dedicated Use Case
            // that can access both CategoryRepository and TransactionRepository.
            Log.w("DELETE_CAT", "Transaction reassignment logic needs to be implemented in a separate UseCase.")
            
            // Example: deleteCategoryFromFirestore(categoryId, getCurrentUserId())
        }
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