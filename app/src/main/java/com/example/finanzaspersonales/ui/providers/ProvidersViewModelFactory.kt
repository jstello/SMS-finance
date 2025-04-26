package com.example.finanzaspersonales.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finanzaspersonales.data.repository.TransactionRepository

class ProvidersViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProvidersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProvidersViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 