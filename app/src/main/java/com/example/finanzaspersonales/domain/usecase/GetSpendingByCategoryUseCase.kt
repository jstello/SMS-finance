package com.example.finanzaspersonales.domain.usecase

import android.util.Log
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
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

        val categories = categoryRepository.getCategories()
        Log.d("SPENDING_USE_CASE", "Total categories: ${categories.size}")

        val transactions = transactionRepository.getTransactions()
        Log.d("SPENDING_USE_CASE", "Total transactions: ${transactions.size}")

        // Filter transactions by year, month, and type
        val filteredTransactions = transactionRepository.filterTransactions(
            transactions = transactions,
            year = year,
            month = month,
            isIncome = isIncome
        )
        Log.d("SPENDING_USE_CASE", "Filtered transactions (Year: $year, Month: $month, IsIncome: $isIncome): ${filteredTransactions.size}")

        val result = mutableMapOf<Category, Float>()

        // Get "Other" category
        val otherCategory = categories.find { it.name == "Other" } ?: categories.lastOrNull()
        if (otherCategory == null) {
            Log.e("SPENDING_USE_CASE", "Could not find 'Other' category or any category to fall back on.")
            return@withContext emptyMap() // Return empty if no categories exist at all
        }
        Log.d("SPENDING_USE_CASE", "Other category: ${otherCategory.name} (${otherCategory.id})")

        // Add up spending for each transaction
        filteredTransactions.forEach { transaction ->
            val categoryId = transaction.categoryId
            val category = if (categoryId != null) {
                categories.find { it.id == categoryId }
            } else {
                null
            }

            val targetCategory = category ?: otherCategory // Assign to found category or fallback to 'Other'

            val oldAmount = result[targetCategory] ?: 0.0f
            val newAmount = oldAmount + transaction.amount
            result[targetCategory] = newAmount
            Log.v("SPENDING_USE_CASE", "Added ${transaction.amount} to '${targetCategory.name}', total: $newAmount (Tx Date: ${transaction.date})")
        }

        // Return only categories with spending > 0
        val nonZeroResults = result.filter { it.value > 0 }
        Log.d("SPENDING_USE_CASE", "Categories with spending > 0: ${nonZeroResults.size}")

        nonZeroResults
    }
} 