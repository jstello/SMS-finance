# Provider Stats Grouping – Issue & Fix

## Background

While investigating why the Providers screen didn't match the Categories UI for some transactions, we discovered that SMS-extracted transactions sometimes had a null `provider` but a non-null `contactName`. The Categories section uses `contactName` for display when `provider` is missing, but the Providers stats were grouping strictly by `transaction.provider` and lumping those under "Unknown."

## Investigation

- **Data Extraction**: `ExtractTransactionDataUseCase` sets `provider` first from the SMS body, then falls back to `contactName` if needed. 
- **Transaction Normalization**: In `TransactionRepositoryImpl.getTransactions()`, we ensure `provider = contactName` when `provider` is null.
- **Stats Aggregation**: `getProviderStats()` originally did:
  ```kotlin
  .groupBy { it.provider ?: "Unknown" }
  ```
- **Mismatch**: Transactions with `provider == null` but `contactName != null` were still grouped as "Unknown."

## Solution

Updated `getProviderStats` in `TransactionRepositoryImpl` to group by:
```kotlin
.groupBy { tx -> tx.contactName ?: tx.provider ?: "Unknown" }
```
This ensures contact-derived providers are shown correctly in the Providers screen.

## Verification

- Patched grouping logic and rebuilt the app.
- Confirmed that transactions with only `contactName` now appear under that name in Providers.

## Next Steps

1. Add unit tests for `getProviderStats` covering both `provider` and `contactName` cases.
2. Consider extracting grouping logic to a shared utility.
3. Review other report/aggregation code for similar inconsistencies. 