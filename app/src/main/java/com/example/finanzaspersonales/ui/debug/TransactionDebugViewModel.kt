package com.example.finanzaspersonales.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TransactionDebugViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val transactions: StateFlow<List<TransactionData>> = _transactions.asStateFlow()

    private val _queryType = MutableStateFlow(QueryType.ALL)
    val queryType: StateFlow<QueryType> = _queryType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTransactions()
    }

    fun setQueryType(type: QueryType) {
        _queryType.value = type
        loadTransactions()
    }

    private fun loadTransactions() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val allTransactions = transactionRepository.getTransactions(forceRefresh = false)
                _transactions.value = when (_queryType.value) {
                    QueryType.ALL -> allTransactions.sortedByDescending { it.date }.take(50)
                    QueryType.THIS_MONTH -> {
                        val calendar = Calendar.getInstance()
                        val currentMonth = calendar.get(Calendar.MONTH)
                        val currentYear = calendar.get(Calendar.YEAR)
                        
                        allTransactions.filter { transaction ->
                            val cal = Calendar.getInstance().apply { time = transaction.date }
                            cal.get(Calendar.MONTH) == currentMonth && 
                            cal.get(Calendar.YEAR) == currentYear
                        }.sortedByDescending { it.date }
                    }
                    QueryType.INCOME -> allTransactions
                        .filter { it.isIncome }
                        .sortedByDescending { it.date }
                        .take(50)
                    QueryType.EXPENSES -> allTransactions
                        .filter { !it.isIncome }
                        .sortedByDescending { it.date }
                        .take(50)
                    QueryType.UNCATEGORIZED -> allTransactions
                        .filter { it.categoryId.isNullOrEmpty() }
                        .sortedByDescending { it.date }
                        .take(50)
                }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
