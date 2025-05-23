# Unstable Transaction IDs Causing Category Persistence Issues

**Date:** 2024-07-28 (Updated 2025-05-23)

## Problem Description

Previously, category assignments made by the user were not persisting correctly across application restarts. Transactions would appear as "Unassigned" because their underlying IDs changed with each SMS processing pass, breaking the link to their saved categories.

## Root Cause Analysis (Historical)

The original issue was that `ExtractTransactionDataUseCase` generated a new, random `UUID` for each `TransactionData` object it created from an SMS message. When these transactions were re-processed (e.g., on app restart or data refresh), they received new IDs. If category assignments were stored elsewhere (e.g., SharedPreferences) mapped by these unstable IDs, the link would be lost.

## Current Solution: Stable ID Generation & Direct Room Persistence

This issue has been addressed through two main changes:

1.  **Stable ID Generation for SMS Transactions:**
    The `ExtractTransactionDataUseCase` now implements a `generateStableId(input: String)` method. This method creates a deterministic MD5 hash based on a unique combination of SMS content:
    ```kotlin
    // In ExtractTransactionDataUseCase.kt
    private fun generateStableId(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("ExtractTransactionDataUseCase", "MD5 hashing failed, falling back to UUID", e)
            UUID.randomUUID().toString()
        }
    }

    // Called with:
    // val uniqueInput = "${message.dateTime.time}-${message.address}-${message.body}"
    // val stableId = generateStableId(uniqueInput)
    ```
    This ensures that the same SMS message will always produce the same `TransactionData.id`.

2.  **ID Generation for Manual Transactions:**
    For transactions added manually via `AddTransactionViewModel`, if the `TransactionData.id` is initially null, `TransactionRepositoryImpl.addTransaction()` assigns a `UUID.randomUUID().toString()` as the ID before inserting it into the Room database.
    ```kotlin
    // In TransactionRepositoryImpl.kt, simplified
    override suspend fun addTransaction(transaction: TransactionData) {
        val entity = transactionMapper.toEntity(transaction.copy(
            id = transaction.id ?: UUID.randomUUID().toString() // Ensure ID exists
        ))
        transactionDao.insert(entity)
    }
    ```

3.  **Direct Category Persistence in Room:**
    Category assignments are no longer stored in a separate SharedPreferences map linking transaction ID to category ID. Instead, the `TransactionEntity` in the Room database has a direct `categoryId: String?` field.
    ```kotlin
    // In TransactionEntity.kt
    @Entity(tableName = "transactions")
    data class TransactionEntity(
        @PrimaryKey val id: String, // Stable ID
        // ... other fields ...
        var categoryId: String?      // Foreign key to CategoryEntity
    )
    ```
    When a transaction is categorized, its corresponding `TransactionEntity` in the database is updated with the new `categoryId`.

## Outcome

With stable IDs generated for all transactions and category assignments stored directly within the transaction's record in the Room database, category persistence is now robust across application sessions and data refreshes. The previous reliance on separate SharedPreferences mappings for this purpose is obsolete. 