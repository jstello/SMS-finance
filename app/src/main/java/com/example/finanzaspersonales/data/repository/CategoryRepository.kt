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

    /**
     * Saves a mapping between a provider name and a category ID for a user.
     */
    suspend fun saveProviderCategoryMapping(userId: String, providerName: String, categoryId: String): Result<Unit>

    /**
     * Retrieves the category ID mapped to a given provider name for a user.
     * Returns null if no mapping exists.
     */
    suspend fun getCategoryForProvider(userId: String, providerName: String): Result<String?>

    /**
     * Deletes a mapping for a given provider name for a user.
     */
    suspend fun deleteProviderCategoryMapping(userId: String, providerName: String): Result<Unit>

    /**
     * Retrieves all provider-category mappings for a user.
     * Returns a map where the key is the provider name and the value is the category ID.
     */
    suspend fun getAllProviderCategoryMappings(userId: String): Result<Map<String, String>>

    /**
     * Gets the total number of categories in the database.
     */
    suspend fun getCategoryCount(): Int
} 