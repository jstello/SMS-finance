# Rules for processing text messages

## Data Structures
```kotlin
// Core data classes storing processed information
data class SmsMessage(
    val address: String,
    val body: String,
    val amount: String?,         // Extracted currency string (e.g., "COP150000")
    val numericAmount: Float?,   // Converted numeric value (e.g., 150000f)
    val dateTime: java.util.Date?,
    val detectedAccount: String? = null,  // Detected account info
    val sourceAccount: String? = null,
    val recipientContact: String? = null,  // Added contact name from phone lookup
    val recipientPhoneNumber: String? = null,  // Extracted from account detection
    val provider: String? = null  // Detected or user-edited provider name
)

data class TransactionData(
    val date: java.util.Date,    // Parsed transaction date
    val amount: Float,           // Numeric amount
    val isIncome: Boolean,       // Income/expense classification
    val originalMessage: SmsMessage
)
```

## Date Parsing Logic

### Extraction Implementation
```kotlin
private fun extractDateTimeFromBody(body: String): Date? {
    val pattern = Pattern.compile("""(\d{2}/\d{2}/\d{4}).*?(\d{2}:\d{2}(?::\d{2})?)|(\d{2}:\d{2}(?::\d{2})?).*?(\d{2}/\d{2}/\d{4})""")
    val matcher = pattern.matcher(body)
    
    return if (matcher.find()) {
        when {
            matcher.group(1) != null && matcher.group(2) != null -> 
                parseDateTimeString("${matcher.group(1)} ${matcher.group(2)}")
            matcher.group(3) != null && matcher.group(4) != null -> 
                parseDateTimeString("${matcher.group(4)} ${matcher.group(3)}")
            else -> null
        }
    } else null
}

private fun parseDateTimeString(dateTimeStr: String): Date? {
    return try {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(dateTimeStr)
    } catch (e: Exception) {
        try {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(dateTimeStr)
        } catch (e: Exception) {
            null
        }
    }
}
```

## Amount Detection Logic

### Core Extraction Function
```kotlin
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
```

## Transaction Classification
```kotlin
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
```

## Account Detection
```kotlin
private fun detectAccountInfo(body: String): Pair<String?, String?> {
    val bancolombiaPattern = Pattern.compile(
        "a\\s(.+?)\\sdesde\\sproducto\\s([*]\\d+)"
    )
    val bcMatcher = bancolombiaPattern.matcher(body)
    if (bcMatcher.find()) {
        return Pair(bcMatcher.group(1), bcMatcher.group(2))
    }
    // ... other detection patterns
    return Pair(null, null)
}
```

## SMS Processing Pipeline
```kotlin
private fun readFilteredSMS(context: Context): List<SmsMessage> {
    // ... content resolver setup
    messages.add(SmsMessage(
        address = address,
        body = body,
        amount = extractAmountFromBody(body),
        numericAmount = parseToFloat(amount),
        dateTime = extractDateTimeFromBody(body),
        detectedAccount = detectedAccount,
        sourceAccount = sourceAccount
    ))
    return messages
}
```

## Enhanced Provider Handling
**New Requirements:**
1. Provider name editing with persistence:
```kotlin
// Save/Load custom provider names
private fun saveCustomProviderName(context: Context, key: String, providerName: String) {
    val prefs = context.getSharedPreferences("custom_provider_names", Context.MODE_PRIVATE)
    prefs.edit().putString(key, providerName).apply()
}

private fun loadCustomProviderName(context: Context, key: String): String? {
    return context.getSharedPreferences(...).getString(key, null)
}
```

2. Provider detection fallback:
```kotlin
val providerName = if (contactName == null) extractProviderFromBody(body) else null
```

## Contact Processing Pipeline
1. **Phone Number Extraction:**
```kotlin
private fun extractPhoneNumberFromAccount(account: String): String? {
    // Pattern: *0000+3XXXXXXXXX
    val pattern = Regex("""[*]?0{3,}(3\d{9})""")
    return pattern.find(account)?.groupValues?.get(1)
}
```

2. **Contact Lookup:**
```kotlin
private fun lookupContactName(context: Context, number: String): String? {
    val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
        .appendPath(number).build()
    // ... content resolver query ...
}
```

## Enhanced Transaction Classification
**Multi-language Support:**
```kotlin
val isIncome = message.body.contains(
    Regex("(recepci[óo]n|recibiste|n[óo]mina)", RegexOption.IGNORE_CASE)
)
```

**New Financial Categories:**
```kotlin
// Default expense categories including investments
listOf("Home", "Rent", "Pets", "Health", "Supermarket", 
       "Restaurants", "Utilities", "Subscriptions", 
       "Transportation", "Investments", "Other")
```

## Persistence Requirements
1. **Transaction Categories:**
```kotlin
private fun saveTransactionCategory(context: Context, key: String, category: String) {
    val prefs = context.getSharedPreferences("transaction_categories", Context.MODE_PRIVATE)
    prefs.edit().putString(key, category).apply()
}
```

2. **Account Directory:**
```kotlin
private fun saveAccounts(context: Context, accounts: List<AccountInfo>) {
    val json = Gson().toJson(accounts)
    context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
        .edit().putString("accounts", json).apply()
}
```

## Enhanced SMS Filtering
```kotlin
// Only process messages containing specific keywords
val cursor = context.contentResolver.query(
    uri,
    projection,
    "${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.BODY} LIKE ?",
    arrayOf("%Bancolombia%", "%Nequi%"),  // Filter by financial institutions
    "${Telephony.Sms.DATE} DESC"
)
```

## UI Integration Rules
1. **Provider Editing:**
   - Users can edit provider names directly in transaction details
   - Changes persist across sessions
   - Original detected provider maintained until edited

2. **Contact Linking:**
   - Automatically link transactions to contacts when:
     - Phone number detected in account info
     - Matching contact exists in device
   - Display contact-specific transaction summaries

3. **Category Management:**
   - Users can add new expense categories
   - Category changes apply to all existing transactions
   - Default categories cannot be removed

```kotlin
// Category management dialog
fun AddCategoryDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    // UI implementation for adding new categories
}
```

## Data Presentation Rules
1. Amount display formatting:
   - Always show COP prefix
   - Format numbers with thousands separators
   - Differentiate income (green) vs expense (red) 