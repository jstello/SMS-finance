package com.example.finanzaspersonales.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.domain.util.StringUtils
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Divider

/**
 * Categories Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onAddCategory: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    
    // State for filter dropdowns
    var showYearDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    
    // Available years and months
    val currentYear = LocalDate.now().year
    val years = (currentYear - 2..currentYear).toList()
    val months = Month.values().toList()
    
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshTransactionData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCategory) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Time period filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Year selector
                Box {
                    OutlinedButton(
                        onClick = { showYearDropdown = true }
                    ) {
                        Text(selectedYear?.toString() ?: "All Years")
                    }
                    
                    DropdownMenu(
                        expanded = showYearDropdown,
                        onDismissRequest = { showYearDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Years") },
                            onClick = {
                                viewModel.setYearMonth(null, selectedMonth)
                                showYearDropdown = false
                            }
                        )
                        
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    viewModel.setYearMonth(year, selectedMonth)
                                    showYearDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Month selector
                Box {
                    OutlinedButton(
                        onClick = { showMonthDropdown = true }
                    ) {
                        Text(selectedMonth?.let { 
                            Month.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault()) 
                        } ?: "All Months")
                    }
                    
                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { showMonthDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Months") },
                            onClick = {
                                viewModel.setYearMonth(selectedYear, null)
                                showMonthDropdown = false
                            }
                        )
                        
                        months.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                                onClick = {
                                    viewModel.setYearMonth(selectedYear, month.value)
                                    showMonthDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // --- Spending Overview Section --- 
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .weight(1f) // Allow this Column to take remaining space
                ) {
                    Text(
                        text = "Spending by Category",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // --- Combined Chart and Category List (Scrollable) --- 
                    SpendingBarChart(
                         categorySpending = categorySpending, 
                         currencyFormat = currencyFormat,
                         onCategoryClick = onCategoryClick,
                         allCategories = categories // Pass all categories
                    )
                }
            }
        }
    }
}

/**
 * Category item row
 */
@Composable
fun CategoryItem(
    category: Category,
    spending: Float,
    currencyFormat: NumberFormat,
    onClick: (Category) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(category) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(category.color))
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Category name
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        // Spending amount
        Text(
            text = currencyFormat.format(spending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Spending summary card
 */
@Composable
fun SpendingSummary(categorySpending: Map<Category, Float>) {
    val totalSpending = categorySpending.values.sum()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Total: $${StringUtils.formatAmount(totalSpending)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show top categories by spending
            categorySpending.entries
                .sortedByDescending { it.value }
                .take(3)
                .forEach { (category, amount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(category.color))
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "$${StringUtils.formatAmount(amount)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
        }
    }
}

// --- Updated Composable for the Bar Chart & Zero Spending List ---
@Composable
fun SpendingBarChart(
    categorySpending: Map<Category, Float>,
    currencyFormat: NumberFormat,
    onCategoryClick: (Category) -> Unit,
    allCategories: List<Category> // Add parameter for all categories
) {
    val spendingCategories = remember(categorySpending) {
        categorySpending.filter { it.value > 0 }.toList().sortedByDescending { it.second }
    }
    val maxSpending = remember(spendingCategories) {
        spendingCategories.firstOrNull()?.second?.coerceAtLeast(1f) ?: 1f 
    }
    
    val spendingCategoryIds = remember(spendingCategories) {
        spendingCategories.map { it.first.id }.toSet()
    }
    
    val zeroSpendingCategories = remember(allCategories, spendingCategoryIds) {
        allCategories.filter { it.id !in spendingCategoryIds }.sortedBy { it.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize() // Allow column to take max space in parent
            .verticalScroll(rememberScrollState()), // Make the whole combined list scrollable
        verticalArrangement = Arrangement.spacedBy(8.dp) 
    ) {
        // --- Bars for categories with spending --- 
        if (spendingCategories.isEmpty()) {
            Text(
                text = "No spending data for the selected period.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            spendingCategories.forEach { (category, spending) ->
                val barFraction = (spending / maxSpending).coerceIn(0f, 1f)
                
                // Bar Row (clickable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(category) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Color Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(category.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Category Name and Amount
                    Column(modifier = Modifier.width(100.dp)) { 
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currencyFormat.format(spending),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f) 
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant) 
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = barFraction)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(category.color))
                        )
                    }
                }
            }
        }

        // --- List of categories with zero spending --- 
        if (spendingCategories.isNotEmpty() && zeroSpendingCategories.isNotEmpty()) {
             Divider(modifier = Modifier.padding(vertical = 8.dp)) // Add divider if both lists have items
        }
        
        if (zeroSpendingCategories.isNotEmpty()) {
            zeroSpendingCategories.forEach { category ->
                // Zero Spending Row (clickable)
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(category) }
                        .padding(vertical = 8.dp), // Slightly more padding than bars
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Color Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(category.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Category Name
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium, // Use slightly larger font than bar labels
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Optionally display $0.00 or hide amount
                    Text(
                        text = currencyFormat.format(0f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (spendingCategories.isEmpty()) {
            // If there was no spending AND no other categories exist (unlikely but possible)
            // The "No spending data" text is already shown above.
        }
    }
} 