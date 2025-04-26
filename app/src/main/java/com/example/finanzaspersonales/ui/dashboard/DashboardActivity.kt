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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.finanzaspersonales.data.auth.AuthRepository
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl
import com.example.finanzaspersonales.ui.auth.LoginScreen
import androidx.compose.ui.platform.LocalContext
import com.example.finanzaspersonales.ui.transaction_list.TransactionListActivity
import kotlin.math.abs
import kotlin.math.round
import com.example.finanzaspersonales.ui.providers.ProvidersActivity

// Helper function to format large numbers to millions with one decimal place
private fun formatToMillions(value: Float): String {
    val millions = value / 1_000_000.0
    val sign = if (value < 0) "-" else ""
    // Format to one decimal place
    val formattedValue = String.format(Locale.US, "%.1f", abs(millions))
    return "$sign$${formattedValue}M"
}

class DashboardActivity : ComponentActivity() {
    
    private lateinit var viewModel: DashboardViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var sharedPrefsManager: SharedPrefsManager
    
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
        
        // Instantiate AuthRepository (TODO: Replace with DI later)
        authRepository = AuthRepositoryImpl()
        
        // Instantiate SharedPrefsManager
        sharedPrefsManager = SharedPrefsManager(this)

        // --- Instantiate Repositories (Needs proper DI later) ---
        // Create TransactionRepo first (as dummy for CategoryRepo)
        val dummyTransactionRepository = object : TransactionRepository { 
            override suspend fun getAllSmsMessages(): List<SmsMessage> = emptyList()
            override suspend fun getTransactions(): List<TransactionData> = emptyList()
            override suspend fun filterTransactions(transactions: List<TransactionData>, year: Int?, month: Int?, isIncome: Boolean?): List<TransactionData> = emptyList()
            override suspend fun getTransactionById(id: String): TransactionData? = null
            override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> = emptyList()
            override suspend fun assignCategoryToTransaction(transactionId: String, categoryId: String): Boolean = false
            override suspend fun refreshSmsData(limitToRecentMonths: Int) { /* Dummy */ }
            override suspend fun initializeTransactions() { /* Dummy */ }
            // Add stubs for new Firestore/Sync functions to satisfy interface
            override suspend fun saveTransactionToFirestore(transaction: TransactionData): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun getTransactionsFromFirestore(userId: String): Result<List<TransactionData>> = Result.failure(NotImplementedError())
            override suspend fun updateTransactionInFirestore(transaction: TransactionData): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun deleteTransactionFromFirestore(transactionId: String, userId: String): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun performInitialTransactionSync(userId: String, syncStartDate: Long): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun getProviderStats(from: Long, to: Long): List<ProviderStat> = emptyList() // Dummy implementation
        }
        
        // Create CategoryRepository (using dummy TransactionRepo)
        categoryRepository = CategoryRepositoryImpl(
            context = this,
            sharedPrefsManager = sharedPrefsManager,
            transactionRepository = dummyTransactionRepository, // Use dummy for now
            authRepository = authRepository // Provide the AuthRepository instance
        )

        // Create dependencies needed for Real TransactionRepository
        val smsDataSource = SmsDataSource(this)
        val extractTransactionDataUseCase = ExtractTransactionDataUseCase(this)
        val categoryAssignmentUseCase = CategoryAssignmentUseCase(categoryRepository) // Use the real CategoryRepo
        
        // Now create the real TransactionRepository
        val transactionRepository = TransactionRepositoryImpl(
            context = this,
            smsDataSource = smsDataSource,
            extractTransactionDataUseCase = extractTransactionDataUseCase,
            categoryAssignmentUseCase = categoryAssignmentUseCase,
            sharedPrefsManager = sharedPrefsManager,
            authRepository = authRepository // Add authRepository parameter
        )
        // -----------------------------------------------------------
        
        // Create ViewModel using the Factory with all dependencies
        viewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(transactionRepository, categoryRepository, sharedPrefsManager)
        )[DashboardViewModel::class.java]
        
        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- Authentication State Check ---
                    val currentUser by authRepository.currentUserState.collectAsState()
                    val context = LocalContext.current // Get context for passing down

                    if (currentUser == null) {
                        // --- User Not Logged In: Show Login Screen ---
                        LoginScreen(
                            onLoginSuccess = {
                                Log.d("DashboardActivity", "Login successful, auth state should update.")
                            }
                        )
                    } else {
                        // --- User Logged In: Check Sync Status & Show appropriate screen ---
                        val userId = currentUser!!.uid // Safe non-null access here
                        val isSyncing by viewModel.isSyncing.collectAsState()
                        val syncError by viewModel.syncError.collectAsState()

                        // Trigger sync check when user becomes non-null
                        LaunchedEffect(userId) { 
                            viewModel.checkAndPerformInitialSync(userId)
                        }

                        // --- Display Sync State or Dashboard ---
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when {
                                isSyncing -> {
                                    // Show Syncing indicator
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                         CircularProgressIndicator()
                                         Spacer(modifier = Modifier.height(8.dp))
                                         Text("Performing initial sync...")
                                    }
                                }
                                syncError != null -> {
                                     // Show Sync Error message
                                     Text("Error during sync: $syncError", color = MaterialTheme.colorScheme.error)
                                     // TODO: Add a retry button?
                                }
                                else -> {
                                    // Sync complete (or not needed), show Dashboard
                                     Log.d("DashboardActivity", "User logged in: $userId, Sync complete/not needed, showing Dashboard")
                                     // Collect necessary states for DashboardScreen
                                    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
                                    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
                                    val recentTransactions by viewModel.recentTransactions.collectAsState()
                                    val isLoading by viewModel.isLoading.collectAsState() // Main dashboard loading

                                    DashboardScreen(
                                        monthlyExpenses = monthlyExpenses,
                                        monthlyIncome = monthlyIncome,
                                        recentTransactions = recentTransactions,
                                        isLoading = isLoading, // Use dashboard loading state here
                                        onRefresh = { checkAndRequestPermissions() },
                                        onCategoriesClick = {
                                            context.startActivity(Intent(context, CategoriesActivity::class.java))
                                        },
                                        onSmsTestClick = {
                                            context.startActivity(Intent(context, SmsPermissionActivity::class.java))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Request permissions *after* setting content
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
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
            // All permissions already granted, load data
            Log.d("PERMISSIONS", "All required permissions already granted.")
            viewModel.loadDashboardData() // Ensure data loads if permissions already exist
        } else {
            // Request the missing permissions
            Log.d("PERMISSIONS", "Requesting missing permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            // Consider showing rationale here if needed, though the first-time OS dialog is usually sufficient
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    monthlyExpenses: Float,
    monthlyIncome: Float,
    recentTransactions: List<TransactionData>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onCategoriesClick: () -> Unit,
    onSmsTestClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val context = LocalContext.current // Get context inside the composable
    val monthlyBalance = monthlyIncome - monthlyExpenses // Calculate balance
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Finanzas Personales", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
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
                    onClick = onCategoriesClick
                )
                
                // Transactions
                DashboardActionItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Transactions",
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(context, TransactionListActivity::class.java)) }
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
                    onClick = { context.startActivity(Intent(context, ProvidersActivity::class.java)) }
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