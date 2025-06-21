package com.example.finanzaspersonales.ui.transaction_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Category

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onBack: () -> Unit,
    onTransactionClick: (TransactionData) -> Unit = {}
) {
    val transactionItems by viewModel.transactionItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    // State for sort dropdown
    var showSortDropdown by remember { mutableStateOf(false) }

    // Load transactions when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.loadTransactions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Sort button with dropdown
                    Box {
                        IconButton(onClick = { showSortDropdown = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest First)") },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.DATE_DESC)
                                    showSortDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest First)") },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.DATE_ASC)
                                    showSortDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (Highest First)") },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.AMOUNT_DESC)
                                    showSortDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (Lowest First)") },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.AMOUNT_ASC)
                                    showSortDropdown = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                transactionItems.isEmpty() -> {
                    Text(
                        text = "No transactions found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn {
                        items(transactionItems) { item ->
                            TransactionListItem(
                                item = item,
                                onClick = { onTransactionClick(item.transaction) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual transaction list item
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    item: TransactionUiModel,
    onClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = item.transaction.contactName ?: item.transaction.provider ?: "Unknown Provider",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(item.transaction.date),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Display Category Name with Color Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Show color circle if category and color exist
                if (item.categoryColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(item.categoryColor))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Category: ${item.categoryName ?: "Unassigned"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = currencyFormat.format(item.transaction.amount),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (item.transaction.isIncome) Color(0xFF008000) else MaterialTheme.colorScheme.error // Darker Green
        )
    }
}