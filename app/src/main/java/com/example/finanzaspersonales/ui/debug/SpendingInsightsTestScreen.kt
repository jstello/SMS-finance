package com.example.finanzaspersonales.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzaspersonales.domain.tools.SpendingInsightsTool
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch

/**
 * Debug screen for testing SpendingInsightsTool with real user scenarios
 * Demonstrates practical financial questions users would ask
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingInsightsTestScreen(
    tool: SpendingInsightsTool
) {
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val scope = rememberCoroutineScope()
    
    var selectedQuestionType by remember { mutableStateOf("budget_check") }
    var categoryName by remember { mutableStateOf("Groceries") }
    var budgetAmount by remember { mutableStateOf("600") }
    var insightResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Spending Insights Tool Testing",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Test real user questions about budgets, spending comparisons, and financial insights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Quick User Questions Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick User Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Budget Check Questions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = tool.checkCategoryBudget("Groceries", 600f)
                                    insightResult = result
                                } catch (e: Exception) {
                                    insightResult = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Food Budget?", fontSize = 12.sp)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = tool.checkCategoryBudget("Transportation", 300f)
                                    insightResult = result
                                } catch (e: Exception) {
                                    insightResult = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transport Budget?", fontSize = 12.sp)
                    }
                }
                
                // Comparison Questions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = tool.compareToLastMonth("Restaurants")
                                    insightResult = result
                                } catch (e: Exception) {
                                    insightResult = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurants vs Last Month?", fontSize = 12.sp)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = tool.compareToLastMonth() // Total spending
                                    insightResult = result
                                } catch (e: Exception) {
                                    insightResult = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Total Spending vs Last Month?", fontSize = 12.sp)
                    }
                }
                
                // Analysis Questions
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val result = tool.getCurrentMonthBreakdown()
                                insightResult = result
                            } catch (e: Exception) {
                                insightResult = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("What's my biggest expense this month?")
                }
            }
        }
        
        // Custom Query Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Custom Query",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Question Type Selection
                Text(
                    text = "Question Type",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "budget_check" to "Budget Check",
                        "monthly_comparison" to "Compare Months",
                        "category_analysis" to "Category Analysis",
                        "spending_trend" to "Spending Trend"
                    ).forEach { (type, label) ->
                        FilterChip(
                            onClick = { selectedQuestionType = type },
                            label = { Text(label, fontSize = 11.sp) },
                            selected = selectedQuestionType == type
                        )
                    }
                }
                
                // Category Input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Groceries, Transportation, Restaurants") }
                )
                
                // Budget Amount Input (for budget checks)
                if (selectedQuestionType == "budget_check") {
                    OutlinedTextField(
                        value = budgetAmount,
                        onValueChange = { budgetAmount = it },
                        label = { Text("Budget Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., 600") }
                    )
                }
                
                // Execute Custom Query
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val query = SpendingInsightsTool.InsightQuery(
                                    questionType = selectedQuestionType,
                                    categoryName = if (categoryName.isBlank()) null else categoryName,
                                    budgetAmount = if (selectedQuestionType == "budget_check" && budgetAmount.isNotBlank()) {
                                        budgetAmount.toFloatOrNull()
                                    } else null
                                )
                                val queryGson = Gson()
                                val result = tool.getSpendingInsight(queryGson.toJson(query))
                                insightResult = result
                            } catch (e: Exception) {
                                insightResult = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Ask Question")
                }
            }
        }
        
        // Example User Scenarios
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Example User Scenarios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val scenarios = listOf(
                    "💰 \"How close am I to my $600 grocery budget this month?\"",
                    "📈 \"Am I spending more on restaurants than last month?\"",
                    "🏆 \"What's my biggest expense category this month?\"",
                    "📊 \"Is my total spending higher this month compared to last month?\"",
                    "🚗 \"How am I doing with my $300 transportation budget?\"",
                    "🎯 \"Show me my spending breakdown for this month\""
                )
                
                scenarios.forEach { scenario ->
                    Text(
                        text = scenario,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        // Results Section
        if (insightResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Insight Result",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Parse and show key info
                        val parsedInsight = remember(insightResult) {
                            try {
                                gson.fromJson(insightResult, SpendingInsightsTool.SpendingInsight::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        parsedInsight?.let { insight ->
                            if (insight.questionType != "error") {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = insight.questionType.replace("_", " ").uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show user-friendly summary first
                    val parsedInsightForSummary = remember(insightResult) {
                        try {
                            gson.fromJson(insightResult, SpendingInsightsTool.SpendingInsight::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    parsedInsightForSummary?.let { insight ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Answer:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = insight.answerSummary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                if (insight.recommendations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Recommendations:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    insight.recommendations.forEach { recommendation ->
                                        Text(
                                            text = "• $recommendation",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Raw JSON (collapsible)
                    var showRawJson by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showRawJson = !showRawJson },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showRawJson) "Hide Raw JSON" else "Show Raw JSON")
                    }
                    
                    if (showRawJson) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val formattedJson = remember(insightResult) {
                            try {
                                val parsed = gson.fromJson(insightResult, Any::class.java)
                                gson.toJson(parsed)
                            } catch (e: Exception) {
                                insightResult
                            }
                        }
                        
                        SelectionContainer {
                            Text(
                                text = formattedJson,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        
        // Tool Metadata
        OutlinedButton(
            onClick = {
                scope.launch {
                    try {
                        val metadata = tool.getToolMetadata()
                        val metadataObject = gson.fromJson(metadata, Any::class.java)
                        insightResult = "Tool Metadata:\n${gson.toJson(metadataObject)}"
                    } catch (e: Exception) {
                        insightResult = "Error getting metadata: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Tool Metadata (for LLM)")
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
} 