package com.example.finanzaspersonales.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.local.SharedPrefsManager

/**
 * Factory to create the DashboardViewModel with required dependencies
 */
class DashboardViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sharedPrefsManager: SharedPrefsManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(transactionRepository, categoryRepository, sharedPrefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 