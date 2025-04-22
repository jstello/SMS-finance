package com.example.finanzaspersonales.ui.transaction_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzaspersonales.data.model.TransactionData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(viewModel: TransactionListViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Search state and filter transactions
    var searchQuery by remember { mutableStateOf("") }
    val filteredTransactions = if (searchQuery.isBlank()) transactions else transactions.filter {
        (it.provider ?: "").contains(searchQuery, ignoreCase = true) ||
        (it.id ?: "").contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Transactions") },
                navigationIcon = {
                    val context = LocalContext.current
                    // Simple back arrow for now, assumes Activity handles finish()
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                filteredTransactions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isBlank()) "No transactions found." else "No results for \"$searchQuery\"")
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredTransactions, key = { it.id!! }) { transaction ->
                            TransactionListItem(transaction = transaction) { transactionId ->
                                // TODO: Implement click action to show category assignment dialog/screen
                                // For now, just log
                                println("Clicked transaction: $transactionId")
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionData,
    onClick: (String) -> Unit // Pass transaction ID on click
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { transaction.id?.let { onClick(it) } } // Call onClick with the ID
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = transaction.provider ?: "Unknown Provider",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(transaction.date),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Display Category ID if available (placeholder for now)
            Text(
                text = "Category: ${transaction.categoryId ?: "Unassigned"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                 overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = currencyFormat.format(transaction.amount),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (transaction.isIncome) Color(0xFF008000) else MaterialTheme.colorScheme.error // Darker Green
        )
    }
} 