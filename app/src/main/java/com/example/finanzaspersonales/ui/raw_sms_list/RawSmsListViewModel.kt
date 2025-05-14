package com.example.finanzaspersonales.ui.raw_sms_list

import androidx.lifecycle.ViewModel
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.model.SmsMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

enum class SmsSortField {
    DATE, AMOUNT, PROVIDER
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

@HiltViewModel
class RawSmsListViewModel @Inject constructor(
    private val smsDataSource: SmsDataSource
) : ViewModel() {

    private val _rawSmsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    // val rawSmsMessages: StateFlow<List<SmsMessage>> = _rawSmsMessages // Not directly exposed as per instructions

    private val _displayedSmsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val displayedSmsMessages: StateFlow<List<SmsMessage>> = _displayedSmsMessages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm

    private val _sortField = MutableStateFlow(SmsSortField.DATE) // Default sort
    val sortField: StateFlow<SmsSortField> = _sortField

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING) // Default order
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    init {
        loadRawSmsMessages()
    }

    fun loadRawSmsMessages(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            // Using a large number for maxResults as per instructions, limitToRecentMonths = 0 means all time
            val messages = smsDataSource.readSmsMessages(limitToRecentMonths = 0, maxResults = 1000)
            _rawSmsMessages.value = messages
            applyFiltersAndSorting()
            _isLoading.value = false
        }
    }

    fun onSearchTermChanged(newTerm: String) {
        _searchTerm.value = newTerm
        applyFiltersAndSorting()
    }

    fun onSortChanged(newField: SmsSortField) {
        if (_sortField.value == newField) {
            _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        } else {
            _sortField.value = newField
            _sortOrder.value = SortOrder.ASCENDING // Default to ascending when field changes
        }
        applyFiltersAndSorting()
    }

    private fun applyFiltersAndSorting() {
        val searchTerm = _searchTerm.value.lowercase(Locale.getDefault())
        val filteredList = if (searchTerm.isBlank()) {
            _rawSmsMessages.value
        } else {
            _rawSmsMessages.value.filter {
                (it.provider?.lowercase(Locale.getDefault())?.contains(searchTerm) == true) ||
                (it.address.lowercase(Locale.getDefault()).contains(searchTerm)) ||
                (it.amount?.lowercase(Locale.getDefault())?.contains(searchTerm) == true)
            }
        }

        val sortedList = when (_sortField.value) {
            SmsSortField.DATE -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.dateTime }
                } else {
                    filteredList.sortedByDescending { it.dateTime }
                }
            }
            SmsSortField.AMOUNT -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.numericAmount }
                } else {
                    filteredList.sortedByDescending { it.numericAmount }
                }
            }
            SmsSortField.PROVIDER -> {
                if (_sortOrder.value == SortOrder.ASCENDING) {
                    filteredList.sortedBy { (it.provider ?: it.address)?.lowercase(Locale.getDefault()) }
                } else {
                    filteredList.sortedByDescending { (it.provider ?: it.address)?.lowercase(Locale.getDefault()) }
                }
            }
        }
        _displayedSmsMessages.value = sortedList
    }
} 