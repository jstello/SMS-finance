package com.example.finanzaspersonales.ui.visualizations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.usecase.GetSpendingHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class VisualizationViewModel @Inject constructor(
    private val getSpendingHistoryUseCase: GetSpendingHistoryUseCase
) : ViewModel() {

    private val _cumulativeSpendingCurrentMonth = MutableStateFlow<List<Float>>(emptyList())
    val cumulativeSpendingCurrentMonth: StateFlow<List<Float>> = _cumulativeSpendingCurrentMonth.asStateFlow()

    private val _cumulativeSpendingPreviousMonth = MutableStateFlow<List<Float>>(emptyList())
    val cumulativeSpendingPreviousMonth: StateFlow<List<Float>> = _cumulativeSpendingPreviousMonth.asStateFlow()

    private val _cumulativeSpendingAvg6Months = MutableStateFlow<List<Float>>(emptyList())
    val cumulativeSpendingAvg6Months: StateFlow<List<Float>> = _cumulativeSpendingAvg6Months.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSpendingData()
    }

    private fun loadSpendingData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val (startDate, endDate) = getSevenMonthsDateRange()

            getSpendingHistoryUseCase(startDate, endDate)
                .catch { e ->
                    _error.value = "Error fetching spending data: ${e.message}"
                    _isLoading.value = false
                }
                .collect { transactions ->
                    processTransactions(transactions)
                    _isLoading.value = false
                }
        }
    }

    private fun processTransactions(transactions: List<TransactionData>) {
        val spendingTransactions = transactions.filter { !it.isIncome }

        // Current Month cumulative up to today
        val nowCal = Calendar.getInstance()
        val currentMonth = nowCal.get(Calendar.MONTH)
        val currentYear = nowCal.get(Calendar.YEAR)
        _cumulativeSpendingCurrentMonth.value = calculateCumulativeSpendingForMonth(spendingTransactions, currentMonth, currentYear)

        // Previous Month cumulative for full month
        val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevMonth = prevCal.get(Calendar.MONTH)
        val prevYear = prevCal.get(Calendar.YEAR)
        _cumulativeSpendingPreviousMonth.value = calculateCumulativeSpendingForMonth(spendingTransactions, prevMonth, prevYear)

        // 6-Month Average: last 6 full months (excluding current)
        val last6MonthsCumulativeSpending = (1..6).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
            calculateCumulativeSpendingForMonth(spendingTransactions, cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        }

        if (last6MonthsCumulativeSpending.isNotEmpty()) {
            val maxDays = last6MonthsCumulativeSpending.maxOf { it.size }
            val averagedList = (0 until maxDays).map { dayIndex ->
                // For each month, use the cumulative value or carry forward last
                val dayValues = last6MonthsCumulativeSpending.map { series ->
                    series.getOrNull(dayIndex) ?: series.lastOrNull() ?: 0f
                }
                dayValues.average().toFloat()
            }
            _cumulativeSpendingAvg6Months.value = averagedList
        }
    }

    private fun calculateCumulativeSpendingForMonth(transactions: List<TransactionData>, month: Int, year: Int): List<Float> {
        val calendar = Calendar.getInstance()
        val transactionsInMonth = transactions.filter {
            calendar.time = it.date
            calendar.get(Calendar.MONTH) == month && calendar.get(Calendar.YEAR) == year
        }

        if (transactionsInMonth.isEmpty()) return emptyList()

        val todayCalendar = Calendar.getInstance()
        val isCurrentMonthAndYear = year == todayCalendar.get(Calendar.YEAR) && month == todayCalendar.get(Calendar.MONTH)

        val daysToProcess = if (isCurrentMonthAndYear) {
            todayCalendar.get(Calendar.DAY_OF_MONTH)
        } else {
            // For past months, get the total days in that month
            Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        // We only need an array for the days we are going to process
        val dailySpending = FloatArray(daysToProcess)

        transactionsInMonth.forEach {
            calendar.time = it.date
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH) - 1 // 0-indexed
            if (dayOfMonth < daysToProcess) {
                dailySpending[dayOfMonth] += it.amount
            }
        }

        val cumulativeSpending = mutableListOf<Float>()
        var sum = 0f
        for (spending in dailySpending) {
            sum += spending
            cumulativeSpending.add(sum)
        }
        return cumulativeSpending
    }

    private fun getSevenMonthsDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        // Read data for the past 12 months
        calendar.add(Calendar.MONTH, -12)
        val startDate = calendar.time
        return startDate to endDate
    }
} 