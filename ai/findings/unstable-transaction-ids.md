# Unstable Transaction IDs Causing Category Persistence Issues

**Date:** 2024-07-28

## Problem Description

Category assignments made by the user were not persisting across application restarts. When the user closed and reopened the app, all transactions appeared as "Unassigned" again, even though the assignments were being saved to SharedPreferences.

## Root Cause Analysis

The core issue was traced back to how transaction IDs were being generated and used for persistence:

1.  **Transaction Extraction:** The `ExtractTransactionDataUseCase` generated a new, random `UUID` for each `TransactionData` object it created from an SMS message:
    ```kotlin
    // in ExtractTransactionDataUseCase
    TransactionData(
        id = UUID.randomUUID().toString(), // New random ID on every extraction
        // ... other fields
    )
    ```
2.  **Persistence:** The `TransactionRepositoryImpl` stored the mapping between a transaction's `id` and its assigned `categoryId` in SharedPreferences using `SharedPrefsManager.saveTransactionCategories(transactionCategoryMap: Map<String, String>)`.
3.  **Loading:** On app start or data refresh, `TransactionRepositoryImpl` would:
    *   Re-extract transactions from SMS messages, generating *new* random UUIDs for the *same* underlying transactions.
    *   Load the `transactionId -> categoryId` map from SharedPreferences (`loadTransactionCategoryCache`).
    *   Attempt to apply saved categories in `applyCategoryAssignments`. However, because the `id` of a re-extracted transaction was different from the `id` saved in SharedPreferences, the lookup `transactionCategoryCache[txId]` would always fail, and no saved categories were reapplied.

The key used for persistence (`TransactionData.id`) was not stable across application sessions.

## Solution Implemented

To ensure stable IDs, the `TransactionRepositoryImpl.processChunk` method was modified:

1.  After extracting the `TransactionData` using `extractTransactionDataUseCase`, which might initially have a random ID (or null).
2.  The existing `generateTransactionKey(transaction: TransactionData)` method, which creates a deterministic hash based on transaction content (date, amount, type, provider), is called to generate a *stable* ID.
3.  A `copy` of the extracted transaction is created with this stable ID assigned to the `id` field.
4.  This transaction object (with the stable ID) is then added to the `cachedTransactions` list.

```kotlin
// In TransactionRepositoryImpl.processChunk
private suspend fun processChunk(chunk: List<SmsMessage>) {
    withContext(Dispatchers.Default) {
        val processedTransactions = chunk.mapNotNull { sms ->
            try {
                // Extract transaction data from SMS (might have a random ID)
                val extractedTransaction = extractTransactionDataUseCase.execute(listOf(sms)).firstOrNull()
                if (extractedTransaction != null) {
                    // Generate a stable ID based on content
                    val stableId = generateTransactionKey(extractedTransaction)
                    // Return a copy with the stable ID
                    extractedTransaction.copy(id = stableId)
                } else {
                    null // Skip if extraction failed
                }
            } catch (e: Exception) {
                Log.e("SMS_PROCESSING", "Error processing SMS: ${sms.body}", e)
                null
            }
        }
        
        // Update the main cache on the main thread (logic simplified for brevity)
        withContext(Dispatchers.Main) {
           // ... merge processedTransactions (now with stable IDs) into cachedTransactions ... 
        }
    }
}
```

This ensures that the same transaction always receives the same ID, allowing the SharedPreferences lookup to succeed and correctly reapply saved category assignments when the app restarts. 