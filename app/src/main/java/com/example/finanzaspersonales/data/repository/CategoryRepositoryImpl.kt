package com.example.finanzaspersonales.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toMonth
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of the CategoryRepository
 */
class CategoryRepositoryImpl(
    private val context: Context,
    private val sharedPrefsManager: SharedPrefsManager,
    private val transactionRepository: TransactionRepository
) : CategoryRepository {
    
    /**
     * Get all categories
     */
    override suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
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
     * Delete a category
     */
    override suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        val categories = sharedPrefsManager.loadCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) {
            categories.removeAt(index)
            sharedPrefsManager.saveCategories(categories)
            
            // Find "Other" category to reassign any transaction with this category
            val otherCategory = categories.find { it.name == "Other" }
            if (otherCategory != null) {
                // Get transactions with this category and reassign them
                val transactions = transactionRepository.getTransactionsByCategory(categoryId)
                for (transaction in transactions) {
                    val transactionId = generateTransactionKey(transaction)
                    transactionRepository.assignCategoryToTransaction(transactionId, otherCategory.id)
                }
            }
        }
    }
    
    /**
     * Set a category for a transaction
     */
    override suspend fun setCategoryForTransaction(transactionId: String, categoryId: String) {
        withContext(Dispatchers.IO) {
            transactionRepository.assignCategoryToTransaction(transactionId, categoryId)
        }
    }
    
    /**
     * Get transactions by category
     */
    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> = withContext(Dispatchers.Default) {
        transactionRepository.getTransactionsByCategory(categoryId)
    }
    
    /**
     * Get spending by category
     */
    override suspend fun getSpendingByCategory(year: Int?, month: Int?): Map<Category, Float> = withContext(Dispatchers.Default) {
        val categories = getCategories()
        val transactions = transactionRepository.getTransactions()
        val filteredTransactions = transactionRepository.filterTransactions(
            transactions = transactions,
            year = year,
            month = month,
            isIncome = false // Only expenses
        )
        
        val result = mutableMapOf<Category, Float>()
        
        // Initialize all categories with 0.0f
        categories.forEach { category ->
            result[category] = 0.0f
        }
        
        // Get "Other" category
        val otherCategory = categories.find { it.name == "Other" } ?: categories.last()
        
        // Add up spending for each transaction
        filteredTransactions.forEach { transaction ->
            val categoryId = transaction.categoryId
            
            if (categoryId != null) {
                // If we have a category for this transaction
                val category = categories.find { it.id == categoryId }
                if (category != null) {
                    result[category] = result[category]!! + transaction.amount
                } else {
                    // Category no longer exists, add to Other
                    result[otherCategory] = result[otherCategory]!! + transaction.amount
                }
            } else {
                // No category assigned, add to Other
                result[otherCategory] = result[otherCategory]!! + transaction.amount
            }
        }
        
        // Return only categories with spending > 0
        result.filter { it.value > 0 }
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
        val message = transaction.originalMessage
        val input = "${message.address}_${message.body}_${message.dateTime?.time}"
        return java.util.UUID.nameUUIDFromBytes(input.toByteArray()).toString()
    }
} 