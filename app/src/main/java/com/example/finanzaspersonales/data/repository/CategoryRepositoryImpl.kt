package com.example.finanzaspersonales.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
        val message = transaction.originalMessage
        val input = "${message.address}_${message.body}_${message.dateTime?.time}"
        return java.util.UUID.nameUUIDFromBytes(input.toByteArray()).toString()
    }
} 