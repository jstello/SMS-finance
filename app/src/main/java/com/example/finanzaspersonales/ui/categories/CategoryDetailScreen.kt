package com.example.finanzaspersonales.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.util.ContactsUtil
import com.example.finanzaspersonales.domain.util.StringUtils
import com.example.finanzaspersonales.domain.util.StringUtils.extractPhoneNumber
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Screen showing details of a category and its transactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    viewModel: CategoriesViewModel,
    category: Category,
    onBack: () -> Unit,
    onTransactionClick: (TransactionData) -> Unit
) {
    val transactions by viewModel.categoryTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    
    // Load transactions for this category with the same filters
    LaunchedEffect(category.id, selectedYear, selectedMonth) {
        viewModel.loadTransactionsForCategory(
            categoryId = category.id,
            year = selectedYear,
            month = selectedMonth,
            isIncome = false // Only show expenses to match main screen
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Category summary card
                CategorySummaryCard(category, transactions)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transactions list
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Transaction table header
                TransactionTableHeader(
                    currentSortField = sortField,
                    currentSortOrder = sortOrder,
                    onSortByAmount = { viewModel.updateSort(TransactionSortField.AMOUNT) }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions found for this category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(transactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Table header for transactions with sorting
 */
@Composable
fun TransactionTableHeader(
    currentSortField: TransactionSortField,
    currentSortOrder: SortOrder,
    onSortByAmount: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Transaction info column
        Text(
            text = "Transaction",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        
        // Amount column with sort
        Row(
            modifier = Modifier
                .clickable(onClick = onSortByAmount),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = if (currentSortField == TransactionSortField.AMOUNT) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Show sort direction if sorting by amount
            if (currentSortField == TransactionSortField.AMOUNT) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (currentSortOrder == SortOrder.ASCENDING) 
                        Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Sort direction",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    HorizontalDivider()
}

/**
 * Summary card for the category
 */
@Composable
fun CategorySummaryCard(category: Category, transactions: List<TransactionData>) {
    val totalAmount = transactions.sumOf { it.amount.toDouble() }.toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category color indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(category.color))
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Category name
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total spending
            Text(
                text = "Total Spending: $${StringUtils.formatAmount(totalAmount)}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transaction count
            Text(
                text = "${transactions.size} transaction${if (transactions.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Item displaying a transaction
 */
@Composable
fun TransactionItem(
    transaction: TransactionData,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = transaction.date.let { dateFormat.format(it) }
    
    // Extract potential phone number from provider
    val phoneNumber = extractPhoneNumber(transaction.provider)
    
    // Look up contact name if a phone number is extracted
    val contactName = remember(phoneNumber) {
        phoneNumber?.let { ContactsUtil.getContactNameFromPhoneNumber(context, it) }
    }
    
    // Display contact name if available, otherwise show provider/address
    val displayText = when {
        contactName != null -> contactName
        phoneNumber != null -> "${transaction.provider} (${phoneNumber})"
        else -> transaction.provider ?: transaction.originalMessage.address
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Provider/Contact name
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Date
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Amount
        Text(
            text = "$${StringUtils.formatAmount(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        )
    }
} 