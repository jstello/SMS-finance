package com.example.finanzaspersonales.ui.categories

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.R
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.data.repository.ProviderStat
import com.example.finanzaspersonales.ui.sms.SmsPermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

/**
 * Activity for the Categories feature
 */
@AndroidEntryPoint
class CategoriesActivity : ComponentActivity() {
    
    // Inject ViewModel using Hilt
    private val viewModel: CategoriesViewModel by viewModels()
    
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
                // Replace CategoriesNavHost with CategoriesApp, pass the injected viewModel
                // You'll need to manage the navState within CategoriesApp or lift it
                Surface(modifier = Modifier.fillMaxSize()) { // Keep Surface or add as needed
                    // Simple state for now, could be managed by NavController later
                    var navState by remember { mutableStateOf<CategoriesNavState>(CategoriesNavState.Categories) }
                    
                    CategoriesApp(
                        viewModel = viewModel, // Pass the Hilt-injected ViewModel
                        navState = navState,
                        onNavStateChange = { navState = it },
                        onBack = { finish() } // Or handle back navigation appropriately
                    )
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
        // Use fully qualified name to avoid ambiguity
        val intent = Intent(this, com.example.finanzaspersonales.ui.sms.SmsPermissionActivity::class.java)
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
fun CategoriesApp(
    viewModel: CategoriesViewModel, // Accept ViewModel as parameter
    navState: CategoriesNavState,
    onNavStateChange: (CategoriesNavState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Use the passed-in viewModel instead
    val categoriesViewModel = viewModel 
    
    // Handle back press within CategoriesApp
    BackHandler(enabled = navState != CategoriesNavState.Categories) {
        Log.d("NAV_BACK", "Back handled within CategoriesApp, going to Categories list.")
        onNavStateChange(CategoriesNavState.Categories)
    }

    // Handle navigation state using the passed parameter
    when (val currentState = navState) {
        is CategoriesNavState.Categories -> {
            CategoriesScreen(
                viewModel = categoriesViewModel,
                onCategoryClick = { category -> 
                    Log.d("NAV", "Navigating to detail for category: ${category.name}")
                    onNavStateChange(CategoriesNavState.CategoryDetail(category))
                 },
                onAddCategory = { 
                    Log.d("NAV", "Navigating to add category screen")
                    onNavStateChange(CategoriesNavState.CategoryEdit())
                 },
                onBack = onBack
            )
        }
        is CategoriesNavState.CategoryDetail -> {
            CategoryDetailScreen(
                category = currentState.category,
                viewModel = categoriesViewModel,
                onBack = {
                    Log.d("NAV_BACK", "Explicit back navigation from Detail screen.")
                    onNavStateChange(CategoriesNavState.Categories)
                 },
                onTransactionClick = { transaction ->
                    Log.d("NAV", "Navigating to transaction detail for ID: ${transaction.id}")
                    onNavStateChange(CategoriesNavState.TransactionDetail(transaction))
                 }
            )
        }
        is CategoriesNavState.TransactionDetail -> {
            TransactionDetailScreen(
                viewModel = categoriesViewModel,
                transaction = currentState.transaction,
                onBack = { 
                    Log.d("NAV_BACK", "Back from TransactionDetailScreen.")
                    onNavStateChange(CategoriesNavState.Categories) // Go back to category list for now
                },
                onCategorySelected = { transactionData, selectedCategory ->
                    Log.d("NAV_CAT_SELECT", "Category selected in TransactionDetail: ${selectedCategory.name}, navigating back.")
                    // ViewModel call is likely handled within TransactionDetailScreen
                    // Just handle navigation back here
                    onNavStateChange(CategoriesNavState.Categories)
                }
            )
        }
        is CategoriesNavState.CategoryEdit -> {
            CategoryEditScreen(
                viewModel = categoriesViewModel,
                category = currentState.category,
                onSave = { category ->
                    Log.d("NAV_SAVE", "Category saved/edited, navigating back to list.")
                    categoriesViewModel.saveCategory(category)
                    onNavStateChange(CategoriesNavState.Categories) 
                },
                onBack = {
                    Log.d("NAV_CANCEL", "Category edit cancelled, navigating back to list via onBack.")
                    onNavStateChange(CategoriesNavState.Categories) 
                }
            )
        }
    }
}