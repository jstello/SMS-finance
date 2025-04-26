## Changelog

### Version X.Y.Z (Date of Release)

#### Improvements

-   Added the ability to copy the original SMS message text from the Transaction Details screen. (Related to adding SelectionContainer)
-   Category colors are now displayed next to the category name in the main Transaction List view. (From "No Transactions Found for Provider" conversation)
-   Enabled inline editing of the SMS provider name directly in the Transaction Details screen, allowing users to Save or Cancel edits. (From "Editable Provider Name" feature)

#### Bug Fixes

-   Resolved an issue where filtering transactions by provider incorrectly showed "No transactions found" for providers linked via contact name. The filtering logic in the transaction list now correctly considers both contact name and provider name. (From "No Transactions Found for Provider" conversation)
-   Fixed a bug in the main Transaction List where the provider name was not displayed for transactions linked via contact name (phone number). The transaction list item now prioritizes displaying the contact name if available, falling back to the provider name. (From "Provider Name Display Issue in Transactions" conversation)
-   Prevented `IllegalArgumentException` in `saveTransactionToFirestore` by falling back to the authenticated user's ID when `transaction.userId` is null, ensuring provider edits succeed. (From diagnosing provider-save failure)
-   Ensured in-memory caches (`cachedTransactions` and ViewModel state flows) are updated after saving a transaction so the UI immediately reflects provider name changes. (From diagnosing stale provider issue) 