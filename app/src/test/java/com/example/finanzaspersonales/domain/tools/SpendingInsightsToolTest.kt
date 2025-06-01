package com.example.finanzaspersonales.domain.tools

import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.domain.usecase.GetSpendingByCategoryUseCase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.junit.Assert.*

/**
 * Test class demonstrating SpendingInsightsTool usage with real user scenarios
 * Shows how the tool answers practical financial questions
 */
@RunWith(MockitoJUnitRunner::class)
class SpendingInsightsToolTest {
    
    @Mock
    private lateinit var mockGetSpendingByCategoryUseCase: GetSpendingByCategoryUseCase
    
    @Mock
    private lateinit var mockTransactionRepository: TransactionRepository
    
    @Mock
    private lateinit var mockCategoryRepository: CategoryRepository
    
    private lateinit var tool: SpendingInsightsTool
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    // Mock categories
    private val groceriesCategory = Category(id = "1", name = "Groceries", color = 0xFF4CAF50.toInt(), userId = null)
    private val transportCategory = Category(id = "2", name = "Transportation", color = 0xFF2196F3.toInt(), userId = null)
    private val restaurantsCategory = Category(id = "3", name = "Restaurants", color = 0xFFFF9800.toInt(), userId = null)
    private val entertainmentCategory = Category(id = "4", name = "Entertainment", color = 0xFF9C27B0.toInt(), userId = null)
    
    @Before
    fun setup() {
        tool = SpendingInsightsTool(
            mockGetSpendingByCategoryUseCase,
            mockTransactionRepository,
            mockCategoryRepository,
            gson
        )
        
        // Setup mock categories
        runBlocking {
            whenever(mockCategoryRepository.getCategories()).thenReturn(
                listOf(groceriesCategory, transportCategory, restaurantsCategory, entertainmentCategory)
            )
        }
    }
    
    @Test
    fun `test budget check - user asks how close they are to food budget`() = runBlocking {
        // Mock current month spending
        val currentSpending = mapOf(
            groceriesCategory to 450f,
            transportCategory to 200f,
            restaurantsCategory to 300f,
            entertainmentCategory to 100f
        )
        
        whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
        
        // User question: "How close am I to my $600 grocery budget this month?"
        val result = tool.checkCategoryBudget("Groceries", 600f)
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals("budget_check", insight.questionType)
        assertEquals(450f, insight.currentAmount)
        assertEquals(600f, insight.budgetAmount)
        assertEquals(75f, insight.percentageOfBudget)
        assertEquals(150f, insight.amountRemaining)
        assertTrue("Should contain budget status", insight.answerSummary.contains("75.0%"))
        assertTrue("Should have recommendations", insight.recommendations.isNotEmpty())
        
        println("Budget Check Result:")
        println(result)
    }
    
    @Test
    fun `test monthly comparison - user asks if spending is higher than last month`() = runBlocking {
        // Mock current month spending
        val currentSpending = mapOf(
            groceriesCategory to 450f,
            transportCategory to 200f,
            restaurantsCategory to 300f,
            entertainmentCategory to 100f
        )
        
        // Mock last month spending (lower)
        val lastMonthSpending = mapOf(
            groceriesCategory to 350f,
            transportCategory to 180f,
            restaurantsCategory to 250f,
            entertainmentCategory to 80f
        )
        
        whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
        whenever(mockGetSpendingByCategoryUseCase(2023, 12, false)).thenReturn(lastMonthSpending)
        
        // User question: "Is my restaurant spending higher this month compared to last month?"
        val result = tool.compareToLastMonth("Restaurants")
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals("monthly_comparison", insight.questionType)
        assertEquals(300f, insight.currentAmount)
        assertEquals(250f, insight.comparisonAmount)
        assertEquals(20f, insight.percentageChange)
        assertEquals("increasing", insight.trendDirection)
        assertTrue("Should mention increase", insight.answerSummary.contains("increased"))
        
        println("Monthly Comparison Result:")
        println(result)
    }
    
    @Test
    fun `test category analysis - user asks about spending breakdown`() = runBlocking {
        // Mock current month spending
        val currentSpending = mapOf(
            groceriesCategory to 450f,
            transportCategory to 200f,
            restaurantsCategory to 300f,
            entertainmentCategory to 100f
        )
        
        whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
        
        // User question: "What's my biggest expense this month?"
        val result = tool.getCurrentMonthBreakdown()
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals("category_analysis", insight.questionType)
        assertEquals(1050f, insight.currentAmount) // Total spending
        assertNotNull("Should have category breakdown", insight.categoryBreakdown)
        assertEquals("Groceries", insight.categoryBreakdown?.first()?.categoryName)
        assertTrue("Should mention biggest expense", insight.answerSummary.contains("Groceries"))
        
        println("Category Analysis Result:")
        println(result)
    }
    
