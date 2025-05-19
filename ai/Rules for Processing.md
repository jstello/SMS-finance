# Rules for processing text messages

## Data Structures
```kotlin
// Core data classes storing processed information
data class SmsMessage(
    val address: String,
    val body: String,
    val amount: String?,         // Extracted currency string (e.g., "COP150000")
    val numericAmount: Float?,   // Converted numeric value (e.g., 150000f)
    val dateTime: java.util.Date?,  // Timestamp from SMS metadata
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

SMS messages have a timestamp associated with them by the Android system. This timestamp is the primary source for the transaction date.

-   **Primary Source**: `Telephony.Sms.Inbox.DATE` (long value representing milliseconds since epoch).
-   **Conversion**: This timestamp is directly converted to `java.util.Date`.

```kotlin
// Example from SmsDataSource logic:
// val date = cursor.getLong(dateColumn) // dateColumn is Telephony.Sms.Inbox.DATE
// val smsDateTime = Date(date)
```

## Amount Detection Logic

### Core Extraction Function
```kotlin
private fun extractAmountFromBody(body: String): String? {
    // Pattern to find amounts like $150,000.00, COP150000, $123.45
    val pattern = Pattern.compile("""(\\$|COP)\s*((\d{1,3}(?:[.,]\d{3})*|\d+))(?:([.,])(\d{2}))?""")
    val matcher = pattern.matcher(body)
    return if (matcher.find()) {
        val currency = matcher.group(1) // $, COP
        val mainNumber = matcher.group(2).replace("[.,]".toRegex(), "") // Remove thousands separators
        val decimal = matcher.group(5) // Decimal part like 00 or 45
        
        when {
            decimal == null -> "$currency$mainNumber" // e.g., $150000
            decimal == "00" -> "$currency$mainNumber" // e.g., COP120000 (from COP120.000,00)
            else -> "$currency$mainNumber.$decimal" // e.g., $123.45
        }
    } else null
}

private fun parseToFloat(amount: String?): Float? {
    return amount?.replace("^(\\\$|COP)".toRegex(), "") // Remove $ or COP prefix
        ?.replace(",", ".") // Ensure decimal point is a period
        ?.toFloatOrNull()
}
```

## Transaction Classification

Determines if a transaction is an income or an expense.

```kotlin
// In TextExtractors.kt
private val INCOME_KEYWORDS = listOf(
    "recibiste",
    "deposito",
    "abono",
    "transferencia recibida",
    "pago por",
    "received",
    "deposit",
    "credit",
    "incoming transfer"
)

// Actual function used by ExtractTransactionDataUseCase:
fun isIncome(body: String): Boolean {
    // Simplified: only checks for "recibiste"
    return body.contains("recibiste", ignoreCase = true)
    // TODO: Expand to use INCOME_KEYWORDS for more robust detection if needed
}

