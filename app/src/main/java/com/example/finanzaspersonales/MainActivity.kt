package com.example.finanzaspersonales

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.ui.unit.dp
import java.util.regex.Pattern
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import kotlin.text.Regex
import kotlin.text.RegexOption
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import java.util.Calendar
import java.text.DateFormatSymbols
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.foundation.layout.Box

data class SmsMessage(
    val address: String,
    val body: String,
    val amount: String?,      // Formatted string
    val numericAmount: Float?, // Add this new property
    val dateTime: java.util.Date?  // Add this new property
)

data class TransactionData(
    val date: java.util.Date,
    val amount: Float,
    val isIncome: Boolean,
    val originalMessage: SmsMessage
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanzasPersonalesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SMSReader(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SMSReader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val smsMessages = remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    val showNumericData = remember { mutableStateOf(false) }
    val numericAmounts = remember { mutableStateOf<List<Float>>(emptyList()) }
    val transactions = remember { mutableStateOf<List<TransactionData>>(emptyList()) }
    val searchQuery = remember { mutableStateOf("") }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            smsMessages.value = readFilteredSMS(context)
        }
    }

    // Auto-load messages on first composition
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) == 
            PackageManager.PERMISSION_GRANTED) {
            smsMessages.value = readFilteredSMS(context)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    Column(modifier = modifier) {
        if (showNumericData.value) {
            NumericDataScreen(
                transactions = transactions.value,
                onBack = { showNumericData.value = false }
            )
        } else {
            androidx.compose.material3.AssistChip(
                onClick = {
                    transactions.value = extractTransactionData(smsMessages.value)
                    showNumericData.value = true
                },
                label = { Text("Show Transaction Data") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            )

            // Add search text field
            androidx.compose.material3.TextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                label = { Text("Search messages") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // Update filtered messages calculation
            val filteredMessages = remember(smsMessages.value, searchQuery.value) {
                smsMessages.value.filter { message ->
                    searchQuery.value.isEmpty() || 
                    message.body.contains(searchQuery.value, ignoreCase = true)
                }
            }

            LazyColumn {
                itemsIndexed(filteredMessages) { index, message ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("From: ${message.address}")
                        message.dateTime?.let {
                            Text("Date: ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm").format(it)}")
                        }
                        message.amount?.let {
                            Text("Amount: $it", color = Color.Red)
                        }
                        Text("Message: ${message.body}")
                    }
                    if (index < filteredMessages.size - 1) {
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp
                        )
                    }
                }
                
                // Add empty state
                if (filteredMessages.isEmpty()) {
                    item {
                        Text(
                            "No messages found",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NumericDataScreen(transactions: List<TransactionData>, onBack: () -> Unit) {
    val selectedTransaction = remember { mutableStateOf<TransactionData?>(null) }
    
    if (selectedTransaction.value != null) {
        MessageDetailScreen(
            message = selectedTransaction.value!!.originalMessage,
            onBack = { selectedTransaction.value = null }
        )
    } else {
        // Get current date values for fallback
        val calendar = remember { Calendar.getInstance() }
        val defaultYear = calendar.get(Calendar.YEAR)
        val defaultMonth = calendar.get(Calendar.MONTH) + 1

        // Initialize filters with null (show all data)
        val filterState = remember { mutableStateOf("all") }
        val selectedYear = remember { mutableStateOf<Int?>(null) }
        val selectedMonth = remember { mutableStateOf<Int?>(null) }
        val showYearFilter = remember { mutableStateOf(false) }
        val showMonthFilter = remember { mutableStateOf(false) }

        // Add years calculation back
        val years = remember(transactions) {
            if (transactions.isEmpty()) listOf(defaultYear)
            else transactions.map {
                val cal = Calendar.getInstance().apply { time = it.date }
                cal.get(Calendar.YEAR)
            }.distinct().sortedDescending()
        }

        // Update monthsInYear calculation to use defaultYear as fallback
        val monthsInYear = remember(selectedYear.value) {
            val year = selectedYear.value ?: defaultYear
            transactions.mapNotNull {
                val cal = Calendar.getInstance().apply { time = it.date }
                if (cal.get(Calendar.YEAR) == year) {
                    cal.get(Calendar.MONTH) + 1
                } else null
            }.distinct().sortedDescending().ifEmpty { listOf(defaultMonth) }
        }

        // Safe filter with fallbacks for nulls
        val filteredTransactions = remember(transactions, filterState.value, selectedYear.value, selectedMonth.value) {
            transactions.filter { transaction ->
                val matchesType = when (filterState.value) {
                    "income" -> transaction.isIncome
                    "expense" -> !transaction.isIncome
                    else -> true
                }
                
                val cal = Calendar.getInstance().apply { time = transaction.date }
                
                // Modified section: Only check year/month if selection exists
                val matchesYear = selectedYear.value?.let { cal.get(Calendar.YEAR) == it } ?: true
                val matchesMonth = selectedMonth.value?.let { (cal.get(Calendar.MONTH) + 1) == it } ?: true
                
                matchesType && matchesYear && matchesMonth
            }
        }

        // Add sorting state
        val sortState = remember { mutableStateOf(Pair("date", false)) } // (column, ascending)

        val sortedTransactions = remember(filteredTransactions, sortState.value) {
            when (sortState.value.first) {
                "amount" -> {
                    if (sortState.value.second) {
                        filteredTransactions.sortedBy { it.amount }
                    } else {
                        filteredTransactions.sortedByDescending { it.amount }
                    }
                }
                else -> { // date is default
                    if (sortState.value.second) {
                        filteredTransactions.sortedBy { it.date }
                    } else {
                        filteredTransactions.sortedByDescending { it.date }
                    }
                }
            }
        }

        // Calculate totals
        val (totalIncome, totalExpense) = remember(filteredTransactions) {
            var income = 0f
            var expense = 0f
            filteredTransactions.forEach {
                if (it.isIncome) income += it.amount else expense += it.amount
            }
            Pair(income, expense)
        }
        
        val totalAmount = remember(filteredTransactions) {
            totalIncome - totalExpense
        }

        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.material3.AssistChip(
                onClick = onBack,
                label = { Text("Back to Messages") },
                leadingIcon = {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Year/Month filter row
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Combined filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Year filter with dropdown
                    Box {
                        androidx.compose.material3.AssistChip(
                            onClick = { showYearFilter.value = true },
                            label = { Text(selectedYear.value?.toString() ?: "Year") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        // Year filter dropdown
                        DropdownMenu(
                            expanded = showYearFilter.value,
                            onDismissRequest = { showYearFilter.value = false },
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(text = year.toString()) },
                                    onClick = {
                                        selectedYear.value = year
                                        showYearFilter.value = false
                                    }
                                )
                            }
                        }
                    }

                    // Month filter with dropdown
                    Box {
                        androidx.compose.material3.AssistChip(
                            onClick = { showMonthFilter.value = true },
                            enabled = selectedYear.value != null,
                            label = {
                                Text(
                                    selectedMonth.value?.let { 
                                        DateFormatSymbols().months[it - 1].take(3) 
                                    } ?: "Month"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        // Month filter dropdown
                        DropdownMenu(
                            expanded = showMonthFilter.value,
                            onDismissRequest = { showMonthFilter.value = false },
                        ) {
                            monthsInYear.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(DateFormatSymbols().months[month - 1]) },
                                    onClick = {
                                        selectedMonth.value = month
                                        showMonthFilter.value = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Clear filters
                androidx.compose.material3.IconButton(
                    onClick = {
                        selectedYear.value = null
                        selectedMonth.value = null
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear filters")
                }
            }
            
            // Filter chips
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("All", "Income", "Expense").forEach { filter ->
                    val filterKey = filter.lowercase()
                    androidx.compose.material3.FilterChip(
                        selected = filterKey == filterState.value,
                        onClick = { 
                            filterState.value = filterKey
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        label = { Text(filter) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (filterKey) {
                                "income" -> MaterialTheme.colorScheme.primary
                                "expense" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    )
                }
            }

            // Table header
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.clickable {
                        sortState.value = if (sortState.value.first == "date") {
                            Pair("date", !sortState.value.second)
                        } else {
                            Pair("date", false)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Date/Time", fontWeight = FontWeight.Bold)
                        SortIndicator(
                            visible = sortState.value.first == "date",
                            ascending = sortState.value.second
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.clickable {
                        sortState.value = if (sortState.value.first == "amount") {
                            Pair("amount", !sortState.value.second)
                        } else {
                            Pair("amount", false)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Amount (COP)", fontWeight = FontWeight.Bold)
                        SortIndicator(
                            visible = sortState.value.first == "amount",
                            ascending = sortState.value.second
                        )
                    }
                }
            }
            
            Divider(color = Color.Gray, thickness = 1.dp)
            
            LazyColumn(modifier = Modifier
                .padding(top = 8.dp)
                .weight(1f)) {
                itemsIndexed(sortedTransactions) { index, transaction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { selectedTransaction.value = transaction },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(java.text.SimpleDateFormat("dd MMM yyyy HH:mm").format(transaction.date))
                        Text(
                            text = "$${"%,.2f".format(transaction.amount)}",
                            color = when {
                                "all" == "all" -> 
                                    if (transaction.isIncome) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    if (index < sortedTransactions.size - 1) {
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Total display
            Column(modifier = Modifier.padding(8.dp)) {
                when (filterState.value) {
                    "income" -> {
                        TotalRow(label = "Total Income:", amount = totalIncome, color = MaterialTheme.colorScheme.primary)
                    }
                    "expense" -> {
                        TotalRow(label = "Total Expense:", amount = totalExpense, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        TotalRow(label = "Total Income:", amount = totalIncome, color = MaterialTheme.colorScheme.primary)
                        TotalRow(label = "Total Expense:", amount = totalExpense, color = MaterialTheme.colorScheme.error)
                        TotalRow(label = "Net Total:", amount = totalIncome - totalExpense, color = Color.DarkGray)
                    }
                }
            }

            // Clear button functionality
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Button(
                    onClick = {
                        selectedYear.value = defaultYear
                        selectedMonth.value = defaultMonth
                    }
                ) {
                    Text("Reset to Current")
                }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, amount: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(
            text = "COP ${"%,.2f".format(amount)}",
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SortIndicator(visible: Boolean, ascending: Boolean) {
    if (visible) {
        Text(
            text = if (ascending) "▲" else "▼",
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun readFilteredSMS(context: android.content.Context): List<SmsMessage> {
    val messages = mutableListOf<SmsMessage>()
    val uri: Uri = Telephony.Sms.CONTENT_URI
    val projection = arrayOf(
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
    )
    
    val cursor: Cursor? = context.contentResolver.query(
        uri,
        projection,
        "${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.BODY} LIKE ?",
        arrayOf("%Bancolombia%", "%Nequi%"),
        "${Telephony.Sms.DATE} DESC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
            val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
            val amount = extractAmountFromBody(body)
            messages.add(SmsMessage(
                address = address,
                body = body,
                amount = amount,
                numericAmount = parseToFloat(amount),
                dateTime = extractDateTimeFromBody(body)
            ))
        }
    }
    return messages
}

private fun extractAmountFromBody(body: String): String? {
    val pattern = Pattern.compile("""(\$|COP)\s*((\d{1,3}(?:[.,]\d{3})*|\d+))(?:([.,])(\d{2}))?""")
    val matcher = pattern.matcher(body)
    return if (matcher.find()) {
        val currency = matcher.group(1)
        val mainNumber = matcher.group(2).replace("[.,]".toRegex(), "")
        val decimal = matcher.group(5)
        
        when {
            decimal == null -> "$currency$mainNumber"
            decimal == "00" -> "$currency$mainNumber"
            else -> "$currency$mainNumber.$decimal"
        }
    } else null
}

private fun parseToFloat(amount: String?): Float? {
    return amount?.replace("^(\\\$|COP)".toRegex(), "")
        ?.replace(",", ".")
        ?.toFloatOrNull()
}

private fun extractNumericAmounts(messages: List<SmsMessage>): List<Float> {
    return messages.mapNotNull { it.numericAmount }
}

private fun extractDateTimeFromBody(body: String): java.util.Date? {
    // Updated pattern to handle both date-time formats
    val pattern = Pattern.compile("""(\d{2}/\d{2}/\d{4}).*?(\d{2}:\d{2}(?::\d{2})?)|(\d{2}:\d{2}(?::\d{2})?).*?(\d{2}/\d{2}/\d{4})""")
    val matcher = pattern.matcher(body)
    
    return if (matcher.find()) {
        when {
            // Case 1: Date first (group 1) then time (group 2)
            matcher.group(1) != null && matcher.group(2) != null -> 
                parseDateTimeString("${matcher.group(1)} ${matcher.group(2)}")
            
            // Case 2: Time first (group 3) then date (group 4)
            matcher.group(3) != null && matcher.group(4) != null -> 
                parseDateTimeString("${matcher.group(4)} ${matcher.group(3)}")
            
            else -> null
        }
    } else null
}

private fun parseDateTimeString(dateTimeStr: String): java.util.Date? {
    return try {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).parse(dateTimeStr)
    } catch (e: Exception) {
        try {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).parse(dateTimeStr)
        } catch (e: Exception) {
            null
        }
    }
}

private fun extractTransactionData(messages: List<SmsMessage>): List<TransactionData> {
    return messages.mapNotNull { message ->
        if (message.dateTime != null && message.numericAmount != null) {
            TransactionData(
                date = message.dateTime,
                amount = message.numericAmount,
                isIncome = message.body.contains(Regex("(recepc[ií]ón|recibiste)", RegexOption.IGNORE_CASE)),
                originalMessage = message
            )
        } else null
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FinanzasPersonalesTheme {
        SMSReader(Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(message: SmsMessage, onBack: () -> Unit) {
    Scaffold(topBar = {
        androidx.compose.material3.TopAppBar(
            title = { Text("Transaction Details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {
            Text("From: ${message.address}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            message.dateTime?.let {
                Text("Date: ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm").format(it)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            message.amount?.let {
                Text("Amount: $it", color = Color.Red, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Original Message:", style = MaterialTheme.typography.titleSmall)
            Text(message.body, modifier = Modifier.padding(top = 8.dp))
        }
    }
}