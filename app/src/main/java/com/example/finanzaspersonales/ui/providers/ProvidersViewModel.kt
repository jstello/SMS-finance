package com.example.finanzaspersonales.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.repository.ProviderStat
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Calendar
import java.util.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<List<ProviderStat>>(emptyList())
    val stats: StateFlow<List<ProviderStat>> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Loads provider spending statistics for a given date range.
     *
     * @param from Start timestamp (inclusive).
     * @param to End timestamp (inclusive).
     */
    fun loadStats(from: Long, to: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("PROVIDERS_VM", "-> loadStats called. From: $from, To: $to")
            try {
                val result = transactionRepository.getProviderStats(from, to)
                _stats.value = result
                Log.d("PROVIDERS_VM", "   Successfully loaded ${result.size} provider stats.")
            } catch (e: Exception) {
                Log.e("PROVIDERS_VM", "   Error loading provider stats", e)
                _error.value = "Failed to load provider statistics: ${e.localizedMessage}"
                _stats.value = emptyList() // Clear stats on error
            } finally {
                _isLoading.value = false
                Log.d("PROVIDERS_VM", "<- loadStats finished.")
            }
        }
    }

    /**
     * Gets the start timestamp for the current year (January 1st, 00:00:00).
     */
    fun getStartOfYearTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
     /**
     * Gets the start timestamp for the current month (1st day, 00:00:00).
     */
    fun getStartOfMonthTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
} 