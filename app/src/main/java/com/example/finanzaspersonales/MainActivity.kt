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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import java.util.Date
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.width
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.compose.material3.Card
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow

// Simplified Vico imports
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CardDefaults

data class SmsMessage(
    val address: String,
    val body: String,
    val amount: String?,
    val numericAmount: Float?,
    val dateTime: java.util.Date?,
    val detectedAccount: String? = null,
    val sourceAccount: String? = null,
    val recipientContact: String? = null,
    val recipientPhoneNumber: String? = null,
    val provider: String? = null  // Add this new field for provider names
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
private fun WhatsAppStyleMessageItem(
    message: SmsMessage,
    onClick: () -> Unit,
    onContactClick: ((String, String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Use first letter of contact name if available, otherwise address
            val displayChar = message.recipientContact?.take(1)?.uppercase() 
                            ?: message.address.take(1).uppercase()
            Text(
                text = displayChar,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            // Top Row (Title + Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title with amount if present and contact name if available
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (message.recipientContact != null && message.recipientPhoneNumber != null && onContactClick != null) {
                        // Make contact name clickable
                        Text(
                            text = message.recipientContact,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onContactClick(message.recipientContact, message.recipientPhoneNumber)
                            }
                        )
                    } else {
                        Text(
                            text = when {
                                message.recipientContact != null -> "${message.recipientContact}"
                                message.provider != null -> "${message.provider}"
                                message.amount != null -> "${message.amount}"
                                else -> message.address
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // If we have both contact and amount, show amount in a chip
                    if (message.recipientContact != null && message.amount != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = message.amount,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else if (message.amount != null) {
                        // Remove the address chip, instead display the provider or just the amount
                        if (message.provider != null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = message.provider,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = message.amount,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                
                // Date
                message.dateTime?.let {
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm").format(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Show phone number if we matched a contact
            if (message.recipientContact != null && message.recipientPhoneNumber != null) {
                Text(
                    text = "To: ${message.recipientPhoneNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            // Message Preview
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    
    Divider(
        modifier = Modifier.padding(start = 80.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    message: SmsMessage, 
    onBack: () -> Unit,
    navController: NavController? = null
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
            
            // Display recipient contact information if available - MAKE CLICKABLE
            if (message.recipientContact != null && navController != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            message.recipientPhoneNumber?.let { phone ->
                                navController.navigate("contact/${message.recipientContact}/${phone}")
                            }
                        },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Recipient:", style = MaterialTheme.typography.labelSmall)
                                Text(message.recipientContact, style = MaterialTheme.typography.titleMedium)
                                message.recipientPhoneNumber?.let {
                                    Text("Phone: $it", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            
                            Icon(
                                Icons.Filled.Analytics,
                                contentDescription = "View Contact Transactions",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else if (message.recipientContact != null) {
                // Non-clickable version when NavController is null
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Recipient:", style = MaterialTheme.typography.labelSmall)
                                Text(message.recipientContact, style = MaterialTheme.typography.titleMedium)
                                message.recipientPhoneNumber?.let {
                                    Text("Phone: $it", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            
                            Icon(
                                Icons.Filled.Analytics,
                                contentDescription = "View Contact Transactions",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else if (message.provider != null) {
                // Add provider information card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Provider:", style = MaterialTheme.typography.labelSmall)
                        Text(message.provider, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Replace the "From:" line with bank or institution if available
            if (message.provider != null) {
                Text("Bank/Institution: ${message.provider}", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("Transaction Details", style = MaterialTheme.typography.titleMedium)
            }
            
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
            val (detectedAccount, sourceAccount) = detectAccountInfo(body)
            
            // Extract phone number from the account number if any
            val phoneNumber = detectedAccount?.let { account -> 
                extractPhoneNumberFromAccount(account) 
            }
            
            // Look up contact name if phone number is available
            val contactName = phoneNumber?.let { number ->
                lookupContactName(context, number)
            }
            
            // Extract provider name if no contact info is available
            val providerName = if (contactName == null) extractProviderFromBody(body) else null
            
            messages.add(SmsMessage(
                address = address,
                body = body,
                amount = amount,
                numericAmount = parseToFloat(amount),
                dateTime = extractDateTimeFromBody(body),
                detectedAccount = detectedAccount,
                sourceAccount = sourceAccount,
                recipientContact = contactName,
                recipientPhoneNumber = phoneNumber,
                provider = providerName
            ))
        }
    }
    return messages
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SMSReader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val smsMessages = remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    val showNumericData = remember { mutableStateOf(false) }
    val transactions = remember { mutableStateOf<List<TransactionData>>(emptyList()) }
    val searchQuery = remember { mutableStateOf("") }
    
    // Add a refreshing state
    val isRefreshing = remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Function to refresh messages
    val refreshMessages = {
        coroutineScope.launch {
            isRefreshing.value = true
            smsMessages.value = readFilteredSMS(context)
            isRefreshing.value = false
        }
    }
    
    // Restore original filter states
    val messageListSelectedYear = remember { mutableStateOf<Int?>(null) }
    val messageListSelectedMonth = remember { mutableStateOf<Int?>(null) }
    val showMessageListYearFilter = remember { mutableStateOf(false) }
    val showMessageListMonthFilter = remember { mutableStateOf(false) }
    
    // Selected message for detail view
    val selectedMessage = remember { mutableStateOf<SmsMessage?>(null) }

    // Restore original message loading logic
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            smsMessages.value = readFilteredSMS(context)
        }
    }

    // Auto-load messages on first composition
    LaunchedEffect(Unit) {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )
        
        if (requiredPermissions.all { 
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED 
        }) {
            smsMessages.value = readFilteredSMS(context)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Update filtered messages to also search by contact
    val filteredMessages = remember(smsMessages.value, searchQuery.value, 
        messageListSelectedYear.value, messageListSelectedMonth.value) {
        
        smsMessages.value.filter { message ->
            val matchesText = searchQuery.value.isEmpty() || 
                message.body.contains(searchQuery.value, ignoreCase = true) ||
                message.recipientContact?.contains(searchQuery.value, ignoreCase = true) == true
            
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

    // Keep the navigation and transaction improvements
    val navController = rememberNavController()
    
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
                        transactions.value = extractTransactionData(filteredMessages)
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                if (selectedMessage.value != null) {
                    // Show message detail, now with NavController
                    MessageDetailScreen(
                        message = selectedMessage.value!!,
                        onBack = { selectedMessage.value = null },
                        navController = navController
                    )
                } else {
                    // Show WhatsApp-style list with pull-to-refresh
                    Column(modifier = Modifier.fillMaxSize()) {
                        // App bar with title
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Financial Messages") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        
                        // Search and filter controls
                        SearchBar(searchQuery)
                        YearMonthFilters(
                            messageListSelectedYear,
                            messageListSelectedMonth,
                            filteredMessages,
                            smsMessages.value
                        )
                        
                        // Wrap with SwipeRefresh
                        androidx.compose.material.pullrefresh.PullRefreshIndicator(
                            refreshing = isRefreshing.value,
                            state = rememberPullRefreshState(
                                refreshing = isRefreshing.value,
                                onRefresh = {
                                    refreshMessages()
                                }
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pullRefresh(
                                        state = rememberPullRefreshState(
                                            refreshing = isRefreshing.value,
                                            onRefresh = {
                                                refreshMessages()
                                            }
                                        )
                                    )
                            ) {
                                itemsIndexed(filteredMessages) { index, message ->
                                    WhatsAppStyleMessageItem(
                                        message = message,
                                        onClick = { selectedMessage.value = message },
                                        onContactClick = { name, phone ->
                                            navController.navigate("contact/$name/$phone")
                                        }
                                    )
                                }
                                
                                if (filteredMessages.isEmpty()) {
                                    item { EmptyState() }
                                }
                            }
                            
                            androidx.compose.material.pullrefresh.PullRefreshIndicator(
                                refreshing = isRefreshing.value,
                                state = rememberPullRefreshState(
                                    refreshing = isRefreshing.value,
                                    onRefresh = {
                                        refreshMessages()
                                    }
                                ),
                                modifier = Modifier.align(Alignment.TopCenter),
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            // Keep other screens
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
                    filterState = remember { mutableStateOf("all") },
                    selectedYear = remember { mutableStateOf<Int?>(null) },
                    selectedMonth = remember { mutableStateOf<Int?>(null) },
                    sortState = remember { mutableStateOf(Pair("date", false)) },
                    navController = navController
                )
            }
            composable(
                route = "dashboard/{year}/{month}",
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 0
                val month = backStackEntry.arguments?.getInt("month") ?: 0
                
                DashboardScreen(
                    year = year,
                    month = month,
                    transactions = transactions.value,
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }
            // Add contact detail route
            composable(
                route = "contact/{name}/{phone}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("phone") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                
                ContactTransactionSummary(
                    contactName = name,
                    phoneNumber = phone,
                    allTransactions = extractTransactionData(smsMessages.value),  // Use all SMS to generate transactions
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }
            // Add transaction detail route
            composable(
                route = "transaction_detail/{key}",
                arguments = listOf(
                    navArgument("key") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: ""
                val transaction = transactions.value.find { generateTransactionKey(it) == key }
                
                if (transaction != null) {
                    MessageDetailScreen(
                        message = transaction.originalMessage,
                        onBack = { navController.popBackStack() },
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
private fun YearMonthFilters(
    selectedYear: MutableState<Int?>,
    selectedMonth: MutableState<Int?>,
    filteredMessages: List<SmsMessage>,
    allMessages: List<SmsMessage>
) {
    val years = remember(allMessages) {
        allMessages.mapNotNull { it.dateTime?.toYear() }.distinct().sortedDescending()
    }
    
    val monthsInYear = remember(selectedYear.value) {
        selectedYear.value?.let { year ->
            allMessages.mapNotNull { 
                it.dateTime?.takeIf { it.toYear() == year }?.toMonth()
            }.distinct().sortedDescending()
        } ?: emptyList()
    }

    val showYearPicker = remember { mutableStateOf(false) }
    val showMonthPicker = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Year filter chip
        Box {
            AssistChip(
                onClick = { showYearPicker.value = true },
                label = {
                    Text(selectedYear.value?.toString() ?: "Select Year")
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = "Select Year",
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            DropdownMenu(
                expanded = showYearPicker.value,
                onDismissRequest = { showYearPicker.value = false }
            ) {
                years.forEach { year ->
                    DropdownMenuItem(
                        onClick = {
                            selectedYear.value = year
                            selectedMonth.value = null
                            showYearPicker.value = false
                        },
                        text = { Text(year.toString()) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        // Month filter chip
        Box {
            AssistChip(
                onClick = { showMonthPicker.value = true },
                enabled = selectedYear.value != null,
                label = {
                    Text(
                        selectedMonth.value?.let {
                            DateFormatSymbols().months[it - 1]
                        } ?: "Select Month"
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Month",
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            DropdownMenu(
                expanded = showMonthPicker.value,
                onDismissRequest = { showMonthPicker.value = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        selectedMonth.value = null
                        showMonthPicker.value = false
                    },
                    text = { Text("All Months") }
                )
                
                monthsInYear.forEach { month ->
                    DropdownMenuItem(
                        onClick = {
                            selectedMonth.value = month
                            showMonthPicker.value = false
                        },
                        text = { Text(DateFormatSymbols().months[month - 1]) }
                    )
                }
            }
        }
    }
}

// Restore helper extensions
private fun Date.toYear(): Int {
    return Calendar.getInstance().apply { time = this@toYear }.get(Calendar.YEAR)
}

private fun Date.toMonth(): Int {
    return Calendar.getInstance().apply { time = this@toMonth }.get(Calendar.MONTH) + 1
}

@Composable
fun NumericDataScreen(
    transactions: List<TransactionData>,
    onBack: () -> Unit,
    filterState: MutableState<String>,
    selectedYear: MutableState<Int?>,
    selectedMonth: MutableState<Int?>,
    sortState: MutableState<Pair<String, Boolean>>,
    navController: NavController
) {
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
        if (!selectedTransaction.value!!.isIncome) {
            // For expense transactions, show the new ExpenseDetailScreen with category editing
            ExpenseDetailScreen(
                transaction = selectedTransaction.value!!,
                onBack = {
                    tapSound.seekTo(0)
                    tapSound.start()
                    selectedTransaction.value = null
                },
                onCategoryChange = { newCategory ->
                    saveTransactionCategory(context, generateTransactionKey(selectedTransaction.value!!), newCategory)
                    tapSound.seekTo(0)
                    tapSound.start()
                    selectedTransaction.value = null
                }
            )
        } else {
            // For income transactions, show the original detail screen.
            MessageDetailScreen(
                message = selectedTransaction.value!!.originalMessage,
                onBack = {
                    tapSound.seekTo(0)
                    tapSound.start()
                    selectedTransaction.value = null
                }
            )
        }
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
                    val matchesYear = selectedYear.value?.let { cal.get(Calendar.YEAR) == it } ?: true
                    val matchesMonth = selectedMonth.value?.let { (cal.get(Calendar.MONTH) + 1) == it } ?: true

                    matchesType && matchesYear && matchesMonth
                }
            }

        val sortedTransactions = remember(filteredTransactions, sortState.value) {
            when (sortState.value.first) {
                "amount" -> {
                    if (sortState.value.second) filteredTransactions.sortedBy { it.amount }
                    else filteredTransactions.sortedByDescending { it.amount }
                }
                else -> { // date is default
                    if (sortState.value.second) filteredTransactions.sortedBy { it.date }
                    else filteredTransactions.sortedByDescending { it.date }
                }
            }
        }

        // Calculate totals - Fix this part to ensure proper calculation
        val (totalIncome, totalExpense) = remember(filteredTransactions) {
            var income = 0f
            var expense = 0f
            
            // Print debugging info to check transaction data
            println("Number of filtered transactions: ${filteredTransactions.size}")
            
            filteredTransactions.forEach { transaction ->
                println("Transaction: ${transaction.date}, Amount: ${transaction.amount}, IsIncome: ${transaction.isIncome}")
                if (transaction.isIncome) {
                    income += transaction.amount
                    println("Added to income: $income")
                } else {
                    expense += transaction.amount
                    println("Added to expense: $expense")
                }
            }
            Pair(income, expense)
        }

        // Fix layout issues - Wrap in a proper Column with Scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Transactions") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),  // Use fillMaxSize to ensure content spans the entire screen
                verticalArrangement = Arrangement.spacedBy(8.dp)  // Add spacing between elements
            ) {
                // Filter chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("all", "income", "expense").forEach { filter ->
                        androidx.compose.material3.FilterChip(
                            selected = filter == filterState.value,
                            onClick = {
                                tapSound.seekTo(0)
                                tapSound.start()
                                filterState.value = filter
                            },
                            label = { Text(filter.capitalize()) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (filter) {
                                    "income" -> MaterialTheme.colorScheme.primary
                                    "expense" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        )
                    }
                }

                // Year/Month filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            DropdownMenuItem(
                                onClick = {
                                    selectedYear.value = null
                                    selectedMonth.value = null
                                    showYearFilter.value = false
                                },
                                text = { Text("All Years") }
                            )
                            
                            years.forEach { year ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedYear.value = year
                                        selectedMonth.value = null
                                        showYearFilter.value = false
                                    },
                                    text = { Text(year.toString()) }
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
                                        DateFormatSymbols().months[it - 1]
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
                            DropdownMenuItem(
                                onClick = {
                                    selectedMonth.value = null
                                    showMonthFilter.value = false
                                },
                                text = { Text("All Months") }
                            )
                            
                            monthsInYear.forEach { month ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedMonth.value = month
                                        showMonthFilter.value = false
                                    },
                                    text = { Text(DateFormatSymbols().months[month - 1]) }
                                )
                            }
                        }
                    }
                }

                // Table header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date/Time", fontWeight = FontWeight.Bold)
                        Text("Amount (COP)", fontWeight = FontWeight.Bold)
                    }
                }

                // Transaction list
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)  // Allow the list to take remaining space
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(sortedTransactions) { index, transaction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectSound.seekTo(0)
                                        selectSound.start()
                                        selectedTransaction.value = transaction
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(java.text.SimpleDateFormat("dd/MM/yy").format(transaction.date))
                                    Text(
                                        text = "$${formatAmount(transaction.amount)}",
                                        color = if (transaction.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Total summary card at the bottom
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                TotalRow(
                                    label = "Net Total:",
                                    amount = totalIncome - totalExpense,
                                    color = if (totalIncome >= totalExpense) 
                                               MaterialTheme.colorScheme.primary 
                                           else 
                                               MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Dashboard button if applicable
                if (selectedYear.value != null && selectedMonth.value != null) {
                    androidx.compose.material3.AssistChip(
                        onClick = {
                            navController.navigate("dashboard/${selectedYear.value}/${selectedMonth.value}")
                        },
                        label = { Text("View Dashboard") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Analytics,
                                contentDescription = "Dashboard",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(searchQuery: MutableState<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
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
            
            BasicTextField(
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
    }
}

@Composable
private fun EmptyState() {
    Text(
        "No messages found",
        modifier = Modifier.padding(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    year: Int,
    month: Int,
    transactions: List<TransactionData>,
    onBack: () -> Unit,
    navController: NavController
) {
    val monthlyData = remember(transactions) {
        transactions.filter {
            val cal = Calendar.getInstance().apply { time = it.date }
            cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
        }
    }

    val (totalIncome, totalExpense) = remember(monthlyData) {
        var income = 0f
        var expense = 0f
        monthlyData.forEach {
            if (it.isIncome) income += it.amount else expense += it.amount
        }
        Pair(income, expense)
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("$month/$year Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Income/Expense Ratio Pie Chart
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Income vs Expenses", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    PieChart(
                        entries = listOf(
                            totalIncome to MaterialTheme.colorScheme.primary,
                            totalExpense to MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Legend(items = listOf(
                        "Income" to MaterialTheme.colorScheme.primary,
                        "Expenses" to MaterialTheme.colorScheme.error
                    ))
                }
            }

            // Monthly Trend Bar Chart
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Trend", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    BarChart(
                        data = calculateDailyTrend(monthlyData, year, month),
                        modifier = Modifier.height(200.dp)
                    )
                }
            }
        }
    }
} // Single closing bracket for the function

@Composable
private fun PieChart(
    entries: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier
) {
    // Instead of using a pie chart (which is causing issues), let's display the data in a simpler way
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        entries.forEach { (value, color) ->
            if (value > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (color == MaterialTheme.colorScheme.primary) "Income" else "Expense",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "COP ${formatAmount(value)}",
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Add total if we have both values
        if (entries.size == 2 && entries[0].first > 0 && entries[1].first > 0) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Net Total:", fontWeight = FontWeight.Bold)
                val total = entries[0].first - entries[1].first
                Text(
                    text = "COP ${formatAmount(total)}",
                    color = if (total >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BarChart(
    data: Map<Int, Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    // Only include days with actual data
    val filteredData = data.filter { (_, values) -> 
        values.first > 0f || values.second > 0f 
    }
    
    if (filteredData.isNotEmpty()) {
        // Convert day -> (income, expense) to list of FloatEntry for income
        val entries = filteredData.map { (day, values) ->
            FloatEntry(x = day.toFloat(), y = values.first) // Display income
        }

        // Create a simple column chart showing income by day
        Chart(
            chart = columnChart(),
            model = entryModelOf(entries),
            modifier = modifier,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis()
        )
    } else {
        // Fallback if no data
        Text("No transaction data available", modifier = modifier.padding(16.dp))
    }
}

@Composable
private fun Legend(items: List<Pair<String, Color>>) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

private fun calculateDailyTrend(
    transactions: List<TransactionData>,
    year: Int,
    month: Int
): Map<Int, Pair<Float, Float>> {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    return (1..daysInMonth).associate { day ->
        val dailyTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { time = it.date }
            cal.get(Calendar.DAY_OF_MONTH) == day
        }
        
        val income = dailyTransactions.filter { it.isIncome }.sumOf { it.amount.toDouble() }.toFloat()
        val expense = dailyTransactions.filter { !it.isIncome }.sumOf { it.amount.toDouble() }.toFloat()
        
        day to Pair(income, expense)
    }
}

// New composable to display and edit expense transaction details with a category dropdown.
@Composable
fun ExpenseDetailScreen(
    transaction: TransactionData,
    onBack: () -> Unit,
    onCategoryChange: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("Uncategorized") }
    val key = generateTransactionKey(transaction)
    LaunchedEffect(key) {
        val saved = loadTransactionCategory(context, key)
        if (saved != null) {
            selectedCategory = saved
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Replace sender address with provider or a generic title
            if (transaction.originalMessage.provider != null) {
                Text("Merchant: ${transaction.originalMessage.provider}", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("Expense Transaction", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Provider information is now merged into the title
            
            transaction.originalMessage.dateTime?.let {
                Text("Date: ${java.text.SimpleDateFormat("dd/MM/yy").format(it)}")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            transaction.originalMessage.amount?.let {
                Text("Amount: $it", color = Color.Red, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Category:", style = MaterialTheme.typography.titleSmall)
            CategoryDropdown(selectedCategory = selectedCategory, onCategorySelected = { newCategory ->
                selectedCategory = newCategory
            })
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onCategoryChange(selectedCategory) }) {
                Text("Save Category")
            }
        }
    }
}

// New helper functions for persistent expense categories:

private fun loadExpenseCategories(context: android.content.Context): List<String> {
    val prefs = context.getSharedPreferences("expense_categories", android.content.Context.MODE_PRIVATE)
    val json = prefs.getString("expense_categories", null)
    return if (json != null) {
        Gson().fromJson(json, Array<String>::class.java).toList()
    } else {
        // Default list including Investments and Rent
        listOf("Home", "Rent", "Pets", "Health", "Supermarket", "Restaurants", "Utilities", "Subscriptions", "Transportation", "Investments", "Other")
    }
}

private fun saveExpenseCategories(context: android.content.Context, categories: List<String>) {
    val prefs = context.getSharedPreferences("expense_categories", android.content.Context.MODE_PRIVATE)
    val json = Gson().toJson(categories)
    prefs.edit().putString("expense_categories", json).apply()
}

private fun addExpenseCategory(context: android.content.Context, newCategory: String, currentList: List<String>): List<String> {
    if (currentList.contains(newCategory)) return currentList
    val newList = currentList + newCategory
    saveExpenseCategories(context, newList)
    return newList
}

// New composable to add a category via a dialog.
@Composable
fun AddCategoryDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = { Text("Category Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textFieldValue.isNotBlank()) {
                        onAdd(textFieldValue)
                    }
                    onDismiss()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Updated CategoryDropdown composable:
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val context = LocalContext.current
    var expenseCategories by remember { mutableStateOf(loadExpenseCategories(context)) }
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedCategory)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            expenseCategories.forEach { category ->
                DropdownMenuItem(
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    text = { Text(category) }
                )
            }
            // Extra option to add a new category
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    showAddDialog = true
                },
                text = { Text("Add New Category") }
            )
        }
        if (showAddDialog) {
            AddCategoryDialog(
                onAdd = { newCategory ->
                    // Update the persistent list and set the new category as selected.
                    expenseCategories = addExpenseCategory(context, newCategory, expenseCategories)
                    onCategorySelected(newCategory)
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

// Utility function to generate a unique key for a transaction based on its date and message content.
private fun generateTransactionKey(transaction: TransactionData): String {
    return transaction.date.time.toString() + "_" + transaction.originalMessage.body.hashCode().toString()
}

// Utility function to save a selected category to the shared preferences.
private fun saveTransactionCategory(context: android.content.Context, key: String, category: String) {
    val prefs = context.getSharedPreferences("transaction_categories", android.content.Context.MODE_PRIVATE)
    prefs.edit().putString(key, category).apply()
}

// Utility function to load a saved category from the shared preferences.
private fun loadTransactionCategory(context: android.content.Context, key: String): String? {
    val prefs = context.getSharedPreferences("transaction_categories", android.content.Context.MODE_PRIVATE)
    return prefs.getString(key, null)
}

// Add this function to look up contact names from phone numbers
private fun lookupContactName(context: android.content.Context, phoneNumber: String): String? {
    val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
        .buildUpon()
        .appendPath(phoneNumber)
        .build()
    
    val projection = arrayOf(
        android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME
    )
    
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(
                android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME))
        }
    }
    return null
}

// New function to extract recipient phone number from account strings with updated pattern
private fun extractPhoneNumberFromAccount(accountNumber: String): String? {
    // More general pattern: 
    // - Looks for a number starting with "3" followed by 9 more digits (10 digits total)
    // - The number must be preceded by at least 3 zeros
    // - The whole thing might be prefixed with "*" (optional)
    val pattern = Regex("""[*]?0{3,}(3\d{9})""")
    val match = pattern.find(accountNumber)
    
    return match?.groupValues?.get(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTransactionSummary(
    contactName: String,
    phoneNumber: String,
    allTransactions: List<TransactionData>,
    onBack: () -> Unit,
    navController: NavController
) {
    val contactTransactions = remember(allTransactions, contactName, phoneNumber) {
        allTransactions.filter { transaction ->
            transaction.originalMessage.recipientContact == contactName &&
            transaction.originalMessage.recipientPhoneNumber == phoneNumber
        }
    }
    
    val totalSent = remember(contactTransactions) {
        contactTransactions.sumOf { it.amount.toDouble() }.toFloat()
    }
    
    // Year/Month filters
    val selectedYear = remember { mutableStateOf<Int?>(null) }
    val selectedMonth = remember { mutableStateOf<Int?>(null) }
    val showYearFilter = remember { mutableStateOf(false) }
    val showMonthFilter = remember { mutableStateOf(false) }
    
    // Filtered transactions
    val filteredTransactions = remember(contactTransactions, selectedYear.value, selectedMonth.value) {
        contactTransactions.filter { transaction ->
            val cal = Calendar.getInstance().apply { time = transaction.date }
            
            val matchesYear = selectedYear.value?.let { 
                cal.get(Calendar.YEAR) == it 
            } ?: true
            
            val matchesMonth = selectedMonth.value?.let { 
                (cal.get(Calendar.MONTH) + 1) == it 
            } ?: true
            
            matchesYear && matchesMonth
        }
    }
    
    // Calculate filtered total
    val filteredTotal = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount.toDouble() }.toFloat()
    }
    
    // Years available
    val years = remember(contactTransactions) {
        contactTransactions
            .map { transaction ->
                Calendar.getInstance().apply { time = transaction.date }
                    .get(Calendar.YEAR)
            }
            .distinct()
            .sorted()
    }
    
    // Months available in selected year
    val months = remember(contactTransactions, selectedYear.value) {
        if (selectedYear.value == null) emptyList()
        else {
            contactTransactions
                .filter { transaction ->
                    val cal = Calendar.getInstance().apply { time = transaction.date }
                    cal.get(Calendar.YEAR) == selectedYear.value
                }
                .map { transaction ->
                    Calendar.getInstance().apply { time = transaction.date }
                        .get(Calendar.MONTH) + 1
                }
                .distinct()
                .sorted()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Contact info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Contact Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phone: $phoneNumber")
                    Text("Total Transactions: ${contactTransactions.size}")
                    Text(
                        "Total Amount: $${formatAmount(totalSent)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Year filter
                Box {
                    AssistChip(
                        onClick = { showYearFilter.value = true },
                        label = { Text(selectedYear.value?.toString() ?: "All Years") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = "Select Year",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showYearFilter.value,
                        onDismissRequest = { showYearFilter.value = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                selectedYear.value = null
                                selectedMonth.value = null
                                showYearFilter.value = false
                            },
                            text = { Text("All Years") }
                        )
                        
                        years.forEach { year ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedYear.value = year
                                    selectedMonth.value = null
                                    showYearFilter.value = false
                                },
                                text = { Text(year.toString()) }
                            )
                        }
                    }
                }
                
                // Month filter
                Box {
                    AssistChip(
                        onClick = { 
                            if (selectedYear.value != null) showMonthFilter.value = true 
                        },
                        enabled = selectedYear.value != null,
                        label = { 
                            Text(
                                selectedMonth.value?.let { 
                                    DateFormatSymbols().months[it - 1] 
                                } ?: "All Months"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Month",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showMonthFilter.value,
                        onDismissRequest = { showMonthFilter.value = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                selectedMonth.value = null
                                showMonthFilter.value = false
                            },
                            text = { Text("All Months") }
                        )
                        
                        months.forEach { month ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedMonth.value = month
                                    showMonthFilter.value = false
                                },
                                text = { Text(DateFormatSymbols().months[month - 1]) }
                            )
                        }
                    }
                }
                
                // Clear filters
                if (selectedYear.value != null || selectedMonth.value != null) {
                    IconButton(
                        onClick = {
                            selectedYear.value = null
                            selectedMonth.value = null
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear Filters")
                    }
                }
            }
            
            // Filtered total
            if (selectedYear.value != null || selectedMonth.value != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Filtered Total: $${formatAmount(filteredTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Transaction list
            Text(
                "Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Use fillMaxSize instead of weight
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredTransactions) { index, transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = {
                            navController.navigate("transaction_detail/${generateTransactionKey(transaction)}")
                        }
                    )
                    
                    if (index < filteredTransactions.size - 1) {
                        Divider()
                    }
                }
                
                if (filteredTransactions.isEmpty()) {
                    item {
                        Text(
                            "No transactions found for the selected filters",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: TransactionData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                java.text.SimpleDateFormat("dd/MM/yyyy").format(transaction.date),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                transaction.originalMessage.body,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(240.dp) // Use width instead of widthIn
            )
        }
        
        Text(
            "$${formatAmount(transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// Add this extension function at the top level
private fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
    }
}

private fun extractProviderFromBody(body: String): String? {
    // First, try to extract the provider using the common Bancolombia pattern
    val bancolombiaPattern = Pattern.compile("""(?:Compraste|pagaste)(?:\s[\$\w,.]+\s|\s)en\s((?:[A-Z0-9]|[*])+(?:\s[A-Z0-9]+)*)""")
    val matcher = bancolombiaPattern.matcher(body)
    
    if (matcher.find()) {
        return matcher.group(1)
    }
    
    // Fallback to finding any words in ALL CAPS with at least 3 characters
    val allCapsPattern = Pattern.compile("""(?<!\w)([A-Z0-9][A-Z0-9*]+(?:\s[A-Z0-9]+)*)(?!\w)""")
    val allCapsMatcher = allCapsPattern.matcher(body)
    
    // Find the longest all-caps match that's likely to be a provider
    var bestMatch: String? = null
    var maxLength = 0
    
    while (allCapsMatcher.find()) {
        val match = allCapsMatcher.group(1)
        // Skip known non-provider words often in ALL CAPS (add more as needed)
        if (match != "COP" && match != "USD" && match.length > 3 && match.length > maxLength) {
            maxLength = match.length
            bestMatch = match
        }
    }
    
    return bestMatch
}