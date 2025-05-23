package com.example.finanzaspersonales.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzaspersonales.ui.components.ExpressiveFinancialCard
import com.example.finanzaspersonales.ui.components.ExpressiveGradientCard
import com.example.finanzaspersonales.ui.navigation.AdaptiveNavigationScaffold
import com.example.finanzaspersonales.ui.theme.SuccessGreen
import com.example.finanzaspersonales.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveDashboardScreen(
    viewModel: DashboardViewModel,
    windowSizeClass: WindowSizeClass,
    onNavigateToCategories: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val monthlyBalance by viewModel.monthlyBalance.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()

    AdaptiveNavigationScaffold(
        windowSizeClass = windowSizeClass,
        currentDestination = "dashboard",
        onDestinationClick = { destination ->
            when (destination) {
                "categories" -> onNavigateToCategories()
                "transactions" -> onNavigateToTransactions()
                "providers" -> onNavigateToProviders()
                "settings" -> onNavigateToSettings()
                "dashboard" -> { /* Already here */ }
            }
        },
        onAddClick = onNavigateToAddTransaction,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                "Financial Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        actions = {
                            IconButton(onClick = { viewModel.loadDashboardData() }) {
                                Icon(
                                    Icons.Filled.Refresh, 
                                    contentDescription = "Refresh Data",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    Icons.Filled.Settings, 
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onNavigateToDebug) {
                                Icon(
                                    Icons.Filled.BugReport, 
                                    contentDescription = "Debug",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onNavigateToAddTransaction,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Financial Overview Cards
                        ExpressiveGradientCard(
                            title = "Monthly Overview",
                            gradientColors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            ),
                            content = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Balance
                                    Text(
                                        text = formatToMillions(monthlyBalance),
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (monthlyBalance >= 0) SuccessGreen else ExpenseRed
                                    )
                                    Text(
                                        text = "Current Balance",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    
                                    // Income and Expenses Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = formatToMillions(monthlyIncome),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                            Text(
                                                text = "Income",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = formatToMillions(monthlyExpenses),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRed
                                            )
                                            Text(
                                                text = "Expenses",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        
                        // Quick Actions (removed redundant income/expense cards)
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExpressiveFinancialCard(
                                title = "Categories",
                                formattedAmount = categoryBreakdown.size.toString(),
                                icon = Icons.Default.Category,
                                subtitle = "${categoryBreakdown.size} categories",
                                onClick = onNavigateToCategories,
                                modifier = Modifier.weight(1f)
                            )
                            
                            ExpressiveFinancialCard(
                                title = "Providers", 
                                formattedAmount = recentTransactions.distinctBy { it.provider }.size.toString(),
                                icon = Icons.Default.Storefront,
                                subtitle = "${recentTransactions.distinctBy { it.provider }.size} providers",
                                onClick = onNavigateToProviders,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Recent Transactions Summary
                        if (recentTransactions.isNotEmpty()) {
                            ExpressiveGradientCard(
                                title = "Recent Activity",
                                gradientColors = listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                ),
                                content = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${recentTransactions.size} transactions in the last 30 days",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        Text(
                                            text = "Most recent: ${recentTransactions.firstOrNull()?.provider ?: "N/A"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

// Helper function to format large numbers to millions with one decimal place
private fun formatToMillions(value: Float): String {
    val millions = value / 1_000_000.0
    val sign = if (value < 0) "-" else ""
    val formattedValue = String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(millions))
    return "$sign$${formattedValue}M"
} 