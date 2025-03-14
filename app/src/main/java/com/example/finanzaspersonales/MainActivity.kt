package com.example.finanzaspersonales

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
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
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import com.google.gson.Gson
import androidx.compose.runtime.MutableState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect

data class SmsMessage(
    val address: String,
    val body: String,
    val amount: String?,
    val numericAmount: Float?,
    val dateTime: java.util.Date?,
    val detectedAccount: String? = null,
    val sourceAccount: String? = null
)

data class TransactionData(
    val date: java.util.Date,
    val amount: Float,
    val isIncome: Boolean,
    val originalMessage: SmsMessage
)

data class AccountInfo(
    val contactName: String,
    val phoneNumber: String,
    val accountNumber: String,
    val bankName: String
)

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Accounts : Screen("accounts")
    object Settings : Screen("settings")
}

@Composable
fun SettingsScreen() {
    Text("Settings Screen", modifier = Modifier.padding(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDirectoryScreen(
    onBack: () -> Unit,
    accounts: MutableState<List<AccountInfo>>,
    context: android.content.Context
) {
    val showDialog = remember { mutableStateOf(false) }
    val editingAccount = remember { mutableStateOf<AccountInfo?>(null) }
    
    val name = remember { mutableStateOf("") }
    val phone = remember { mutableStateOf("") }
    val account = remember { mutableStateOf("") }
    val bank = remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Account Directory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        editingAccount.value = null
                        name.value = ""
                        phone.value = ""
                        account.value = ""
                        bank.value = ""
                        showDialog.value = true 
                    }) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Add Account")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed<AccountInfo>(accounts.value) { index, acc ->
                    AccountListItem(
                        account = acc,
                        onEdit = {
                            editingAccount.value = acc
                            name.value = acc.contactName
                            phone.value = acc.phoneNumber
                            account.value = acc.accountNumber
                            bank.value = acc.bankName
                            showDialog.value = true
                        },
                        onDelete = {
                            accounts.value = accounts.value.toMutableList().apply { removeAt(index) }
                            saveAccounts(context, accounts.value)
                        }
                    )
                    Divider()
                }
            }
            
            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = { showDialog.value = false },
                    title = { Text(if (editingAccount.value == null) "Add Account" else "Edit Account") },
                    text = {
                        Column {
                            TextField(
                                value = name.value,
                                onValueChange = { name.value = it },
                                label = { Text("Contact Name") }
                            )
                            TextField(
                                value = phone.value,
                                onValueChange = { phone.value = it },
                                label = { Text("Phone Number") }
                            )
                            TextField(
                                value = account.value,
                                onValueChange = { account.value = it },
                                label = { Text("Account Number") }
                            )
                            TextField(
                                value = bank.value,
                                onValueChange = { bank.value = it },
                                label = { Text("Bank Name") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val newAccount = AccountInfo(
                                name.value,
                                phone.value,
                                account.value,
                                bank.value
                            )
                            
                            accounts.value = accounts.value.toMutableList().apply {
                                if (editingAccount.value != null) {
                                    val index = indexOfFirst { it == editingAccount.value }
                                    if (index != -1) this[index] = newAccount
                                } else {
                                    add(newAccount)
                                }
                            }
                            saveAccounts(context, accounts.value)
                            showDialog.value = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDialog.value = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessage) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        // Sender info with date in a row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message.address,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            message.dateTime?.let {
                Text(
                    text = java.text.SimpleDateFormat("dd/MM/yy").format(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        // Message bubble
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = androidx.compose.ui.graphics.Color(0xFF282C34).copy(alpha = 0.8f), // Darker background like WhatsApp
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Add account info if detected
                message.detectedAccount?.let { account ->
                    Text(
                        text = "Account: $account",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                message.sourceAccount?.let { source ->
                    Text(
                        text = "From: $source",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // Message body
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.LightGray, // Lighter text on dark background
                    modifier = Modifier.padding(bottom = if (message.amount != null) 12.dp else 0.dp)
                )
                
                // Amount if present
                message.amount?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = androidx.compose.ui.graphics.Color(0xFFBF2E34), // Red background for amount
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    message: SmsMessage, 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accounts = remember { mutableStateOf(loadAccounts(context)) }
    val matchedAccount = remember(message.address) {
        accounts.value.firstOrNull { it.phoneNumber == message.address }
    }

    Scaffold(topBar = {
        androidx.compose.material3.TopAppBar(
            title = { Text("Transaction Details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {
            
            if (matchedAccount != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Linked Account:", style = MaterialTheme.typography.labelSmall)
                        Text(matchedAccount.contactName, style = MaterialTheme.typography.titleMedium)
                        Text("Account: ${matchedAccount.accountNumber}", style = MaterialTheme.typography.bodyMedium)
                        Text("Bank: ${matchedAccount.bankName}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text("From: ${message.address}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            message.dateTime?.let {
                Text("Date: ${java.text.SimpleDateFormat("dd/MM/yy").format(it)}")
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
            val (amount, numericAmount) = extractAmountFromBody(body)
            val (detectedAccount, sourceAccount) = detectAccountInfo(body)
            
            messages.add(SmsMessage(
                address = address,
                body = body,
                amount = amount,
                numericAmount = numericAmount,
                dateTime = extractDateTimeFromBody(body),
                detectedAccount = detectedAccount,
                sourceAccount = sourceAccount
            ))
        }
    }
    return messages
}

private fun extractAmountFromBody(body: String): Pair<String?, Float?> {
    val pattern = Pattern.compile("""(\$|COP)\s*((\d{1,3}(?:[.,]\d{3})*|\d+))(?:([.,])(\d{2}))?""")
    val matcher = pattern.matcher(body)
    return if (matcher.find()) {
        val currency = matcher.group(1)
        val mainNumber = matcher.group(2).replace("[.,]".toRegex(), "")
        val decimal = matcher.group(5)
        
        when {
            decimal == null -> Pair(currency + mainNumber, null)
            decimal == "00" -> Pair(currency + mainNumber, null)
            else -> Pair(currency + mainNumber + "." + decimal, parseToFloat(currency + mainNumber + "." + decimal))
        }
    } else Pair(null, null)
}

fun parseToFloat(amount: String?): Float? {
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
                isIncome = message.body.contains(
                    Regex("(recepci[óo]n|recibiste|n[óo]mina)", RegexOption.IGNORE_CASE)
                ),
                originalMessage = message
            )
        } else null
    }
}

private fun detectAccountInfo(body: String): Pair<String?, String?> {
    val accountPattern = Pattern.compile(
        "(cuenta|producto|desde)\\s*([*]\\d+)|" +
        "a\\s(.+?)\\sdesde"
    )
    
    val matcher = accountPattern.matcher(body)
    var detectedAccount: String? = null
    var sourceAccount: String? = null
    
    while (matcher.find()) {
        when {
            matcher.group(2) != null -> {
                detectedAccount = matcher.group(2)
                sourceAccount = matcher.group(2)
            }
            matcher.group(3) != null -> {
                detectedAccount = matcher.group(3)
            }
        }
    }
    
    // Special case for Bancolombia format
    val bancolombiaPattern = Pattern.compile(
        "a\\s(.+?)\\sdesde\\sproducto\\s([*]\\d+)"
    )
    val bcMatcher = bancolombiaPattern.matcher(body)
    if (bcMatcher.find()) {
        detectedAccount = bcMatcher.group(1)
        sourceAccount = bcMatcher.group(2)
    }
    
    return Pair(detectedAccount, sourceAccount)
}

private fun saveAccounts(context: android.content.Context, accounts: List<AccountInfo>) {
    val prefs = context.getSharedPreferences("account_prefs", android.content.Context.MODE_PRIVATE)
    val json = Gson().toJson(accounts)
    prefs.edit().putString("accounts", json).apply()
}

private fun loadAccounts(context: android.content.Context): List<AccountInfo> {
    val prefs = context.getSharedPreferences("account_prefs", android.content.Context.MODE_PRIVATE)
    val json = prefs.getString("accounts", null)
    return if (json != null) {
        Gson().fromJson(json, Array<AccountInfo>::class.java).toList()
    } else {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountListItem(
    account: AccountInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.contactName, style = MaterialTheme.typography.titleMedium)
                Text("Phone: ${account.phoneNumber}", style = MaterialTheme.typography.bodySmall)
                Text("Account: ${account.accountNumber}", style = MaterialTheme.typography.bodySmall)
                Text("Bank: ${account.bankName}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete")
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
            text = "COP ${formatAmount(amount)}",
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

private fun formatAmount(amount: Float): String {
    val formatter = java.text.DecimalFormat("#,###")
    return formatter.format(amount)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanzasPersonalesTheme(darkTheme = true) {
                SMSReader(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        }
    }
}

@Composable
fun SMSReader(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val transactions = remember { mutableStateOf<List<TransactionData>>(emptyList()) }
    val selectedYear = remember { mutableStateOf<Int?>(null) }
    val selectedMonth = remember { mutableStateOf<Int?>(null) }
    val filterState = remember { mutableStateOf("all") }
    val sortState = remember { mutableStateOf(Pair("date", false)) }
    
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .height(100.dp)
                    .navigationBarsPadding(),
                tonalElevation = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ) {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Filled.Home, 
                            contentDescription = "Messages",
                            modifier = Modifier.size(36.dp)
                        ) 
                    },
                    label = { },
                    alwaysShowLabel = false,
                    selected = navController.currentDestination?.route == Screen.Home.route,
                    onClick = { navController.navigate(Screen.Home.route) }
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Filled.Analytics, 
                            contentDescription = "Transactions",
                            modifier = Modifier.size(36.dp)
                        ) 
                    },
                    label = { },
                    alwaysShowLabel = false,
                    selected = navController.currentDestination?.route == "transactions_data",
                    onClick = { navController.navigate("transactions_data") }
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Filled.AccountBox, 
                            contentDescription = "Accounts",
                            modifier = Modifier.size(36.dp)
                        ) 
                    },
                    label = { },
                    alwaysShowLabel = false,
                    selected = navController.currentDestination?.route == Screen.Accounts.route,
                    onClick = { navController.navigate(Screen.Accounts.route) }
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Filled.Settings, 
                            contentDescription = "Settings",
                            modifier = Modifier.size(36.dp)
                        ) 
                    },
                    label = { },
                    alwaysShowLabel = false,
                    selected = navController.currentDestination?.route == Screen.Settings.route,
                    onClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        },
        floatingActionButton = {
            if (navController.currentDestination?.route == Screen.Home.route) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        val extractedTransactions = extractTransactionData(
                            readFilteredSMS(context)
                        )
                        transactions.value = extractedTransactions
                        navController.navigate("transactions_data")
                    },
                    containerColor = androidx.compose.ui.graphics.Color(0xFF25D366), // WhatsApp green
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(56.dp) // Larger size for better visibility
                ) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = "Show Transactions",
                        modifier = Modifier.size(26.dp) // Larger icon
                    )
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreenContent(
                    modifier = Modifier.fillMaxSize(),
                    onShowTransactionsData = { extractedTransactions ->
                        transactions.value = extractedTransactions
                        navController.navigate("transactions_data")
                    }
                )
            }
            composable(Screen.Accounts.route) {
                AccountDirectoryScreen(
                    onBack = { navController.popBackStack() },
                    accounts = remember { mutableStateOf<List<AccountInfo>>(loadAccounts(context)) },
                    context = context
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable("transactions_data") {
                NumericDataScreen(
                    transactions = transactions.value,
                    onBack = { navController.popBackStack() },
                    filterState = filterState,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    sortState = sortState
                )
            }
        }
    }
}

