package com.example.finanzaspersonales.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Database Stats", style = MaterialTheme.typography.headlineSmall)
        Text("Total Transactions: ${state.count}", modifier = Modifier.padding(top = 8.dp))
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        Text("First Transaction Date: ${state.firstDate?.let { formatter.format(it) } ?: "N/A"}", modifier = Modifier.padding(top = 4.dp))
        Text("Last Transaction Date: ${state.lastDate?.let { formatter.format(it) } ?: "N/A"}", modifier = Modifier.padding(top = 4.dp))
    }
} 