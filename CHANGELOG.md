## Changelog

### Version X.Y.Z (Date of Release)

#### Improvements

-   Added the ability to copy the original SMS message text from the Transaction Details screen. (Related to adding SelectionContainer)
-   Category colors are now displayed next to the category name in the main Transaction List view. (From "No Transactions Found for Provider" conversation)
-   Enabled inline editing of the SMS provider name directly in the Transaction Details screen, allowing users to Save or Cancel edits. (From "Editable Provider Name" feature)
-   Enforced that SMS import only processes messages starting with "Bancolombia:" to avoid importing non-relevant messages.
-   Filtered categories in the Income tab to only show those with actual income transactions by updating the UI logic to use `categorySpending.keys`.
-   Triggered data refresh when switching between Income and Expenses tabs so that the view updates immediately on tab change.
-   Removed legacy partial SMS refresh and implemented full SMS reprocessing (`limitToRecentMonths = 0`) in `refreshTransactionData()` to apply updated income detection logic to all messages.

#### Bug Fixes

-   Resolved an issue where filtering transactions by provider incorrectly showed "No transactions found" for providers linked via contact name. The filtering logic in the transaction list now correctly considers both contact name and provider name. (From "No Transactions Found for Provider" conversation)
-   Fixed a bug in the main Transaction List where the provider name was not displayed for transactions linked via contact name (phone number). The transaction list item now prioritizes displaying the contact name if available, falling back to the provider name. (From "Provider Name Display Issue in Transactions" conversation)
-   Prevented `IllegalArgumentException` in `saveTransactionToFirestore` by falling back to the authenticated user's ID when `transaction.userId` is null, ensuring provider edits succeed. (From diagnosing provider-save failure)
-   Ensured in-memory caches (`cachedTransactions` and ViewModel state flows) are updated after saving a transaction so the UI immediately reflects provider name changes. (From diagnosing stale provider issue)
-   Resolved issue where manually edited provider names were overwritten during SMS data refresh, ensuring provider edits persist across app restarts and refreshes.
-   Corrected inaccurate category totals displayed after refreshing the category detail screen; refresh now merges recent SMS data without discarding historical transaction data from the cache.
-   Prevented the creation of duplicate transactions originating from repeated SMS processing or related SMS messages generating the same transaction ID.
-   Fixed issue where the `isIncome` filter value was not being passed from the ViewModel to `getSpendingByCategory`, causing both Income and Expense tabs to display the same data.

#### Known Issues

-   Payroll is showing up in both Expenses and Income tabs for April 2025. Ensure `isIncome` flag is correctly set on payroll transactions. 
+   Unresolved build errors remain after Hilt migration and refactoring:
+     - Parameter mismatch when calling `ProvidersScreen` from `ProvidersActivity` (e.g., passing `initialSelectedDateMillis` which is not accepted).
+     - Missing required parameters in Composable function calls (e.g., `onBackClick`, `onProviderClick` for `ProvidersScreen`).
+     - Potential other compilation errors need investigation. 