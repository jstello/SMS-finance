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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.util.StringUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.ui.platform.LocalContext
import android.util.Log

/**
 * Screen showing details of a transaction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: CategoriesViewModel,
    transaction: TransactionData,
    onBack: () -> Unit,
    onCategorySelected: (TransactionData, Category) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val isAssigning by viewModel.isAssigningCategory.collectAsState()
    val assignmentResult by viewModel.assignmentResult.collectAsState()
    
    var currentCategory by remember { mutableStateOf<Category?>(null) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var pendingCategoryChange by remember { mutableStateOf<Category?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load the category for this transaction
    LaunchedEffect(transaction.id, transaction.categoryId) {
        currentCategory = viewModel.getCategoryForTransaction(transaction)
    }
    
    // Handle assignment result
    LaunchedEffect(assignmentResult) {
        val result = assignmentResult
        if (result != null) {
            if (result.isSuccess) {
                // Success! Update the displayed category
                currentCategory = pendingCategoryChange
                pendingCategoryChange = null // Clear pending state
                snackbarHostState.showSnackbar("Category updated successfully!")
            } else {
                // Failure! Show error message
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to update category"
                Log.e("TransactionDetailScreen", "Category assignment failed", result.exceptionOrNull())
                snackbarHostState.showSnackbar("Error: $errorMessage")
                pendingCategoryChange = null // Clear pending state on failure too
            }
            // Clear the result in the ViewModel so this effect doesn't re-trigger
            viewModel.clearAssignmentResult()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isAssigning
                    ) {
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
                .padding(16.dp)
        ) {
            // Transaction details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Amount
                    Text(
                        text = "$${StringUtils.formatAmount(transaction.amount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (transaction.isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Provider info
                    DetailRow(
                        label = "Provider",
                        value = transaction.provider ?: "Unknown"
                    )
                    
                    // Date info
                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    val dateString = transaction.date.let { dateFormat.format(it) }
                    
                    DetailRow(
                        label = "Date",
                        value = dateString
                    )
                    
                    // Transaction type
                    DetailRow(
                        label = "Type",
                        value = if (transaction.isIncome) "Income" else "Expense"
                    )
                    
                    // Contact name if available
                    transaction.contactName?.let {
                        DetailRow(
                            label = "Contact",
                            value = it
                        )
                    }
                    
                    // Account info if available
                    transaction.accountInfo?.let {
                        DetailRow(
                            label = "Account",
                            value = it
                        )
                    }
                    
                    // Category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(100.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isAssigning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Updating...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            if (currentCategory != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(currentCategory!!.color))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentCategory!!.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Uncategorized",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Change category button
                    OutlinedButton(
                        onClick = { showCategorySelector = true },
                        enabled = !isAssigning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Category")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Original message
                    Text(
                        text = "Original Message Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "From: ${transaction.provider ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = transaction.description ?: "No description available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Category selector dialog
    if (showCategorySelector) {
        CategorySelectorDialog(
            categories = categories,
            currentCategory = currentCategory,
            onDismiss = { if (!isAssigning) showCategorySelector = false },
            onCategorySelected = { selectedCategory ->
                // Store the choice temporarily
                pendingCategoryChange = selectedCategory
                Log.d("CAT_ASSIGN_UI", "Attempting assignment: TxID='${transaction.id}', Selected CatID='${selectedCategory.id}'")
                // Trigger the ViewModel assignment (which sets isAssigning = true)
                viewModel.assignCategoryToTransaction(transaction, selectedCategory)
                // Close the dialog immediately (or could keep it open until success/failure)
                showCategorySelector = false
            }
        )
    }
}

/**
 * Dialog for selecting a category
 */
@Composable
fun CategorySelectorDialog(
    categories: List<Category>,
    currentCategory: Category?,
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
                            color = if (currentCategory?.id == category.id) 
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

/**
 * Row displaying a label and value
 */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
} 