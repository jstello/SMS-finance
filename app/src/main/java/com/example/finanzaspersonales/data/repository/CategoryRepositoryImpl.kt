package com.example.finanzaspersonales.data.repository

import com.example.finanzaspersonales.data.db.mapper.toDomain
import com.example.finanzaspersonales.data.db.mapper.toEntity
import com.example.finanzaspersonales.data.local.room.dao.CategoryDao
import com.example.finanzaspersonales.data.model.Category
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the CategoryRepository
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
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

    override suspend fun saveProviderCategoryMapping(userId: String, providerName: String, categoryId: String): Result<Unit> =
        throw UnsupportedOperationException("Provider mapping is no longer supported")

    override suspend fun getCategoryForProvider(userId: String, providerName: String): Result<String?> =
        throw UnsupportedOperationException("Provider mapping is no longer supported")

    override suspend fun deleteProviderCategoryMapping(userId: String, providerName: String): Result<Unit> =
        throw UnsupportedOperationException("Provider mapping is no longer supported")

    override suspend fun getAllProviderCategoryMappings(userId: String): Result<Map<String, String>> =
        throw UnsupportedOperationException("Provider mapping is no longer supported")

    override suspend fun getCategoryCount(): Int {
        return categoryDao.getCategoryCount()
    }
} 