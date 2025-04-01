package com.example.finanzaspersonales.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class DashboardViewModel(
    private val transactionRepository: TransactionRepository
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
    
    /**
     * Load data for the dashboard
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Initialize transactions with saved categories
                transactionRepository.initializeTransactions()
                
                // Refresh SMS data
                transactionRepository.refreshSmsData()
                
                // Get all transactions
                val allTransactions = transactionRepository.getTransactions()
                
                // Get current month's transactions
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
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
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Format a currency amount for display
     */
    fun formatCurrency(amount: Float): String {
        return "$ %.2f".format(amount)
    }
} 