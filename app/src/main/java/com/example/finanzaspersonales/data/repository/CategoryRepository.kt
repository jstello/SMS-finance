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
    suspend fun setCategoryForTransaction(transactionId: String, categoryId: String)
    
    /**
     * Get transactions by category
     */
    suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData>
    
    /**
     * Get category ID for a specific transaction
     */
    suspend fun getCategoryIdForTransaction(transactionId: String): String?
    
    /**
     * Get spending by category
     */
    suspend fun getSpendingByCategory(year: Int? = null, month: Int? = null): Map<Category, Float>
} 