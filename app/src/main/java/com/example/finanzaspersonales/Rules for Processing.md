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
    val sourceAccount: String? = null
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

Each section now shows the actual implementation code alongside the processing rules, making it easier to correlate the documentation with the codebase. The code snippets are taken directly from the current implementation in `MainActivity.kt` and show the complete processing flow from raw SMS data to structured financial information.