package com.example.finanzaspersonales.ui.raw_sms_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.finanzaspersonales.domain.util.DateTimeUtils
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.ui.raw_sms_list.RawSmsListViewModel
import com.example.finanzaspersonales.ui.raw_sms_list.SmsSortField
import com.example.finanzaspersonales.ui.raw_sms_list.SortOrder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawSmsListScreen(
    navController: NavController,
    viewModel: RawSmsListViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val displayedSmsMessages by viewModel.displayedSmsMessages.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedSms by remember { mutableStateOf<SmsMessage?>(null) }

    if (showDialog && selectedSms != null) {
        SmsDetailDialog(smsMessage = selectedSms!!, onDismiss = { showDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raw SMS Transactions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRawSmsMessages(forceRefresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) {
        paddingValues -> 
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
        ) {
            TextField(
                value = searchTerm,
                onValueChange = { viewModel.onSearchTermChanged(it) },
                label = { Text("Search Provider or Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortableHeader(
                    text = "Date",
                    field = SmsSortField.DATE,
                    currentSortField = sortField,
                    currentSortOrder = sortOrder,
                    onClick = { viewModel.onSortChanged(SmsSortField.DATE) },
                    modifier = Modifier.weight(0.4f) // Adjusted weight
                )
                SortableHeader(
                    text = "Amount",
                    field = SmsSortField.AMOUNT,
                    currentSortField = sortField,
                    currentSortOrder = sortOrder,
                    onClick = { viewModel.onSortChanged(SmsSortField.AMOUNT) },
                    modifier = Modifier.weight(0.3f) // Adjusted weight
                )
                SortableHeader(
                    text = "Provider",
                    field = SmsSortField.PROVIDER,
                    currentSortField = sortField,
                    currentSortOrder = sortOrder,
                    onClick = { viewModel.onSortChanged(SmsSortField.PROVIDER) },
                    modifier = Modifier.weight(0.3f) // Adjusted weight
                )
            }
            HorizontalDivider()

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayedSmsMessages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchTerm.isBlank()) "No SMS messages found" else "No matching SMS found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedSmsMessages) { smsMessage ->
                        SmsListItem(
                            smsMessage = smsMessage,
                            onClick = {
                                selectedSms = smsMessage
                                showDialog = true
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun SortableHeader(
    text: String,
    field: SmsSortField,
    currentSortField: SmsSortField,
    currentSortOrder: SortOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, fontWeight = if (currentSortField == field) FontWeight.Bold else FontWeight.Normal)
        if (currentSortField == field) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (currentSortOrder == SortOrder.ASCENDING) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (currentSortOrder == SortOrder.ASCENDING) "Ascending" else "Descending",
                modifier = Modifier.height(16.dp)
            )
        }
    }
}

@Composable
fun SmsListItem(smsMessage: SmsMessage, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateTimeUtils.formatDateTime(smsMessage.dateTime, "dd MMM yy HH:mm"), // Using shorter year
            modifier = Modifier.weight(0.4f), // Adjusted weight
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = smsMessage.amount ?: "N/A",
            modifier = Modifier.weight(0.3f).padding(start = 4.dp), // Adjusted weight
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = smsMessage.provider ?: smsMessage.address ?: "Unknown Sender",
            modifier = Modifier.weight(0.3f).padding(start = 4.dp), // Adjusted weight
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmsDetailDialog(smsMessage: SmsMessage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "SMS Detail from: ${smsMessage.address}")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) { // Make content scrollable
                Text(text = smsMessage.body)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
} 