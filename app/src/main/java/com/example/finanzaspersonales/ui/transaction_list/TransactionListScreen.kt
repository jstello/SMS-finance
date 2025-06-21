package com.example.finanzaspersonales.ui.transaction_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Category

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onBack: () -> Unit
) {
    val transactionItems by viewModel.transactionItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isAssigningCategory by viewModel.isAssigningCategory.collectAsState()
    val assignmentResult by viewModel.assignmentResult.collectAsState()

    // State for sort dropdown
    var showSortDropdown by remember { mutableStateOf(false) }
    
    // State for category selection
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedTransactionForCategory by remember { mutableStateOf<TransactionUiModel?>(null) }

    // Handle assignment result
    LaunchedEffect(assignmentResult) {
        assignmentResult?.let { result ->
            if (result.isSuccess) {
                // Category assigned successfully
                selectedTransactionForCategory = null
                showCategoryDialog = false
            }
            // Clear the result after handling
            viewModel.clearAssignmentResult()
        }
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
                                onLongClick = {
                                    selectedTransactionForCategory = item
                                    showCategoryDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Show loading overlay when assigning category
            if (isAssigningCategory) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Category selection dialog
    if (showCategoryDialog && selectedTransactionForCategory != null) {
        CategorySelectorDialog(
            categories = categories,
            currentCategoryId = selectedTransactionForCategory!!.transaction.categoryId,
            onDismiss = {
                showCategoryDialog = false
                selectedTransactionForCategory = null
            },
            onCategorySelected = { category ->
                selectedTransactionForCategory?.transaction?.id?.let { transactionId ->
                    category.id?.let { categoryId ->
                        viewModel.assignCategoryToTransaction(transactionId, categoryId)
                    }
                }
            }
        )
    }
}

/**
 * Individual transaction list item
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    item: TransactionUiModel,
    onLongClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
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

/**
 * Dialog for selecting a category
 */
@Composable
fun CategorySelectorDialog(
    categories: List<Category>,
    currentCategoryId: String?,
    onDismiss: () -> Unit,
    onCategorySelected: (Category) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category color indicator
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
                            color = if (currentCategoryId == category.id) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}