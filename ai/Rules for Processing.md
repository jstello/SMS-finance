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
    val id: String?, // Can be null initially
    val userId: String?, // Can be null initially
    val date: java.util.Date,    // Parsed transaction date
    val amount: Float,           // Numeric amount
    val isIncome: Boolean,       // Income/expense classification
    val description: String?,    // Original SMS body or null for manual entries
    val provider: String?,       // Detected/extracted provider, or manually entered provider
    val contactName: String?,    // Contact name if resolved from number
    val accountInfo: AccountInfo?, // Detected account info
    var categoryId: String?      // Mutable for easier assignment in cache?
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
                id = null,
                userId = null,
                date = message.dateTime,
                amount = message.numericAmount,
                isIncome = message.body.contains(
                    Regex("(recepci[óo]n|recibiste|n[óo]mina)", RegexOption.IGNORE_CASE)
                ),
                description = message.body,
                provider = message.provider,
                contactName = message.recipientContact,
                accountInfo = message.detectedAccount?.let { AccountInfo(it) },
                categoryId = null
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

3. Bancolombia income pattern extraction:
   - For messages like `Bancolombia: Recibiste un pago por AMOUNT de PROVIDER a tu cuenta ...` or `Bancolombia: Recibiste una transferencia por AMOUNT de PROVIDER en tu cuenta ...`, extract the uppercase provider name immediately after `de ` and before `a tu cuenta` or `en tu cuenta`.
```kotlin
// Capture PROVIDER in 'de PROVIDER a tu cuenta' or 'de PROVIDER en tu cuenta'
val specificIncomePattern = Pattern.compile(
    """de\s+(.+?)\s+(?:a|en)\s+tu\s+cuenta""",
    Pattern.CASE_INSENSITIVE
)
```

4. Bancolombia expense pattern extraction:
   - For messages like `Bancolombia: Pagaste $AMOUNT a PROVIDER desde tu producto *XXXX ...`, extract the provider name between `a ` and ` desde tu producto`.
   - For messages like `Bancolombia: Compraste $AMOUNT en PROVIDER con tu T.Deb *XXXX ...`, extract the provider name between `en ` and ` con tu`.
```kotlin
// Capture PROVIDER in 'a PROVIDER desde tu producto'
val specificExpensePattern = Pattern.compile(
    """(?:Compraste|Pagaste)\s+(?:\$|COP)\s*[\d.,]+\s+a\s+(.+?)\s+desde""",
    Pattern.CASE_INSENSITIVE
)

// Capture PROVIDER in 'en PROVIDER con tu'
val conTuExpensePattern = Pattern.compile(
    """(?:Compraste|Pagaste)\s+(?:\$|COP)\s*[\d.,]+\s+en\s+(.+?)\s+con\s+tu""",
    Pattern.CASE_INSENSITIVE
)
```

5. Direct phone number detection fallback:
   - If no provider was extracted by text patterns, look for a phone number within the SMS body matching `0{4,}(3\d{9})`. If found, attempt a contact lookup and use that contact name as the provider.
```kotlin
// Direct phone number detection fallback
private val directPhonePattern = Regex("0{4,}(3\\d{9})")
val directPhoneMatch = directPhonePattern.find(body)
if (directPhoneMatch != null) {
    val directPhoneNumber = directPhoneMatch.groupValues[1]
    val directContact = lookupContactName(context, directPhoneNumber)
    if (directContact != null) {
        provider = directContact
    }
}
```

## Enhanced SMS Filtering
**Exclusion Rule:**
- Any SMS containing URLs should be excluded from transaction processing as they are likely promotional messages.
```kotlin
// Before processing, skip messages with URLs
if (TextExtractors.containsUrl(body)) {
    return false
}
```