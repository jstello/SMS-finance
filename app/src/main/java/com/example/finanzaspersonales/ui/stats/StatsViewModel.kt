package com.example.finanzaspersonales.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** UI state for stats screen */
data class StatsUiState(
    val count: Int = 0,
    val firstDate: Date? = null,
    val lastDate: Date? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val count = transactionRepository.getTransactionCount()
            val first = transactionRepository.getFirstTransactionDate()
            val last = transactionRepository.getLastTransactionDate()
            _uiState.value = StatsUiState(
                count = count,
                firstDate = first,
                lastDate = last
            )
        }
    }
} 