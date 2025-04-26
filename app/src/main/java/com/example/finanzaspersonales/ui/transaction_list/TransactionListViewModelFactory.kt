package com.example.finanzaspersonales.ui.transaction_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.CategoryRepository
import android.os.Bundle

class TransactionListViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val defaultArgs: Bundle?
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(TransactionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionListViewModel(
                transactionRepository,
                categoryRepository,
                handle
            ) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
} 