    @Test
    fun `test over budget scenario - user is spending too much`() = runBlocking {
        // Mock spending that exceeds budget
        val currentSpending = mapOf(
            groceriesCategory to 750f, // Over $600 budget
            transportCategory to 200f,
            restaurantsCategory to 300f,
            entertainmentCategory to 100f
        )
        
        whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
        
        // User question: "How am I doing with my $600 grocery budget?"
        val result = tool.checkCategoryBudget("Groceries", 600f)
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals(750f, insight.currentAmount)
        assertEquals(600f, insight.budgetAmount)
        assertEquals(125f, insight.percentageOfBudget)
        assertEquals(-150f, insight.amountRemaining) // Negative = over budget
        assertTrue("Should warn about over budget", insight.answerSummary.contains("Over budget"))
        assertTrue("Should suggest reducing expenses", 
            insight.recommendations.any { it.contains("reducing") })
        
        println("Over Budget Result:")
        println(result)
    }
    
    @Test
    fun `test category not found scenario`() = runBlocking {
        // User asks about non-existent category
        val result = tool.checkCategoryBudget("NonExistentCategory", 500f)
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals("budget_check", insight.questionType)
        assertTrue("Should mention category not found", insight.answerSummary.contains("not found"))
        assertTrue("Should suggest checking spelling", 
            insight.recommendations.any { it.contains("spelling") })
        
        println("Category Not Found Result:")
        println(result)
    }
    
    @Test
    fun `test total monthly spending comparison`() = runBlocking {
        // Mock total spending for comparison
        val currentSpending = mapOf(
            groceriesCategory to 450f,
            transportCategory to 200f,
            restaurantsCategory to 300f,
            entertainmentCategory to 100f
        )
        
        val lastMonthSpending = mapOf(
            groceriesCategory to 400f,
            transportCategory to 150f,
            restaurantsCategory to 200f,
            entertainmentCategory to 150f
        )
        
        whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
        whenever(mockGetSpendingByCategoryUseCase(2023, 12, false)).thenReturn(lastMonthSpending)
        
        // User question: "Is my total spending higher this month?"
        val result = tool.compareToLastMonth() // No category = total comparison
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals(1050f, insight.currentAmount) // Current total
        assertEquals(900f, insight.comparisonAmount) // Last month total
        assertTrue("Should show percentage change", insight.percentageChange!! > 0)
        
        println("Total Spending Comparison Result:")
        println(result)
    }
    
    @Test
    fun `test LLM integration pattern with real user questions`() {
        println("\n=== Real User Questions Demo ===")
        
        // Setup mock data for realistic scenario
        runBlocking {
            val currentSpending = mapOf(
                groceriesCategory to 520f,
                transportCategory to 180f,
                restaurantsCategory to 280f,
                entertainmentCategory to 120f
            )
            
            val lastMonthSpending = mapOf(
                groceriesCategory to 450f,
                transportCategory to 200f,
                restaurantsCategory to 200f,
                entertainmentCategory to 100f
            )
            
            whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
            whenever(mockGetSpendingByCategoryUseCase(2023, 12, false)).thenReturn(lastMonthSpending)
        }
        
        // 1. User asks: "How close am I to my $600 food budget?"
        println("\n1. User Question: 'How close am I to my $600 food budget?'")
        val budgetCheck = tool.checkCategoryBudget("Groceries", 600f)
        val budgetInsight = gson.fromJson(budgetCheck, SpendingInsightsTool.SpendingInsight::class.java)
        println("Answer: ${budgetInsight.answerSummary}")
        println("Recommendations: ${budgetInsight.recommendations.joinToString("; ")}")
        
        // 2. User asks: "Am I spending more on restaurants than last month?"
        println("\n2. User Question: 'Am I spending more on restaurants than last month?'")
        val comparisonResult = tool.compareToLastMonth("Restaurants")
        val comparisonInsight = gson.fromJson(comparisonResult, SpendingInsightsTool.SpendingInsight::class.java)
        println("Answer: ${comparisonInsight.answerSummary}")
        
        // 3. User asks: "What's my biggest expense category this month?"
        println("\n3. User Question: 'What's my biggest expense category this month?'")
        val breakdownResult = tool.getCurrentMonthBreakdown()
        val breakdownInsight = gson.fromJson(breakdownResult, SpendingInsightsTool.SpendingInsight::class.java)
        println("Answer: ${breakdownInsight.answerSummary}")
        
        // 4. Show tool metadata for LLM
        println("\n4. Tool Metadata for LLM:")
        val metadata = tool.getToolMetadata()
        println(metadata)
    }
    
    @Test
    fun `test complex LLM query with JSON input`() = runBlocking {
        // Mock data
        val currentSpending = mapOf(
            groceriesCategory to 450f,
            transportCategory to 200f,
            restaurantsCategory to 300f,
            entertainmentCategory to 100f
        )
        
        whenever(mockGetSpendingByCategoryUseCase(2024, 1, false)).thenReturn(currentSpending)
        
        // Simulate LLM constructing a complex query
        val llmQuery = """
        {
            "question_type": "budget_check",
            "category_name": "Transportation",
            "budget_amount": 250.0,
            "time_period": "current_month"
        }
        """.trimIndent()
        
        val result = tool.getSpendingInsight(llmQuery)
        val insight = gson.fromJson(result, SpendingInsightsTool.SpendingInsight::class.java)
        
        assertEquals("budget_check", insight.questionType)
        assertEquals(200f, insight.currentAmount)
        assertEquals(250f, insight.budgetAmount)
        assertEquals(80f, insight.percentageOfBudget)
        
        println("Complex LLM Query Result:")
        println(result)
    }
} 