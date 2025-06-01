package com.example.finanzaspersonales.domain.tools

import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.domain.usecase.GetSpendingByCategoryUseCase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool for providing spending insights and answering user questions about their finances
 * This tool focuses on practical user questions like budget tracking and spending comparisons
 */
@Singleton
class SpendingInsightsTool @Inject constructor(
    private val getSpendingByCategoryUseCase: GetSpendingByCategoryUseCase,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val gson: Gson
) {
    
    /**
     * Input parameters for spending insights queries
     */
    data class InsightQuery(
        @SerializedName("question_type") val questionType: String, // "budget_check", "monthly_comparison", "category_analysis", "spending_trend"
        @SerializedName("category_name") val categoryName: String? = null, // e.g., "Food", "Transportation"
        @SerializedName("budget_amount") val budgetAmount: Float? = null, // User's budget for the category/month
        @SerializedName("time_period") val timePeriod: String = "current_month", // "current_month", "last_month", "current_year"
        @SerializedName("comparison_period") val comparisonPeriod: String? = null, // "last_month", "last_year", "same_month_last_year"
        @SerializedName("include_income") val includeIncome: Boolean = false
    )
    
    /**
     * Structured response with spending insights
     */
    data class SpendingInsight(
        @SerializedName("question_type") val questionType: String,
        @SerializedName("answer_summary") val answerSummary: String,
        @SerializedName("current_amount") val currentAmount: Float,
        @SerializedName("comparison_amount") val comparisonAmount: Float? = null,
        @SerializedName("budget_amount") val budgetAmount: Float? = null,
        @SerializedName("percentage_of_budget") val percentageOfBudget: Float? = null,
        @SerializedName("amount_remaining") val amountRemaining: Float? = null,
        @SerializedName("percentage_change") val percentageChange: Float? = null,
        @SerializedName("trend_direction") val trendDirection: String? = null, // "increasing", "decreasing", "stable"
        @SerializedName("category_breakdown") val categoryBreakdown: List<CategorySpending>? = null,
        @SerializedName("insights") val insights: List<String>,
        @SerializedName("recommendations") val recommendations: List<String>
    ) {
        data class CategorySpending(
            @SerializedName("category_name") val categoryName: String,
            @SerializedName("amount") val amount: Float,
            @SerializedName("percentage_of_total") val percentageOfTotal: Float
        )
    }
    
    /**
     * Main tool function - answers spending-related questions
     */
    fun getSpendingInsight(inputJson: String): String {
        return try {
            val query = gson.fromJson(inputJson, InsightQuery::class.java)
            val insight = runBlocking { generateInsight(query) }
            gson.toJson(insight)
        } catch (e: Exception) {
            gson.toJson(SpendingInsight(
                questionType = "error",
                answerSummary = "Error processing your question: ${e.message}",
                currentAmount = 0f,
                insights = listOf("Unable to process the request"),
                recommendations = listOf("Please try rephrasing your question")
            ))
        }
    }
    
    /**
     * Quick budget check for a specific category
     */
    fun checkCategoryBudget(categoryName: String, budgetAmount: Float): String {
        val query = InsightQuery(
            questionType = "budget_check",
            categoryName = categoryName,
            budgetAmount = budgetAmount
        )
        return getSpendingInsight(gson.toJson(query))
    }
    
    /**
     * Compare current month to previous month
     */
    fun compareToLastMonth(categoryName: String? = null): String {
        val query = InsightQuery(
            questionType = "monthly_comparison",
            categoryName = categoryName,
            comparisonPeriod = "last_month"
        )
        return getSpendingInsight(gson.toJson(query))
    }
    
    /**
     * Get spending breakdown for current month
     */
    fun getCurrentMonthBreakdown(): String {
        val query = InsightQuery(
            questionType = "category_analysis",
            timePeriod = "current_month"
        )
        return getSpendingInsight(gson.toJson(query))
    }
    
    /**
     * Core insight generation logic
     */
    private suspend fun generateInsight(query: InsightQuery): SpendingInsight {
        val currentDate = LocalDate.now()
        val currentYear = currentDate.year
        val currentMonth = currentDate.monthValue
        
        return when (query.questionType) {
            "budget_check" -> generateBudgetInsight(query, currentYear, currentMonth)
            "monthly_comparison" -> generateComparisonInsight(query, currentYear, currentMonth)
            "category_analysis" -> generateCategoryAnalysisInsight(query, currentYear, currentMonth)
            "spending_trend" -> generateTrendInsight(query, currentYear, currentMonth)
            else -> SpendingInsight(
                questionType = query.questionType,
                answerSummary = "Unknown question type",
                currentAmount = 0f,
                insights = listOf("Please specify a valid question type"),
                recommendations = emptyList()
            )
        }
    }
    
    /**
     * Generate budget-related insights
     */
    private suspend fun generateBudgetInsight(query: InsightQuery, year: Int, month: Int): SpendingInsight {
        val categoryName = query.categoryName
        val budgetAmount = query.budgetAmount ?: 0f
        
        if (categoryName != null) {
            // Budget check for specific category
            val categories = categoryRepository.getCategories()
            val targetCategory = categories.find { it.name.equals(categoryName, ignoreCase = true) }
            
            if (targetCategory == null) {
                return SpendingInsight(
                    questionType = "budget_check",
                    answerSummary = "Category '$categoryName' not found",
                    currentAmount = 0f,
                    insights = listOf("The category '$categoryName' doesn't exist in your system"),
                    recommendations = listOf("Check the category name spelling", "View available categories in the Categories section")
                )
            }
            
            val spending = getSpendingByCategoryUseCase(year, month, false)
            val categorySpending = spending[targetCategory] ?: 0f
            val percentageOfBudget = if (budgetAmount > 0) (categorySpending / budgetAmount) * 100 else 0f
            val amountRemaining = budgetAmount - categorySpending
            
            val answerSummary = when {
                percentageOfBudget <= 50 -> "You're doing great! You've spent ${String.format("%.1f", percentageOfBudget)}% of your $categoryName budget."
                percentageOfBudget <= 80 -> "You're on track. You've used ${String.format("%.1f", percentageOfBudget)}% of your $categoryName budget."
                percentageOfBudget <= 100 -> "Getting close! You've spent ${String.format("%.1f", percentageOfBudget)}% of your $categoryName budget."
                else -> "Over budget! You've exceeded your $categoryName budget by ${String.format("%.1f", percentageOfBudget - 100)}%."
            }
            
            val insights = mutableListOf<String>()
            insights.add("Current $categoryName spending: $${String.format("%.2f", categorySpending)}")
            insights.add("Budget: $${String.format("%.2f", budgetAmount)}")
            if (amountRemaining > 0) {
                insights.add("Remaining budget: $${String.format("%.2f", amountRemaining)}")
            } else {
                insights.add("Over budget by: $${String.format("%.2f", -amountRemaining)}")
            }
            
            val recommendations = mutableListOf<String>()
            when {
                percentageOfBudget > 100 -> {
                    recommendations.add("Consider reducing $categoryName expenses for the rest of the month")
                    recommendations.add("Review recent $categoryName transactions to identify areas to cut back")
                }
                percentageOfBudget > 80 -> {
                    recommendations.add("Monitor $categoryName spending closely for the rest of the month")
                    recommendations.add("Consider if any upcoming $categoryName expenses can be postponed")
                }
                else -> {
                    recommendations.add("Great job staying within budget!")
                    recommendations.add("You have room for additional $categoryName expenses if needed")
                }
            }
            
            return SpendingInsight(
                questionType = "budget_check",
                answerSummary = answerSummary,
                currentAmount = categorySpending,
                budgetAmount = budgetAmount,
                percentageOfBudget = percentageOfBudget,
                amountRemaining = amountRemaining,
                insights = insights,
                recommendations = recommendations
            )
        } else {
            // Overall monthly budget check
            val totalSpending = getSpendingByCategoryUseCase(year, month, false).values.sum()
            val percentageOfBudget = if (budgetAmount > 0) (totalSpending / budgetAmount) * 100 else 0f
            val amountRemaining = budgetAmount - totalSpending
            
            return SpendingInsight(
                questionType = "budget_check",
                answerSummary = "Total monthly spending: $${String.format("%.2f", totalSpending)} (${String.format("%.1f", percentageOfBudget)}% of budget)",
                currentAmount = totalSpending,
                budgetAmount = budgetAmount,
                percentageOfBudget = percentageOfBudget,
                amountRemaining = amountRemaining,
                insights = listOf("Monthly spending analysis", "Budget: $${String.format("%.2f", budgetAmount)}"),
                recommendations = if (percentageOfBudget > 90) listOf("Consider reducing expenses") else listOf("Spending is under control")
            )
        }
    }
    
    /**
     * Generate month-to-month comparison insights
     */
    private suspend fun generateComparisonInsight(query: InsightQuery, year: Int, month: Int): SpendingInsight {
        val lastMonth = if (month == 1) 12 else month - 1
        val lastMonthYear = if (month == 1) year - 1 else year
        
        val categoryName = query.categoryName
        
        if (categoryName != null) {
            // Compare specific category
            val categories = categoryRepository.getCategories()
            val targetCategory = categories.find { it.name.equals(categoryName, ignoreCase = true) }
            
            if (targetCategory == null) {
                return SpendingInsight(
                    questionType = "monthly_comparison",
                    answerSummary = "Category '$categoryName' not found",
                    currentAmount = 0f,
                    insights = listOf("Category not found"),
                    recommendations = emptyList()
                )
            }
            
            val currentSpending = getSpendingByCategoryUseCase(year, month, false)
            val lastMonthSpending = getSpendingByCategoryUseCase(lastMonthYear, lastMonth, false)
            
            val currentAmount = currentSpending[targetCategory] ?: 0f
            val lastMonthAmount = lastMonthSpending[targetCategory] ?: 0f
            
            val percentageChange = if (lastMonthAmount > 0) {
                ((currentAmount - lastMonthAmount) / lastMonthAmount) * 100
            } else if (currentAmount > 0) {
                100f // New spending where there was none before
            } else {
                0f
            }
            
            val trendDirection = when {
                percentageChange > 5 -> "increasing"
                percentageChange < -5 -> "decreasing"
                else -> "stable"
            }
            
            val answerSummary = when {
                percentageChange > 20 -> "Your $categoryName spending increased significantly by ${String.format("%.1f", percentageChange)}% compared to last month."
                percentageChange > 5 -> "Your $categoryName spending increased by ${String.format("%.1f", percentageChange)}% compared to last month."
                percentageChange < -20 -> "Great! Your $categoryName spending decreased by ${String.format("%.1f", -percentageChange)}% compared to last month."
                percentageChange < -5 -> "Your $categoryName spending decreased by ${String.format("%.1f", -percentageChange)}% compared to last month."
                else -> "Your $categoryName spending is similar to last month (${String.format("%.1f", percentageChange)}% change)."
            }
            
            return SpendingInsight(
                questionType = "monthly_comparison",
                answerSummary = answerSummary,
                currentAmount = currentAmount,
                comparisonAmount = lastMonthAmount,
                percentageChange = percentageChange,
                trendDirection = trendDirection,
                insights = listOf(
                    "This month: $${String.format("%.2f", currentAmount)}",
                    "Last month: $${String.format("%.2f", lastMonthAmount)}",
                    "Change: ${if (percentageChange >= 0) "+" else ""}${String.format("%.1f", percentageChange)}%"
                ),
                recommendations = when {
                    percentageChange > 20 -> listOf("Consider reviewing what caused the increase in $categoryName spending")
                    percentageChange < -20 -> listOf("Great job reducing $categoryName expenses!")
                    else -> listOf("Spending pattern is consistent")
                }
            )
        } else {
            // Compare total spending
            val currentSpending = getSpendingByCategoryUseCase(year, month, false).values.sum()
            val lastMonthSpending = getSpendingByCategoryUseCase(lastMonthYear, lastMonth, false).values.sum()
            
            val percentageChange = if (lastMonthSpending > 0) {
                ((currentSpending - lastMonthSpending) / lastMonthSpending) * 100
            } else {
                0f
            }
            
            return SpendingInsight(
                questionType = "monthly_comparison",
                answerSummary = "Total spending this month: $${String.format("%.2f", currentSpending)} vs last month: $${String.format("%.2f", lastMonthSpending)}",
                currentAmount = currentSpending,
                comparisonAmount = lastMonthSpending,
                percentageChange = percentageChange,
                trendDirection = if (percentageChange > 5) "increasing" else if (percentageChange < -5) "decreasing" else "stable",
                insights = listOf("Monthly spending comparison"),
                recommendations = if (percentageChange > 15) listOf("Consider reviewing this month's expenses") else listOf("Spending is consistent")
            )
        }
    }
    
    /**
     * Generate category analysis insights
     */
    private suspend fun generateCategoryAnalysisInsight(query: InsightQuery, year: Int, month: Int): SpendingInsight {
        val spending = getSpendingByCategoryUseCase(year, month, false)
        val totalSpending = spending.values.sum()
        
        val categoryBreakdown = spending.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                SpendingInsight.CategorySpending(
                    categoryName = category.name,
                    amount = amount,
                    percentageOfTotal = if (totalSpending > 0) (amount / totalSpending) * 100 else 0f
                )
            }
        
        val topCategory = categoryBreakdown.firstOrNull()
        val answerSummary = if (topCategory != null) {
            "Your biggest expense this month is ${topCategory.categoryName} at $${String.format("%.2f", topCategory.amount)} (${String.format("%.1f", topCategory.percentageOfTotal)}% of total spending)."
        } else {
            "No expenses recorded for this month."
        }
        
        val insights = mutableListOf<String>()
        insights.add("Total monthly spending: $${String.format("%.2f", totalSpending)}")
        insights.add("Number of categories with expenses: ${categoryBreakdown.size}")
        
        if (categoryBreakdown.size >= 3) {
            insights.add("Top 3 categories: ${categoryBreakdown.take(3).joinToString(", ") { "${it.categoryName} (${String.format("%.1f", it.percentageOfTotal)}%)" }}")
        }
        
        val recommendations = mutableListOf<String>()
        if (topCategory != null && topCategory.percentageOfTotal > 40) {
            recommendations.add("${topCategory.categoryName} represents a large portion of your spending - consider if this aligns with your priorities")
        }
        if (categoryBreakdown.size > 10) {
            recommendations.add("You have expenses across many categories - consider consolidating or reviewing smaller expenses")
        }
        
        return SpendingInsight(
            questionType = "category_analysis",
            answerSummary = answerSummary,
            currentAmount = totalSpending,
            categoryBreakdown = categoryBreakdown,
            insights = insights,
            recommendations = recommendations
        )
    }
    
    /**
     * Generate spending trend insights
     */
    private suspend fun generateTrendInsight(query: InsightQuery, year: Int, month: Int): SpendingInsight {
        // Get last 3 months of data for trend analysis
        val months = mutableListOf<Pair<Int, Int>>() // (year, month) pairs
        var currentYear = year
        var currentMonth = month
        
        repeat(3) {
            months.add(currentYear to currentMonth)
            currentMonth--
            if (currentMonth == 0) {
                currentMonth = 12
                currentYear--
            }
        }
        
        val monthlyTotals = months.map { (y, m) ->
            getSpendingByCategoryUseCase(y, m, false).values.sum()
        }.reversed() // Oldest to newest
        
        val trend = when {
            monthlyTotals.size < 2 -> "stable"
            monthlyTotals.last() > monthlyTotals[monthlyTotals.size - 2] * 1.1 -> "increasing"
            monthlyTotals.last() < monthlyTotals[monthlyTotals.size - 2] * 0.9 -> "decreasing"
            else -> "stable"
        }
        
        return SpendingInsight(
            questionType = "spending_trend",
            answerSummary = "Your spending treclaude nd over the last 3 months is $trend",
            currentAmount = monthlyTotals.lastOrNull() ?: 0f,
            trendDirection = trend,
            insights = listOf("3-month spending analysis"),
            recommendations = when (trend) {
                "increasing" -> listOf("Consider reviewing recent spending increases")
                "decreasing" -> listOf("Great job reducing expenses!")
                else -> listOf("Spending is consistent month-to-month")
            }
        )
    }
    
    /**
     * Get tool metadata for LLM integration
     */
    fun getToolMetadata(): String {
        val metadata = mapOf(
            "name" to "spending_insights",
            "description" to "Provides spending insights and answers questions about budgets, spending comparisons, and financial patterns",
            "parameters" to mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "question_type" to mapOf(
                        "type" to "string",
                        "enum" to listOf("budget_check", "monthly_comparison", "category_analysis", "spending_trend"),
                        "description" to "Type of spending question to answer"
                    ),
                    "category_name" to mapOf(
                        "type" to "string",
                        "description" to "Name of the category to analyze (optional)"
                    ),
                    "budget_amount" to mapOf(
                        "type" to "number",
                        "description" to "Budget amount for comparison (optional)"
                    ),
                    "time_period" to mapOf(
                        "type" to "string",
                        "enum" to listOf("current_month", "last_month", "current_year"),
                        "description" to "Time period for analysis"
                    ),
                    "comparison_period" to mapOf(
                        "type" to "string",
                        "enum" to listOf("last_month", "last_year", "same_month_last_year"),
                        "description" to "Period to compare against (optional)"
                    )
                ),
                "required" to listOf("question_type")
            )
        )
        return gson.toJson(metadata)
    }
} 