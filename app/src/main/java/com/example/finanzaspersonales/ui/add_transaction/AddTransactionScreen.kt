package com.example.finanzaspersonales.ui.add_transaction

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.ui.categories.CategorySelectorDialog
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
// Assume ViewModelFactory exists or use Hilt
// import com.example.finanzaspersonales.ui.add_transaction.AddTransactionViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    // Provide factory or use Hilt for proper ViewModel instantiation
    viewModel: AddTransactionViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    // --- State Variables from UI ---
    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var isIncome by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }

    // --- State from ViewModel ---
    val categories by viewModel.categories.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // --- Date Picker Dialog Logic (Remains the same) ---
    if (showDatePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val newCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                date = newCalendar.timeInMillis
                showDatePicker = false
            },
            year, month, day
        ).show()
        // To prevent recomposition showing the dialog multiple times after configuration change
        LaunchedEffect(Unit) { showDatePicker = false }
    }

    // --- Category Selector Dialog ---
     if (showCategorySelector) {
         CategorySelectorDialog(
             categories = categories, // Use categories from ViewModel
             currentCategory = selectedCategory,
             onDismiss = { if (!isSaving) showCategorySelector = false }, // Prevent closing while saving
             onCategorySelected = { category ->
                 selectedCategory = category
                 showCategorySelector = false
             }
         )
     }

    // --- Effect to handle save result ---
    LaunchedEffect(saveResult) {
        val result = saveResult
        if (result != null) {
            if (result.isSuccess) {
                scope.launch {
                    snackbarHostState.showSnackbar("Transaction saved successfully!")
                    onNavigateBack() // Navigate back on success
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to save transaction"
                 scope.launch {
                    snackbarHostState.showSnackbar("Error: $errorMsg")
                 }
            }
            viewModel.clearSaveResult() // Reset the result in ViewModel
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // Add SnackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Add New Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSaving) { // Disable back while saving
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("$") },
                enabled = !isSaving // Disable when saving
            )

            // Description/Provider Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Provider") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving // Disable when saving
            )

            // Date Picker Trigger
            OutlinedTextField(
                value = dateFormat.format(Date(date)),
                onValueChange = { /* Read Only */ },
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving) { showDatePicker = true }, // Disable when saving
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }, enabled = !isSaving) { // Disable when saving
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                enabled = !isSaving // Disable when saving
            )

            // Income/Expense Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isIncome) "Income" else "Expense")
                Switch(
                    checked = isIncome,
                    onCheckedChange = { isIncome = it },
                    enabled = !isSaving // Disable when saving
                )
            }

            // Category Selector Trigger
            OutlinedTextField(
                value = selectedCategory?.name ?: "Select Category",
                onValueChange = { /* Read Only */ },
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving && categories.isNotEmpty()) { showCategorySelector = true }, // Disable when saving or no categories
                trailingIcon = {
                    // Optional: Add dropdown arrow icon
                },
                enabled = !isSaving // Disable when saving
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes button to bottom

            // Save Button
            Button(
                onClick = {
                    val amountFloat = amount.toFloatOrNull()
                    // Basic validation
                    if (amountFloat != null && amountFloat > 0 && description.isNotBlank()) {
                        viewModel.addManualTransaction(
                            amount = amountFloat,
                            description = description,
                            date = Date(date),
                            isIncome = isIncome,
                            categoryId = selectedCategory?.id
                        )
                    } else {
                        // Show validation error via Snackbar
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a valid amount and description.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving // Disable when saving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Transaction")
                }
            }
        }
    }
} 