// In ExtractTransactionDataUseCase.kt (conceptual)
// val isTransactionIncome = TextExtractors.isIncome(message.body)
```

## Account Detection

Various regex patterns are used to detect account information (account numbers, phone numbers associated with accounts) from the SMS body. This is primarily handled in `TextExtractors.detectAccountInfo` and `TextExtractors.extractPhoneNumberFromAccount`.

### `TextExtractors.detectAccountInfo(body: String): Pair<String?, String?>`
Returns a pair: (detectedAccount, sourceAccount)

Patterns include:
- Bancolombia Nequi: `"Nequi te envio.*?cta \*(\d+)"`
- Bancolombia product: `"desde producto ([*]\d+)"` or `"a tu Prod ([*]\d+)"`
- Bancolombia (misidentified DaviPlata): `"en DaviPlata \*(\d+)"` (Likely an error, DaviPlata is not Bancolombia)
- DaviPlata: `"de su DaviPlata \*(\d+)"`, `"a su DaviPlata \*(\d+)"`, `"DaviPlata (\*\d+)"`
- General Nequi (phone number): `"Nequi.*?(\d{10})"`
- Masked numbers: `"[*]+(\d{3,4})"` (e.g., ****123)

### `TextExtractors.extractPhoneNumberFromAccount(account: String): String?`
Extracts a potential phone number from a detected account string.
Patterns include:
- `"\*(\d{10})$"` (e.g., *3001234567)
- `"0{4,}(3\d{9})"` (e.g., 00003001234567) - This pattern is also noted in provider fallback.
- `"^(3\d{9})$"` (e.g., 3001234567)

## SMS Processing Pipeline

The process of converting raw SMS into `TransactionData` involves several steps, primarily orchestrated by `SmsDataSource` and `ExtractTransactionDataUseCase`.

**1. `SmsDataSource.readSmsMessages()`**
   - Reads SMS from the device's Content Resolver (`Telephony.Sms.Inbox`).
   - For each SMS, it retrieves:
     - `address` (sender's phone number/shortcode).
     - `body` (SMS content).
     - `date` (timestamp from SMS metadata).
   - Populates an `SmsMessage` object:
     - `SmsMessage.address = address`
     - `SmsMessage.body = body`
     - `SmsMessage.dateTime = new Date(metadata_timestamp)`
     - `SmsMessage.amount = TextExtractors.extractAmountFromBody(body)`
     - `SmsMessage.numericAmount = TextExtractors.parseToFloat(amountString)`
     - Account Info:
       - `val (detectedAcc, sourceAcc) = TextExtractors.detectAccountInfo(body)`
       - `SmsMessage.detectedAccount = detectedAcc`
       - `SmsMessage.sourceAccount = sourceAcc`
     - Contact Info (from detected account's potential phone number):
       - `val phoneNumber = TextExtractors.extractPhoneNumberFromAccount(detectedAcc)`
       - `SmsMessage.recipientPhoneNumber = phoneNumber`
       - `SmsMessage.recipientContact = TextExtractors.lookupContactName(context, phoneNumber)` (if phone number found)
     - Initial Provider: `SmsMessage.provider = address` (This is a crucial initial assignment).

**2. `ExtractTransactionDataUseCase.execute(messages: List<SmsMessage>)`**
   - Takes a list of `SmsMessage` objects.
   - For each `SmsMessage`:
     - **Provider Refinement**:
       1. `val providerFromBody = TextExtractors.extractProviderFromBody(message.body)`
       2. `val contactName = message.recipientContact` (already populated by `SmsDataSource`)
       3. `val finalProvider = providerFromBody ?: contactName ?: message.address` (Order of precedence: body extraction, then contact name, then original sender address).
     - **Income Classification**: `val isIncome = TextExtractors.isIncome(message.body)`
     - Creates a `TransactionData` object:
       - `TransactionData.date = message.dateTime`
       - `TransactionData.amount = message.numericAmount`
       - `TransactionData.isIncome = isIncome`
       - `TransactionData.description = message.body`
       - `TransactionData.provider = finalProvider`
       - `TransactionData.contactName = message.recipientContact`
       - `TransactionData.accountInfo = AccountInfo(message.detectedAccount)` (if account detected)
       - `id`, `userId`, `categoryId` are handled by the repository or later stages.

## Enhanced Provider Handling

Provider identification is a multi-step process involving direct extraction from the SMS body, contact name resolution, and fallbacks.

**New Requirements & Current Implementation Status:**

1.  **Provider name editing with persistence**: (Status: Potentially a UI/Repository level feature. Not observed in core parsing/use case logic. `SharedPrefsManager` exists, could be used here but needs verification.)
    ```kotlin
    // Placeholder for saving/loading custom provider names (e.g., via SharedPreferences)
    // fun saveCustomProviderName(context: Context, originalIdentifier: String, customName: String)
    // fun loadCustomProviderName(context: Context, originalIdentifier: String): String?
    ```

2.  **Provider Detection and Fallback Logic** (Implemented in `ExtractTransactionDataUseCase` and `TextExtractors`):
    The final provider name for a transaction is determined by the following order of precedence:
    a.  Name extracted from SMS body by `TextExtractors.extractProviderFromBody()`. 
    b.  If no provider found in body, use `SmsMessage.recipientContact` (Contact name resolved via phone number lookup if a phone number was extracted from `detectedAccount`).
    c.  If still no provider, use `SmsMessage.address` (the original sender ID of the SMS).

    ```kotlin
    // In ExtractTransactionDataUseCase:
    // val providerFromBody = TextExtractors.extractProviderFromBody(message.body)
    // val contactName = message.recipientContact // Derived from detected account -> phone -> contact lookup
    // val finalProvider = providerFromBody ?: contactName ?: message.address
    ```

3.  **Bancolombia Income Pattern Extraction** (In `TextExtractors.extractProviderFromBody`):
    -   For messages like `Bancolombia: Recibiste ... de PROVIDER (a|en) tu cuenta ...`, extracts `PROVIDER`.
    -   For transfer messages like `Transferiste ... a la cuenta *06882320535`, extracts `06882320535` (account number).
    -   For button/gateway transfers like `Transferiste $X por Boton Bancolombia a Wompi SAS desde producto *XXXX`, extracts `Wompi SAS` (company name).
    
    ```kotlin
    // Pattern for account number in transfer messages
    // Example: "a la cuenta *06882320535" -> "06882320535"
    val destAccountPattern = Pattern.compile(
        """a\s+la\s+cuenta\s+\*?(\d{5,})""",
        Pattern.CASE_INSENSITIVE
    )
    
    // Pattern for button/gateway transfers with company names
    // Example: "por Boton Bancolombia a Wompi SAS desde" -> "Wompi SAS"
    val buttonTransferPattern = Pattern.compile(
        """por\s+(?:Boton|Bot[oó]n)\s+\w+\s+a\s+([A-Za-z][A-Za-z0-9\s&.]+?)\s+desde""",
        Pattern.CASE_INSENSITIVE
    )
    
    // Original pattern for provider name
    val bancolombiaIncomePattern = Pattern.compile(
        """de\s+(.+?)\s+(?:a|en)\s+tu\s+cuenta""",
        Pattern.CASE_INSENSITIVE
    )
    ```
    - Additional Bancolombia expense pattern: `(?:Compraste|pagaste)(?:\s+[\\$\w,.]+\s+|\s+)en\s+((?:[A-Z0-9]|[*])+(?:\s+[A-Z0-9*]+)*)`

4.  **General Expense Pattern Extraction** (In `TextExtractors.extractProviderFromBody`):
    - `(?:Compra|Pago)\s+en\s+(.+?)(?:\s+por|\s+con|\s+el|$)`

5.  **Direct Phone Number Detection & Contact Lookup for Provider Fallback**:
    -   `TextExtractors.extractPhoneNumberFromAccount(account: String)` extracts potential phone numbers (e.g., using `0{4,}(3\d{9})`) from *detected account strings*.
    -   If a phone number is found, `TextExtractors.lookupContactName(context, phoneNumber)` attempts to find a contact name.
    -   This `contactName` is then used as a fallback for the provider if `extractProviderFromBody` returns null (as per rule #2 logic).

6.  **General ALL CAPS Fallback for Provider** (In `TextExtractors.extractProviderFromBody`):
    -   If other specific patterns fail, it looks for the longest sequence of uppercase words (and numbers, `*`) in the message body as a potential provider name, excluding common currency codes.
    ```kotlin
    // Pattern: (?<!\\w)([A-Z0-9][A-Z0-9*]{2,}(?:\\s+[A-Z0-9*]+)*)(?!\\w)
    val allCapsPattern = Pattern.compile(
        """(?<!\\w)([A-Z0-9][A-Z0-9*]{2,}(?:\\s+[A-Z0-9*]+)*)(?!\\w)""",
        Pattern.CASE_INSENSITIVE
    )
    ```

This updated section aims to accurately reflect the multi-layered approach to provider identification currently in the codebase.

## Enhanced SMS Filtering
**Exclusion Rule:**
- Any SMS containing URLs should be excluded from transaction processing as they are likely promotional messages.
```kotlin
// Before processing, skip messages with URLs
if (TextExtractors.containsUrl(body)) {
    return false
}