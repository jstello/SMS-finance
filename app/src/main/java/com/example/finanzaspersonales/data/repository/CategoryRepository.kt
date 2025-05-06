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
     * Note: Reassigning transactions should be handled by a dedicated UseCase.
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    
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

    /**
     * Returns a placeholder Category object to represent uncategorized transactions.
     * This object typically has a null ID.
     */
    fun getUncategorizedCategoryPlaceholder(): Category
} 