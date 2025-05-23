package com.example.finanzaspersonales.data.repository

import com.example.finanzaspersonales.data.db.mapper.toDomain
import com.example.finanzaspersonales.data.db.mapper.toEntity
import com.example.finanzaspersonales.data.local.room.dao.CategoryDao
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Implementation of the CategoryRepository
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val sharedPrefsManager: SharedPrefsManager
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> =
        categoryDao.getAllCategories().first().map { it.toDomain() }

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = try {
        val entity = categoryDao.getAllCategories().first().first { it.id == categoryId }
        categoryDao.deleteCategory(entity)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveCategoryToFirestore(category: Category): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun getCategoriesFromFirestore(userId: String): Result<List<Category>> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun updateCategoryInFirestore(category: Category): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun deleteCategoryFromFirestore(categoryId: String, userId: String): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun performInitialCategorySync(userId: String): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    /**
     * Returns a placeholder Category object to represent uncategorized transactions.
     */
    override fun getUncategorizedCategoryPlaceholder(): Category =
        Category(id = null, name = "Other", color = 0xFF808080.toInt(), userId = null)

    override suspend fun saveProviderCategoryMapping(userId: String, providerName: String, categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentMappings = sharedPrefsManager.loadProviderCategoryMappings().toMutableMap()
            currentMappings[providerName] = categoryId
            sharedPrefsManager.saveProviderCategoryMappings(currentMappings)
            Log.d("CategoryRepoImpl", "Saved mapping: Provider '$providerName' -> CategoryID '$categoryId'")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CategoryRepoImpl", "Error saving provider category mapping for '$providerName'", e)
            Result.failure(e)
        }
    }

    override suspend fun getCategoryForProvider(userId: String, providerName: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val mappings = sharedPrefsManager.loadProviderCategoryMappings()
            val categoryId = mappings[providerName]
            if (categoryId != null) {
                Log.d("CategoryRepoImpl", "Found mapping for Provider '$providerName': CategoryID '$categoryId'")
            } else {
                Log.d("CategoryRepoImpl", "No mapping found for Provider '$providerName'")
            }
            Result.success(categoryId)
        } catch (e: Exception) {
            Log.e("CategoryRepoImpl", "Error getting category for provider '$providerName'", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProviderCategoryMapping(userId: String, providerName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentMappings = sharedPrefsManager.loadProviderCategoryMappings().toMutableMap()
            if (currentMappings.containsKey(providerName)) {
                currentMappings.remove(providerName)
                sharedPrefsManager.saveProviderCategoryMappings(currentMappings)
                Log.d("CategoryRepoImpl", "Deleted mapping for Provider '$providerName'")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CategoryRepoImpl", "Error deleting provider category mapping for '$providerName'", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllProviderCategoryMappings(userId: String): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val mappings = sharedPrefsManager.loadProviderCategoryMappings()
            Log.d("CategoryRepoImpl", "Loaded ${mappings.size} provider category mappings.")
            Result.success(mappings)
        } catch (e: Exception) {
            Log.e("CategoryRepoImpl", "Error getting all provider category mappings", e)
            Result.failure(e)
        }
    }

    override suspend fun getCategoryCount(): Int {
        return categoryDao.getCategoryCount()
    }
} 