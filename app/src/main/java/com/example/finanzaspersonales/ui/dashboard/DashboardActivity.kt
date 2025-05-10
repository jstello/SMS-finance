package com.example.finanzaspersonales.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.ui.categories.CategoriesActivity
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.ProviderStat
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.sms.SmsPermissionActivity
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.ui.auth.AuthViewModel
import com.example.finanzaspersonales.ui.auth.LoginScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.finanzaspersonales.data.auth.AuthRepository
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl
import com.example.finanzaspersonales.ui.transaction_list.TransactionListActivity
import kotlin.math.abs
import kotlin.math.round
import com.example.finanzaspersonales.ui.providers.ProvidersActivity
import com.example.finanzaspersonales.ui.add_transaction.AddTransactionActivity
import androidx.compose.ui.platform.LocalContext
import com.example.finanzaspersonales.ui.settings.SettingsActivity

// Helper function to format large numbers to millions with one decimal place
private fun formatToMillions(value: Float): String {
    val millions = value / 1_000_000.0
    val sign = if (value < 0) "-" else ""
    // Format to one decimal place
    val formattedValue = String.format(Locale.US, "%.1f", abs(millions))
    return "$sign$${formattedValue}M"
}

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    
    // Inject ViewModel using Hilt
    private val viewModel: DashboardViewModel by viewModels()
    
    // Permission launcher - updated to handle READ_CONTACTS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsPermissionGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val receiveSmsPermissionGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val contactsPermissionGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false

        if (smsPermissionGranted && receiveSmsPermissionGranted) {
            // Core SMS permissions granted, load data regardless of contacts permission
            viewModel.loadDashboardData()
            if (!contactsPermissionGranted) {
                Toast.makeText(
                    this,
                    "Contacts permission needed to show names for phone transfers",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Essential SMS Permissions denied
            Toast.makeText(
                this,
                "SMS permissions are required to detect transactions",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Choose Login or Dashboard based on auth state
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val currentUser by authViewModel.currentUserState.collectAsState()

                    if (currentUser == null) {
                        LoginScreen(onLoginSuccess = {
                            // After successful login, load dashboard data
                            viewModel.loadDashboardData()
                        })
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToCategories = { navigateToCategories() },
                            onNavigateToTransactions = { navigateToTransactions() },
                            onNavigateToProviders = { navigateToProviders() },
                            onNavigateToAddTransaction = { navigateToAddTransaction() }
                        )
                    }
                }
            }
        }

        // Check permissions when activity is created
        // Need AuthRepository instance for this check. We'll handle this with Hilt modules.
        // val currentUser = authRepository.currentUser
        // if (currentUser != null) {
            checkPermissionsAndLoadData() // Still need to check permissions
        //     viewModel.performInitialSyncIfNecessary(currentUser.uid)
        // }
    }
    
    private fun checkPermissionsAndLoadData() {
        val readSmsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val receiveSmsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val readContactsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!readSmsGranted) permissionsToRequest.add(Manifest.permission.READ_SMS)
        if (!receiveSmsGranted) permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        if (!readContactsGranted) permissionsToRequest.add(Manifest.permission.READ_CONTACTS)

        if (permissionsToRequest.isEmpty()) {
            Log.d("PERMISSIONS", "All permissions granted.")
            viewModel.loadDashboardData()
        } else {
            Log.d("PERMISSIONS", "Requesting missing permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Navigation functions
    private fun navigateToCategories() {
        startActivity(Intent(this, CategoriesActivity::class.java))
    }

    private fun navigateToTransactions() {
        startActivity(Intent(this, TransactionListActivity::class.java))
    }

    private fun navigateToProviders() {
        startActivity(Intent(this, ProvidersActivity::class.java))
    }

    // TODO: Implement navigation to AddTransactionScreen
    private fun navigateToAddTransaction() {
        // Start AddTransactionActivity using an Intent
        val intent = Intent(this, AddTransactionActivity::class.java)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToAddTransaction: () -> Unit // Add parameter for navigation
) {
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current // Get context for Toast and starting activities
    val monthlyBalance = monthlyIncome - monthlyExpenses

    LaunchedEffect(Unit) {
        // Initial load or refresh logic if needed within the composable
        // viewModel.loadDashboardData() // Maybe called from Activity already
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
                    }
                    IconButton(onClick = { 
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = { // Add FAB here
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
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
            // Financial summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "This Month's Overview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Text(
                            text = formatToMillions(monthlyBalance),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (monthlyBalance >= 0) MaterialTheme.colorScheme.primary else Color.Red
                        )
                        Text(
                            text = "This Month's Balance",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = formatToMillions(monthlyIncome),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Green
                                )
                                Text(
                                    text = "Income",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(
                                    text = formatToMillions(monthlyExpenses),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Red
                                )
                                Text(
                                    text = "Expenses",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Actions
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Categories
                DashboardActionItem(
                    icon = Icons.Default.Category,
                    title = "Categories",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCategories
                )
                
                // Transactions
                DashboardActionItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Transactions",
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTransactions
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Providers (renamed from Reports)
                DashboardActionItem(
                    icon = Icons.Default.Storefront,
                    title = "Providers",
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToProviders
                )
                
                // Jars (renamed from SMS Test)
                DashboardActionItem(
                    icon = Icons.Default.Savings,
                    title = "Jars",
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { Toast.makeText(context, "Coming Soon: Manage your savings jars/goals!", Toast.LENGTH_SHORT).show() }
                )
            }
            
            // Recent Transactions
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (recentTransactions.isEmpty()) {
                // Empty state for transactions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No recent transactions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transactions will appear here once they're detected from SMS messages",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Transaction list
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        recentTransactions.forEachIndexed { index, transaction ->
                            TransactionItem(transaction = transaction)
                            if (index < recentTransactions.size - 1) {
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionData) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.provider ?: "Unknown",
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormat.format(transaction.date),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = currencyFormat.format(transaction.amount),
            fontWeight = FontWeight.Bold,
            color = if (transaction.isIncome) Color.Green else Color.Red
        )
    }
}

@Composable
fun DashboardActionItem(
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
} 