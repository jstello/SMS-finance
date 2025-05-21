package com.example.finanzaspersonales.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sharedPrefsManager: SharedPrefsManager
) : ViewModel() {
    
    // Current month's spending summary
    private val _monthlyExpenses = MutableStateFlow(0.0f)
    val monthlyExpenses: StateFlow<Float> = _monthlyExpenses.asStateFlow()
    
    private val _monthlyIncome = MutableStateFlow(0.0f)
    val monthlyIncome: StateFlow<Float> = _monthlyIncome.asStateFlow()
    
    // Recent transactions
    private val _recentTransactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val recentTransactions: StateFlow<List<TransactionData>> = _recentTransactions.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // --- Add Sync State ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()
    // ---------------------
    
    /**
     * Checks if initial sync is needed and performs it.
     * Should be called after successful login.
     */
    fun checkAndPerformInitialSync(userId: String) {
        if (sharedPrefsManager.hasCompletedInitialSync(userId)) {
            Log.d("SYNC", "Initial sync already completed for user $userId")
            // Sync already done, load regular data
            loadDashboardData()
            return
        }
        
        Log.d("SYNC", "Initial sync needed for user $userId. Starting...")
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            try {
                // Calculate start date for sync (e.g., first day of current month)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val syncStartDateMillis = calendar.timeInMillis
                
                // Perform Category Sync
                val categoryResult = categoryRepository.performInitialCategorySync(userId)
                if (categoryResult.isFailure) {
                    throw categoryResult.exceptionOrNull() ?: Exception("Unknown category sync error")
                }
                 Log.d("SYNC", "Category sync successful for user $userId")
                 
                 // Perform Transaction Sync
                 // Important: Ensure local data (cachedTransactions) is populated before syncing
                 // Might need to call refreshSmsData first if cache is empty?
                 // transactionRepository.refreshSmsData(1) // Example: load last month
                 
                 val transactionResult = transactionRepository.performInitialTransactionSync(userId, syncStartDateMillis)
                 if (transactionResult.isFailure) {
                     throw transactionResult.exceptionOrNull() ?: Exception("Unknown transaction sync error")
                 }
                 Log.d("SYNC", "Transaction sync successful for user $userId")
                 
                // Mark sync as complete
                sharedPrefsManager.markInitialSyncComplete(userId)
                Log.d("SYNC", "Initial sync marked complete for user $userId")
                
                // Load regular dashboard data after successful sync
                loadDashboardData()
                
            } catch (e: Exception) {
                Log.e("SYNC", "Initial sync failed for user $userId", e)
                _syncError.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Load data for the dashboard
     */
    fun loadDashboardData() {
        viewModelScope.launch {
             // Don't show main loading indicator if sync indicator is active
            if (!_isSyncing.value) {
                _isLoading.value = true
            }
            
            try {
                // Get all transactions, forcing a refresh from remote and reprocessing SMS
                // This single call should handle fetching the latest data from all sources.
                Log.d("DASHBOARD_LOAD", "Loading all transactions with forceRefresh=true")
                val allTransactions = transactionRepository.getTransactions(forceRefresh = true)
                Log.d("DASHBOARD_LOAD", "Loaded ${allTransactions.size} transactions after force refresh.")
                
                // Get current month's transactions
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)
                
                val currentMonthTransactions = transactionRepository.filterTransactions(
                    transactions = allTransactions,
                    month = currentMonth,
                    year = currentYear,
                    isIncome = null
                )
                
                // Calculate expenses and income
                var expenseSum = 0.0f
                var incomeSum = 0.0f
                
                currentMonthTransactions.forEach { transaction ->
                    if (transaction.isIncome) {
                        incomeSum += transaction.amount
                    } else {
                        expenseSum += transaction.amount
                    }
                }
                
                _monthlyExpenses.value = expenseSum
                _monthlyIncome.value = incomeSum
                
                // Get recent transactions (last 5)
                _recentTransactions.value = allTransactions
                    .sortedByDescending { it.date }
                    .take(5)
                
                Log.d("DASHBOARD_LOAD", "Dashboard data loaded successfully. Income: ${_monthlyIncome.value}, Expenses: ${_monthlyExpenses.value}, Recent: ${_recentTransactions.value.size}")
                
                // Log database contents
                logDatabaseContents()
                
            } catch (e: Exception) {
                Log.e("DASHBOARD_LOAD", "Error loading dashboard data", e)
                // Handle error - e.g., clear data or show error message
                _monthlyExpenses.value = 0.0f
                _monthlyIncome.value = 0.0f
                _recentTransactions.value = emptyList()
            } finally {
                 // Don't hide loading if sync indicator is active
                if (!_isSyncing.value) {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Format a currency amount for display
     */
    fun formatCurrency(amount: Float): String {
        return "$ %.2f".format(amount)
    }
    
    /**
     * Logs the current counts of entities in the Room database.
     */
    private fun logDatabaseContents() {
        viewModelScope.launch {
            try {
                val transactionCount = transactionRepository.getTransactionCount()
                val categoryCount = categoryRepository.getCategoryCount()
                Log.d("DATABASE_LOG", "Current Database Counts: Transactions = $transactionCount, Categories = $categoryCount")
            } catch (e: Exception) {
                Log.e("DATABASE_LOG", "Error fetching database counts for logging", e)
            }
        }
    }
} 