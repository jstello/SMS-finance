package com.example.finanzaspersonales.domain.usecase

import android.util.Log
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.local.SharedPrefsManager // For Other Category ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetSpendingByCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        year: Int?,
        month: Int?,
        isIncome: Boolean?
    ): Map<Category, Float> = withContext(Dispatchers.Default) {
        Log.d("SPENDING_USE_CASE", "====== Getting Spending By Category ======")
        Log.d("SPENDING_USE_CASE", "Filter - Year: $year, Month: $month, IsIncome: $isIncome")

        val allCategories = categoryRepository.getCategories()
        if (allCategories.isEmpty()) {
            Log.w("SPENDING_USE_CASE", "No categories found in the repository. Returning empty map.")
            return@withContext emptyMap()
        }
        Log.d("SPENDING_USE_CASE", "Total categories from repository: ${allCategories.size}")

        val transactions = transactionRepository.getTransactions(forceRefresh = false) // Use cached transactions for performance
        Log.d("SPENDING_USE_CASE", "Total transactions from repository: ${transactions.size}")

        val filteredTransactions = transactionRepository.filterTransactions(
            transactions = transactions,
            year = year,
            month = month,
            isIncome = isIncome
        )
        Log.d("SPENDING_USE_CASE", "Filtered transactions (Year: $year, Month: $month, IsIncome: $isIncome): ${filteredTransactions.size}")

        val result = mutableMapOf<Category, Float>()
        // Initialize all categories with 0.0f spending
        allCategories.forEach {
            result[it] = 0.0f
        }

        val otherCategoryId = SharedPrefsManager.DEFAULT_CATEGORIES.find { it.name == "Other" }?.id
        val otherCategory = allCategories.find { it.id == otherCategoryId }

        if (otherCategory == null) {
            Log.e("SPENDING_USE_CASE", "Critical: 'Other' category with ID '$otherCategoryId' not found in the database. Uncategorized transactions might be missed.")
            // Continue without a specific 'Other' category if not found, though this is an error state.
        }

        filteredTransactions.forEach { transaction ->
            val categoryId = transaction.categoryId
            val foundCategory = if (categoryId != null && categoryId.isNotBlank()) {
                allCategories.find { it.id == categoryId }
            } else {
                null // Explicitly null for uncategorized or blank categoryId
            }

            val targetCategory = foundCategory ?: otherCategory // Fallback to 'Other' if found, otherwise tx remains unmapped from a specific category object if 'Other' is also missing
            
            if (targetCategory != null) {
                 val currentAmount = result[targetCategory] ?: 0.0f
                 result[targetCategory] = currentAmount + transaction.amount
            } else if (categoryId.isNullOrBlank()) {
                // This case should ideally not happen if 'Other' category exists and is found.
                // If 'otherCategory' is null, uncategorized transactions will not be grouped.
                Log.w("SPENDING_USE_CASE", "Transaction with ID ${transaction.id} is uncategorized (CatID: '$categoryId') but 'Other' category was not found. This spending will not be attributed.")
            } else {
                Log.w("SPENDING_USE_CASE", "Transaction with ID ${transaction.id} has CategoryID '$categoryId' but this category was not found in allCategories. This spending will not be attributed.")
            }
        }
        Log.d("SPENDING_USE_CASE", "Spending calculation complete. Result map size: ${result.size}")
        result // Return all categories, including those with 0 spending for the period
    }
} 