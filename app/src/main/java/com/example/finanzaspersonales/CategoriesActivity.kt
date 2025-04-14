package com.example.finanzaspersonales

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.categories.CategoriesScreen
import com.example.finanzaspersonales.ui.categories.CategoriesViewModel
import com.example.finanzaspersonales.ui.categories.CategoryDetailScreen
import com.example.finanzaspersonales.ui.categories.CategoryEditScreen
import com.example.finanzaspersonales.ui.categories.TransactionDetailScreen
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl

/**
 * Activity for the Categories feature
 */
class CategoriesActivity : ComponentActivity() {
    
    // Permission launcher for SMS read permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("APP_PERMISSIONS", "SMS permission granted")
            // Permission granted, continue app initialization
            initializeAppWithPermissions()
        } else {
            Log.e("APP_PERMISSIONS", "SMS permission denied")
            // Permission denied, show message to user
            Toast.makeText(this, "SMS permission is required for transaction tracking", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == 
                    PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                Log.d("APP_PERMISSIONS", "SMS permission already granted")
                initializeAppWithPermissions()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) -> {
                // Explain why we need permission
                Log.d("APP_PERMISSIONS", "Should show permission rationale")
                Toast.makeText(this, "SMS permission is needed to analyze your transactions", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
            else -> {
                // Request permission directly
                Log.d("APP_PERMISSIONS", "Requesting SMS permission")
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }
    
    private fun initializeAppWithPermissions() {
        // This function will be called once permissions are granted
        // You can put any initialization that requires permissions here
        Log.d("APP_STARTUP", "App initialized with permissions")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("APP_STARTUP", "onCreate() started - Bundle: ${savedInstanceState?.toString()}")
        super.onCreate(savedInstanceState)
        Log.d("APP_STARTUP", "Super.onCreate() completed")
        
        // Add this temporary exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            throwable.printStackTrace()
        }

        // Set content with Compose
        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CategoriesApp(onBack = { finish() })
                }
            }
        }
        
        // Check and request permissions after setting content
        checkAndRequestPermissions()
        
        Log.d("APP_STARTUP", "onCreate() completed successfully")
    }

    override fun onStart() {
        super.onStart()
        Log.d("APP_LIFECYCLE", "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("APP_LIFECYCLE", "onResume() called")
    }
    
    /**
     * Launch the SMS test activity
     */
    private fun launchSmsTestActivity() {
        val intent = Intent(this, com.example.finanzaspersonales.sms.SmsPermissionActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * Create options menu with SMS Test option
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    /**
     * Handle menu item selection
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sms_test -> {
                launchSmsTestActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

/**
 * Navigation state for Categories feature
 */
sealed class CategoriesNavState {
    object Categories : CategoriesNavState()
    data class CategoryDetail(val category: Category) : CategoriesNavState()
    data class TransactionDetail(val transaction: TransactionData) : CategoriesNavState()
    data class CategoryEdit(val category: Category? = null) : CategoriesNavState()
}

/**
 * Main composable for the Categories feature
 */
@Composable
fun CategoriesApp(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // Navigation state
    var navState by remember { mutableStateOf<CategoriesNavState>(CategoriesNavState.Categories) }
    
    val categoriesViewModel: CategoriesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Create repositories and dependencies
                val sharedPrefsManager = SharedPrefsManager(context)
                val smsDataSource = SmsDataSource(context)
                val extractTransactionDataUseCase = ExtractTransactionDataUseCase(context)
                val authRepository = AuthRepositoryImpl() // Instantiate AuthRepository
                
                // Create a "dummy" transaction repository first for the CategoryRepository
                val dummyTransactionRepository = object : TransactionRepository {
                    override suspend fun getAllSmsMessages(): List<SmsMessage> = emptyList()
                    override suspend fun getTransactions(): List<TransactionData> = emptyList()
                    override suspend fun filterTransactions(transactions: List<TransactionData>, year: Int?, month: Int?, isIncome: Boolean?): List<TransactionData> = emptyList()
                    override suspend fun getTransactionById(id: String): TransactionData? = null
                    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> = emptyList()
                    override suspend fun assignCategoryToTransaction(transactionId: String, categoryId: String): Boolean = false
                    override suspend fun refreshSmsData(limitToRecentMonths: Int) { /* Dummy */ }
                    override suspend fun initializeTransactions() { /* Dummy */ }
                    // --- Add Stubs for Firestore/Sync functions ---
                    override suspend fun saveTransactionToFirestore(transaction: TransactionData): Result<Unit> = Result.failure(NotImplementedError("Dummy implementation"))
                    override suspend fun getTransactionsFromFirestore(userId: String): Result<List<TransactionData>> = Result.failure(NotImplementedError("Dummy implementation"))
                    override suspend fun updateTransactionInFirestore(transaction: TransactionData): Result<Unit> = Result.failure(NotImplementedError("Dummy implementation"))
                    override suspend fun deleteTransactionFromFirestore(transactionId: String, userId: String): Result<Unit> = Result.failure(NotImplementedError("Dummy implementation"))
                    override suspend fun performInitialTransactionSync(userId: String, syncStartDate: Long): Result<Unit> = Result.failure(NotImplementedError("Dummy implementation"))
                }
                
                // Create temp CategoryRepository with AuthRepository
                val tempCategoryRepository = CategoryRepositoryImpl(
                    context = context,
                    sharedPrefsManager = sharedPrefsManager,
                    transactionRepository = dummyTransactionRepository,
                    authRepository = authRepository // Pass AuthRepository
                )
                
                val categoryAssignmentUseCase = CategoryAssignmentUseCase(tempCategoryRepository)
                
                // Now we can create the actual repositories with the proper dependencies
                val transactionRepository = TransactionRepositoryImpl(
                    context = context,
                    smsDataSource = smsDataSource,
                    extractTransactionDataUseCase = extractTransactionDataUseCase,
                    categoryAssignmentUseCase = categoryAssignmentUseCase,
                    sharedPrefsManager = sharedPrefsManager
                )
                
                // Replace the temporary repository with the real one, including AuthRepository
                val categoryRepository: CategoryRepository = CategoryRepositoryImpl(
                    context = context,
                    sharedPrefsManager = sharedPrefsManager,
                    transactionRepository = transactionRepository,
                    authRepository = authRepository // Pass AuthRepository
                )
                
                return CategoriesViewModel(
                    categoryRepository = categoryRepository,
                    transactionRepository = transactionRepository
                ) as T
            }
        }
    )
    
    // Handle navigation state
    when (val currentState = navState) {
        is CategoriesNavState.Categories -> {
            CategoriesScreen(
                viewModel = categoriesViewModel,
                onBack = onBack,
                onCategoryClick = { category ->
                    navState = CategoriesNavState.CategoryDetail(category)
                },
                onAddCategory = {
                    navState = CategoriesNavState.CategoryEdit()
                }
            )
        }
        
        is CategoriesNavState.CategoryDetail -> {
            CategoryDetailScreen(
                viewModel = categoriesViewModel,
                category = currentState.category,
                onBack = { navState = CategoriesNavState.Categories },
                onTransactionClick = { transaction ->
                    navState = CategoriesNavState.TransactionDetail(transaction)
                }
            )
        }
        
        is CategoriesNavState.TransactionDetail -> {
            TransactionDetailScreen(
                viewModel = categoriesViewModel,
                transaction = currentState.transaction,
                onBack = { 
                    // Go back to the appropriate screen based on where we came from
                    val previousState = navState
                    if (previousState is CategoriesNavState.CategoryDetail) {
                        navState = previousState
                    } else {
                        navState = CategoriesNavState.Categories
                    }
                },
                onCategorySelected = { transaction, category ->
                    Log.d("CAT_ASSIGN_UI", "-> onCategorySelected triggered. TxID: ${transaction.id}, New Cat: ${category.name} (${category.id})")
                    categoriesViewModel.assignCategoryToTransaction(transaction, category)
                    // Log before navigation
                    Log.d("CAT_ASSIGN_UI", "<- Navigating back to Categories list after triggering VM.")
                    navState = CategoriesNavState.Categories
                }
            )
        }
        
        is CategoriesNavState.CategoryEdit -> {
            CategoryEditScreen(
                viewModel = categoriesViewModel,
                category = currentState.category,
                onBack = { navState = CategoriesNavState.Categories },
                onSave = { category ->
                    categoriesViewModel.saveCategory(category)
                    navState = CategoriesNavState.Categories
                }
            )
        }
    }
} 