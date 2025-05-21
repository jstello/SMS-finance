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
    override suspend fun deleteCategory(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUserState.firstOrNull()?.uid
        if (userId == null) {
            Log.e("CAT_REPO_DELETE", "User not logged in, cannot delete category $categoryId from Firestore.")
            return@withContext Result.failure(IllegalStateException("User not logged in"))
        }

        // Attempt to delete from Firestore first
        val firestoreDeleteResult = deleteCategoryFromFirestore(categoryId, userId)
        if (firestoreDeleteResult.isFailure) {
            Log.e("CAT_REPO_DELETE", "Failed to delete category $categoryId from Firestore.", firestoreDeleteResult.exceptionOrNull())
            return@withContext firestoreDeleteResult // Propagate Firestore deletion failure
        }
        Log.i("CAT_REPO_DELETE", "Successfully deleted category $categoryId from Firestore for user $userId.")

        // Proceed with SharedPreferences deletion only if Firestore deletion was successful
        val categories = sharedPrefsManager.loadCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) {
            categories.removeAt(index)
            sharedPrefsManager.saveCategories(categories)
            Log.i("CAT_REPO_DELETE", "Successfully deleted category $categoryId from SharedPreferences.")
        } else {
            Log.w("CAT_REPO_DELETE", "Category $categoryId not found in SharedPreferences, might have only been in Firestore.")
        }
        
        // Transaction reassignment logic is handled by a dedicated Use Case.
        Log.w("DELETE_CAT", "Transaction reassignment logic should be handled in a separate UseCase if necessary.")
        
        return@withContext Result.success(Unit)
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

    /**
     * Returns a placeholder Category object to represent uncategorized transactions.
     */
    override fun getUncategorizedCategoryPlaceholder(): Category {
        // Returns a new instance each time to prevent accidental modification of a shared object.
        // The ID is null, signifying it's a placeholder for transactions without a categoryId.
        return Category(
            id = null, // Crucial: ID is null for this placeholder
            name = "Other",
            color = 0xFF808080.toInt(),  // Grey or a neutral color, converted to Int
            userId = null    // Not tied to a specific user for saving, it's a concept
        )
    }

    // Define a constant for the mappings document path
    private companion object {
        const val PROVIDER_MAPPINGS_COLLECTION = "provider_category_settings"
        const val MAPPINGS_DOCUMENT = "mappings"
        const val PROVIDER_TO_CATEGORY_MAP_FIELD = "providerToCategoryMap"
    }

    override suspend fun saveProviderCategoryMapping(userId: String, providerName: String, categoryId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val docRef = db.collection("users").document(userId)
                    .collection(PROVIDER_MAPPINGS_COLLECTION).document(MAPPINGS_DOCUMENT)

                // Firestore uses dot notation for nested fields in maps
                docRef.set(mapOf(PROVIDER_TO_CATEGORY_MAP_FIELD to mapOf(providerName to categoryId)), SetOptions.merge()).await()
                Log.i("PROVIDER_MAP", "Saved mapping for provider '$providerName' to categoryId '$categoryId' for user $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("PROVIDER_MAP", "Error saving provider mapping for '$providerName' for user $userId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoryForProvider(userId: String, providerName: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val docRef = db.collection("users").document(userId)
                    .collection(PROVIDER_MAPPINGS_COLLECTION).document(MAPPINGS_DOCUMENT)

                val document = docRef.get().await()
                if (document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val mappings = document.get(PROVIDER_TO_CATEGORY_MAP_FIELD) as? Map<String, String>
                    val categoryId = mappings?.get(providerName)
                    if (categoryId != null) {
                        Log.i("PROVIDER_MAP", "Found mapping for provider '$providerName' -> categoryId '$categoryId' for user $userId")
                    } else {
                        Log.i("PROVIDER_MAP", "No mapping found for provider '$providerName' for user $userId")
                    }
                    Result.success(categoryId)
                } else {
                    Log.i("PROVIDER_MAP", "No mappings document found for user $userId")
                    Result.success(null) // No mappings document means no mapping for this provider
                }
            } catch (e: Exception) {
                Log.e("PROVIDER_MAP", "Error fetching category for provider '$providerName' for user $userId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteProviderCategoryMapping(userId: String, providerName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val docRef = db.collection("users").document(userId)
                    .collection(PROVIDER_MAPPINGS_COLLECTION).document(MAPPINGS_DOCUMENT)

                // To delete a key from a map, we need to use FieldValue.delete()
                // This requires careful handling if the map itself doesn't exist or the key isn't present.
                // A simpler way for this structure is to fetch, modify, and set, or use a specific field update.
                // For field deletion within a map: update(map_field.key_to_delete, FieldValue.delete())
                docRef.update("$PROVIDER_TO_CATEGORY_MAP_FIELD.$providerName", com.google.firebase.firestore.FieldValue.delete()).await()
                Log.i("PROVIDER_MAP", "Deleted mapping for provider '$providerName' for user $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                // Firestore throws an exception if the path to delete doesn't exist, which is fine (idempotent delete).
                // However, log it for awareness if it's not due to non-existence.
                Log.w("PROVIDER_MAP", "Error deleting provider mapping for '$providerName' for user $userId (might be non-existent): ${e.message}")
                // We can consider this a success if the goal is to ensure the mapping is gone.
                // However, to be precise about the operation outcome:
                if (e.message?.contains("No document to update") == true || e.message?.contains("NOT_FOUND") == true) {
                     Log.i("PROVIDER_MAP", "Mapping for provider '$providerName' was already non-existent for user $userId.")
                     Result.success(Unit) // Effectively, the state is as desired.
                } else {
                     Result.failure(e)
                }
            }
        }
    }

    override suspend fun getAllProviderCategoryMappings(userId: String): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val docRef = db.collection("users").document(userId)
                    .collection(PROVIDER_MAPPINGS_COLLECTION).document(MAPPINGS_DOCUMENT)

                val document = docRef.get().await()
                if (document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val mappings = document.get(PROVIDER_TO_CATEGORY_MAP_FIELD) as? Map<String, String>
                    if (mappings != null) {
                        Log.i("PROVIDER_MAP", "Fetched ${mappings.size} provider mappings for user $userId")
                        Result.success(mappings)
                    } else {
                        Log.i("PROVIDER_MAP", "Mappings field '$PROVIDER_TO_CATEGORY_MAP_FIELD' is null or not a map for user $userId")
                        Result.success(emptyMap()) // Field is missing or wrong type
                    }
                } else {
                    Log.i("PROVIDER_MAP", "No mappings document found for user $userId. Returning empty map.")
                    Result.success(emptyMap()) // No mappings document
                }
            } catch (e: Exception) {
                Log.e("PROVIDER_MAP", "Error fetching all provider mappings for user $userId", e)
                Result.failure(e)
            }
        }
    }
} 