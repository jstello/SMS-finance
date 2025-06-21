package com.example.finanzaspersonales.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.finanzaspersonales.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by settingsViewModel.isLoading.collectAsState()
    val showConfirmationDialog by settingsViewModel.showConfirmationDialog.collectAsState()
    // val currentUser by settingsViewModel.currentUser.collectAsState() // Commented out due to auth removal
    val userMessage by settingsViewModel.userMessage.collectAsState()
    val forecastedIncomeAmount by settingsViewModel.forecastedIncomeAmount.collectAsState()
    
    // Local state for the text field
    var forecastedIncomeText by remember { mutableStateOf("") }
    
    // Initialize text field with current value
    LaunchedEffect(forecastedIncomeAmount) {
        if (forecastedIncomeText.isEmpty()) {
            forecastedIncomeText = NumberFormat.getInstance(Locale.US).format(forecastedIncomeAmount.toLong())
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(message = it)
                settingsViewModel.clearUserMessage() // Clear message after showing
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_settings)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (!navController.popBackStack()) {
                            (context as? android.app.Activity)?.finish()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Forecasted Income Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Monthly Salary Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "Set your expected monthly salary amount for balance forecasting. This helps predict your end-of-month balance when your salary hasn't arrived yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        OutlinedTextField(
                            value = forecastedIncomeText,
                            onValueChange = { newValue ->
                                // Only allow digits and commas
                                val filtered = newValue.filter { it.isDigit() || it == ',' }
                                forecastedIncomeText = filtered
                            },
                            label = { Text("Expected Monthly Salary (COP)") },
                            placeholder = { Text("15,254,625") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                try {
                                    // Remove commas and parse as number
                                    val cleanText = forecastedIncomeText.replace(",", "")
                                    val amount = cleanText.toFloatOrNull()
                                    if (amount != null && amount > 0) {
                                        settingsViewModel.updateForecastedIncome(amount)
                                    }
                                } catch (e: Exception) {
                                    // Handle error - maybe show a snackbar
                                }
                            },
                            enabled = !isLoading && forecastedIncomeText.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Update Forecast")
                        }
                    }
                }
                
                // Divider
                Divider()
                
                // Reset Data Section
                Text(
                    text = "Developer Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                Button(
                    onClick = { settingsViewModel.onShowDialog() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Clear All Transactions & Resync")
                    }
                }

                if (showConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { settingsViewModel.onDismissDialog() },
                        title = { Text("Confirm Reset") },
                        text = { Text("This will delete ALL your transaction data from the cloud and local cache, then rescan SMS. This action cannot be undone. Are you sure?") },
                        confirmButton = {
                            Button(
                                onClick = { settingsViewModel.onConfirmReset() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Reset Data")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { settingsViewModel.onDismissDialog() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
} 