@Composable
private fun HomeScreenContent(
    modifier: Modifier = Modifier, 
    onShowTransactionsData: (List<TransactionData>) -> Unit
) {
    val context = LocalContext.current
    val smsMessages = remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    val searchQuery = remember { mutableStateOf("") }
    
    // Sound effects
    val tapSound = remember { MediaPlayer.create(context, R.raw.tap_sound) }
    val selectSound = remember { MediaPlayer.create(context, R.raw.select_sound) }
    
    // Clean up resources when composable is disposed
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            tapSound.release()
            selectSound.release()
        }
    }
    
    // Add message list filter states
    val messageListSelectedYear = remember { mutableStateOf<Int?>(null) }
    val messageListSelectedMonth = remember { mutableStateOf<Int?>(null) }
    val showMessageListYearFilter = remember { mutableStateOf(false) }
    val showMessageListMonthFilter = remember { mutableStateOf(false) }

    // Calculate years from messages
    val years = remember(smsMessages.value) {
        smsMessages.value.mapNotNull { 
            it.dateTime?.let { date ->
                val cal = Calendar.getInstance().apply { time = date }
                cal.get(Calendar.YEAR)
            }
        }.distinct().sortedDescending()
    }

    // Calculate months for selected year
    val monthsInYear = remember(messageListSelectedYear.value) {
        messageListSelectedYear.value?.let { year ->
            smsMessages.value.mapNotNull { 
                it.dateTime?.let { date ->
                    val cal = Calendar.getInstance().apply { time = date }
                    if (cal.get(Calendar.YEAR) == year) {
                        cal.get(Calendar.MONTH) + 1
                    } else null
                }
            }.distinct().sortedDescending()
        } ?: emptyList()
    }

    // Update filtered messages calculation with date filters
    val filteredMessages = remember(smsMessages.value, searchQuery.value, messageListSelectedYear.value, messageListSelectedMonth.value) {
        smsMessages.value.filter { message ->
            val matchesText = searchQuery.value.isEmpty() || 
                message.body.contains(searchQuery.value, ignoreCase = true)
            
            val matchesYear = messageListSelectedYear.value?.let { year ->
                message.dateTime?.let {
                    val cal = Calendar.getInstance().apply { time = it }
                    cal.get(Calendar.YEAR) == year
                } ?: false
            } ?: true
            
            val matchesMonth = messageListSelectedMonth.value?.let { month ->
                message.dateTime?.let {
                    val cal = Calendar.getInstance().apply { time = it }
                    (cal.get(Calendar.MONTH) + 1) == month
                } ?: true
            } ?: true
            
            matchesText && matchesYear && matchesMonth
        }
    }
    
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

    val accounts = remember { mutableStateOf<List<AccountInfo>>(loadAccounts(context)) }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ) {
        // Search bar - WhatsApp style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp) // Much shorter height
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50)) // Fully rounded corners
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                
                if (searchQuery.value.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery.value = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Show hint text when empty
            if (searchQuery.value.isEmpty()) {
                Text(
                    text = "Search messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 36.dp) // Position after the search icon
                )
            }
        }

        // Add filter row before the message list
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Year filter
                Box {
                    androidx.compose.material3.AssistChip(
                        onClick = { 
                            tapSound.seekTo(0)
                            tapSound.start()
                            showMessageListYearFilter.value = true 
                        },
                        label = { Text(messageListSelectedYear.value?.toString() ?: "Year") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showMessageListYearFilter.value,
                        onDismissRequest = { showMessageListYearFilter.value = false }
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    selectSound.seekTo(0)
                                    selectSound.start()
                                    messageListSelectedYear.value = year
                                    messageListSelectedMonth.value = null
                                    showMessageListYearFilter.value = false
                                }
                            )
                        }
                    }
                }

                // Month filter
                Box {
                    androidx.compose.material3.AssistChip(
                        onClick = { 
                            tapSound.seekTo(0)
                            tapSound.start()
                            showMessageListMonthFilter.value = true 
                        },
                        enabled = messageListSelectedYear.value != null,
                        label = {
                            Text(
                                messageListSelectedMonth.value?.let { 
                                    DateFormatSymbols().months[it - 1].take(3) 
                                } ?: "Month"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showMessageListMonthFilter.value,
                        onDismissRequest = { showMessageListMonthFilter.value = false }
                    ) {
                        monthsInYear.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(DateFormatSymbols().months[month - 1]) },
                                onClick = {
                                    selectSound.seekTo(0)
                                    selectSound.start()
                                    messageListSelectedMonth.value = month
                                    showMessageListMonthFilter.value = false
                                }
                            )
                        }
                    }
                }
            }

            // Clear filters
            androidx.compose.material3.IconButton(
                onClick = {
                    tapSound.seekTo(0)
                    tapSound.start()
                    messageListSelectedYear.value = null
                    messageListSelectedMonth.value = null
                }
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Clear filters")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(filteredMessages) { index, message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
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

@Composable
fun NumericDataScreen(
    transactions: List<TransactionData>,
    onBack: () -> Unit,
    filterState: androidx.compose.runtime.MutableState<String>,
    selectedYear: androidx.compose.runtime.MutableState<Int?>,
    selectedMonth: androidx.compose.runtime.MutableState<Int?>,
    sortState: androidx.compose.runtime.MutableState<Pair<String, Boolean>>
) {
    // Add BackHandler at the top of the composable
    androidx.activity.compose.BackHandler(onBack = onBack)

    val selectedTransaction = remember { mutableStateOf<TransactionData?>(null) }

    // Add local dropdown visibility states
    val showYearFilter = remember { mutableStateOf(false) }
    val showMonthFilter = remember { mutableStateOf(false) }

    // Sound effects
    val context = LocalContext.current
    val tapSound = remember { MediaPlayer.create(context, R.raw.tap_sound) }
    val selectSound = remember { MediaPlayer.create(context, R.raw.select_sound) }

    // Clean up resources when composable is disposed
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            tapSound.release()
            selectSound.release()
        }
    }

    if (selectedTransaction.value != null) {
        MessageDetailScreen(
            message = selectedTransaction.value!!.originalMessage,
            onBack = {
                tapSound.seekTo(0)
                tapSound.start()
                selectedTransaction.value = null
            }
        )
    } else {
        val calendar = remember { Calendar.getInstance() }
        val defaultYear = calendar.get(Calendar.YEAR)
        val defaultMonth = calendar.get(Calendar.MONTH) + 1

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
        val filteredTransactions =
            remember(transactions, filterState.value, selectedYear.value, selectedMonth.value) {
                transactions.filter { transaction ->
                    val matchesType = when (filterState.value) {
                        "income" -> transaction.isIncome
                        "expense" -> !transaction.isIncome
                        else -> true
                    }

                    val cal = Calendar.getInstance().apply { time = transaction.date }

                    // Modified section: Only check year/month if selection exists
                    val matchesYear =
                        selectedYear.value?.let { cal.get(Calendar.YEAR) == it } ?: true
                    val matchesMonth =
                        selectedMonth.value?.let { (cal.get(Calendar.MONTH) + 1) == it } ?: true

                    matchesType && matchesYear && matchesMonth
                }
            }

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

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            androidx.compose.material3.AssistChip(
                onClick = {
                    selectSound.seekTo(0)
                    selectSound.start()
                    onBack()
                },
                label = { Text("Back to Messages") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.ArrowBack,
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
                            onClick = {
                                tapSound.seekTo(0)
                                tapSound.start()
                                showYearFilter.value = true
                            },
                            label = { Text(selectedYear.value?.toString() ?: "Year") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        DropdownMenu(
                            expanded = showYearFilter.value,
                            onDismissRequest = { showYearFilter.value = false }
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectSound.seekTo(0)
                                        selectSound.start()
                                        selectedYear.value = year
                                        selectedMonth.value = null
                                        showYearFilter.value = false
                                    }
                                )
                            }
                        }
                    }

                    // Month filter with dropdown
                    Box {
                        androidx.compose.material3.AssistChip(
                            onClick = {
                                tapSound.seekTo(0)
                                tapSound.start()
                                showMonthFilter.value = true
                            },
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
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showMonthFilter.value,
                            onDismissRequest = { showMonthFilter.value = false }
                        ) {
                            monthsInYear.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(DateFormatSymbols().months[month - 1]) },
                                    onClick = {
                                        selectSound.seekTo(0)
                                        selectSound.start()
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
                        tapSound.seekTo(0)
                        tapSound.start()
                        selectedYear.value = null
                        selectedMonth.value = null
                    }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear filters")
                }
            }

            // Filter chips
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("All", "Income", "Expense").forEach { filter ->
                    val filterKey = filter.lowercase()
                    androidx.compose.material3.FilterChip(
                        selected = filterKey == filterState.value,
                        onClick = {
                            tapSound.seekTo(0)
                            tapSound.start()
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
                        tapSound.seekTo(0)
                        tapSound.start()
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
                        tapSound.seekTo(0)
                        tapSound.start()
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

            LazyColumn(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .weight(1f)
            ) {
                itemsIndexed(sortedTransactions) { index, transaction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                selectSound.seekTo(0)
                                selectSound.start()
                                selectedTransaction.value = transaction
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(java.text.SimpleDateFormat("dd/MM/yy").format(transaction.date))
                        Text(
                            text = "$${formatAmount(transaction.amount)}",
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
                        TotalRow(
                            label = "Total Income:",
                            amount = totalIncome,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    "expense" -> {
                        TotalRow(
                            label = "Total Expense:",
                            amount = totalExpense,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        TotalRow(
                            label = "Total Income:",
                            amount = totalIncome,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TotalRow(
                            label = "Total Expense:",
                            amount = totalExpense,
                            color = MaterialTheme.colorScheme.error
                        )
                        TotalRow(
                            label = "Net Total:",
                            amount = totalIncome - totalExpense,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}