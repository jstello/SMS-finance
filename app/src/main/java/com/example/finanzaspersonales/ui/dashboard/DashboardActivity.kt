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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.finanzaspersonales.ui.transaction_list.TransactionListActivity
import kotlin.math.abs
import kotlin.math.round
import com.example.finanzaspersonales.ui.providers.ProvidersActivity
import com.example.finanzaspersonales.ui.add_transaction.AddTransactionActivity
import androidx.compose.ui.platform.LocalContext
import com.example.finanzaspersonales.ui.settings.SettingsActivity
import com.example.finanzaspersonales.ui.raw_sms_list.RawSmsListActivity
import androidx.compose.material3.HorizontalDivider
import com.example.finanzaspersonales.ui.debug.TransactionDebugActivity
import com.example.finanzaspersonales.ui.debug.SpendingInsightsTestActivity
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.finanzaspersonales.ui.visualizations.VisualizationsActivity
import com.example.finanzaspersonales.ui.stats.StatsActivity

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
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val windowSizeClass = calculateWindowSizeClass(this@DashboardActivity)
                    
                    ExpressiveDashboardScreen(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                        onNavigateToCategories = { navigateToCategories() },
                        onNavigateToTransactions = { navigateToTransactions() },
                        onNavigateToProviders = { navigateToProviders() },
                        onNavigateToVisualizations = { navigateToVisualizations() },
                        onNavigateToAddTransaction = { navigateToAddTransaction() },
                        onNavigateToSettings = { navigateToSettings() },
                        onNavigateToDebug = { navigateToDebug() },
                        onNavigateToSpendingInsightsTest = { navigateToSpendingInsightsTest() },
                        onNavigateToStats = { navigateToStats() }
                    )
                }
            }
        }

        // Check permissions when activity is created
        checkPermissionsAndLoadData()
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
        val intent = Intent(this, CategoriesActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTransactions() {
        startActivity(Intent(this, TransactionListActivity::class.java))
    }

    private fun navigateToProviders() {
        startActivity(Intent(this, ProvidersActivity::class.java))
    }

    private fun navigateToAddTransaction() {
        val intent = Intent(this, AddTransactionActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun navigateToDebug() {
        startActivity(Intent(this, TransactionDebugActivity::class.java))
    }

    private fun navigateToSpendingInsightsTest() {
        startActivity(Intent(this, SpendingInsightsTestActivity::class.java))
    }

    private fun navigateToVisualizations() {
        val intent = Intent(this, VisualizationsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRawSmsList() {
        startActivity(Intent(this, RawSmsListActivity::class.java))
    }

    private fun navigateToStats() {
        startActivity(Intent(this, StatsActivity::class.java))
    }
} 