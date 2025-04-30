package com.example.finanzaspersonales.data.repository

import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData

/**
 * Repository interface for handling categories
 */
interface CategoryRepository {
    
    /**
     * Get all categories
     */
    suspend fun getCategories(): List<Category>
    
    /**
     * Add a new category
     */
    suspend fun addCategory(category: Category)
    
    /**
     * Update a category
     */
    suspend fun updateCategory(category: Category)
    
    /**
     * Delete a category
     */
    suspend fun deleteCategory(categoryId: String)
    
    /**
     * Set a category for a transaction
     */
    suspend fun setCategoryForTransaction(transactionId: String, categoryId: String): Boolean
    
    /**
     * Get transactions by category
     */
    suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData>
    
    /**
     * Get category ID for a specific transaction
     */
    suspend fun getCategoryIdForTransaction(transactionId: String): String?
    
    /**
     * Get spending by category, optionally filtering by income or expense
     */
    suspend fun getSpendingByCategory(
        year: Int? = null,
        month: Int? = null,
        isIncome: Boolean? = null
    ): Map<Category, Float>

    /**
     * Save a category to Firestore
     */
    suspend fun saveCategoryToFirestore(category: Category): Result<Unit>

    /**
     * Get categories from Firestore
     */
    suspend fun getCategoriesFromFirestore(userId: String): Result<List<Category>>

    /**
     * Update a category in Firestore
     */
    suspend fun updateCategoryInFirestore(category: Category): Result<Unit>

    /**
     * Delete a category from Firestore
     */
    suspend fun deleteCategoryFromFirestore(categoryId: String, userId: String): Result<Unit>

    /**
     * Perform initial category sync
     */
    suspend fun performInitialCategorySync(userId: String): Result<Unit>
} 