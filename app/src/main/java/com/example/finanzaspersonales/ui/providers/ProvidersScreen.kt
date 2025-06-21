package com.example.finanzaspersonales.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.data.repository.ProviderStat
import java.text.NumberFormat
import java.util.Locale
import androidx.activity.ComponentActivity
import com.example.finanzaspersonales.domain.util.StringUtils
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable

enum class ProviderDateFilter { YTD, MONTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    viewModel: ProvidersViewModel = viewModel(), // Use default viewModel() factory if needed, ensure Factory is provided in Activity
    onBackClick: () -> Unit,
    onProviderClick: (providerName: String, from: Long, to: Long) -> Unit // Add callback for provider click
) {
    var selectedFilter by remember { mutableStateOf(ProviderDateFilter.YTD) }
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // Filter stats based on search query
    val filteredStats = remember(stats, searchQuery) { // Recalculate only when stats or query change
        if (searchQuery.isBlank()) {
            stats
        } else {
            stats.filter {
                it.provider.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Calculate and remember date range
    val dateRange = remember(selectedFilter) {
        val to = System.currentTimeMillis()
        val from = when (selectedFilter) {
            ProviderDateFilter.YTD -> viewModel.getStartOfYearTimestamp()
            ProviderDateFilter.MONTH -> viewModel.getStartOfMonthTimestamp()
        }
        from to to
    }

    // Load initial data based on the default filter (YTD)
    LaunchedEffect(dateRange) {
        viewModel.loadStats(dateRange.first, dateRange.second)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending by Provider") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ProviderDateFilter.YTD,
                    onClick = { selectedFilter = ProviderDateFilter.YTD },
                    label = { Text("Year-to-Date") }
                )
                FilterChip(
                    selected = selectedFilter == ProviderDateFilter.MONTH,
                    onClick = { selectedFilter = ProviderDateFilter.MONTH },
                    label = { Text("This Month") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Providers") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content based on state
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Error: $error", color = MaterialTheme.colorScheme.error)
                    }
                }
                filteredStats.isEmpty() -> { // Check filtered list for empty state
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (searchQuery.isBlank()) "No provider spending data found for the selected period."
                            else "No providers found matching \"$searchQuery\""
                        )
                    }
                }
                else -> {
                    // Determine the maximum total for scaling progress bars
                    val maxTotal = filteredStats.maxOfOrNull { it.total } ?: 1f // Use filtered list for max

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredStats, key = { it.provider }) { stat ->
                            ProviderRow(
                                stat = stat,
                                maxTotal = maxTotal,
                                onClick = { onProviderClick(stat.provider, dateRange.first, dateRange.second) } // Pass provider name up
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderRow(stat: ProviderStat, maxTotal: Float, onClick: () -> Unit) {
    // Ensure progress is between 0.0 and 1.0
    val progress = if (maxTotal > 0) (stat.total / maxTotal).coerceIn(0f, 1f) else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Make the row clickable
            .padding(vertical = 8.dp) // Add some vertical padding for touch target
    ) {
        // Provider Name (takes up available space)
        Text(
            text = stat.provider,
            modifier = Modifier.weight(2f).padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge
        )
        // Total Amount (fixed width for alignment)
        Text(
            text = StringUtils.formatToMillions(stat.total),
            modifier = Modifier.width(100.dp), // Adjust width as needed
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        // Progress Bar (takes remaining space after amount)
        LinearProgressIndicator(
            progress = { progress }, // Use lambda syntax for progress
            modifier = Modifier
                .height(8.dp)
                .weight(1f) // Let it take available horizontal space
                .padding(start = 8.dp),
            color = MaterialTheme.colorScheme.primary, // Or use a color mapping
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
} 