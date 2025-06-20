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

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

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

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    init {
        loadTransactions()
    }

    fun setQueryType(type: QueryType) {
        _queryType.value = type
        loadTransactions()
    }

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.DESCENDING) SortDirection.ASCENDING else SortDirection.DESCENDING
        loadTransactions()
    }

    private fun loadTransactions() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val allTransactions = transactionRepository.getTransactions(forceRefresh = false)
                val sortedTransactions = when (_sortDirection.value) {
                    SortDirection.ASCENDING -> allTransactions.sortedBy { it.date }
                    SortDirection.DESCENDING -> allTransactions.sortedByDescending { it.date }
                }

                _transactions.value = when (_queryType.value) {
                    QueryType.ALL -> sortedTransactions.take(50)
                    QueryType.THIS_MONTH -> {
                        val calendar = Calendar.getInstance()
                        val currentMonth = calendar.get(Calendar.MONTH)
                        val currentYear = calendar.get(Calendar.YEAR)

                        sortedTransactions.filter { transaction ->
                            val cal = Calendar.getInstance().apply { time = transaction.date }
                            cal.get(Calendar.MONTH) == currentMonth &&
                                    cal.get(Calendar.YEAR) == currentYear
                        }
                    }
                    QueryType.INCOME -> sortedTransactions
                        .filter { it.isIncome }
                        .take(50)
                    QueryType.EXPENSES -> sortedTransactions
                        .filter { !it.isIncome }
                        .take(50)
                    QueryType.UNCATEGORIZED -> sortedTransactions
                        .filter { it.categoryId.isNullOrEmpty() }
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
