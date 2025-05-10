# Finanzas Personales Android App Documentation

## Overview
A personal finance management Android application that automatically tracks transactions from SMS messages and manual entries, with Firestore synchronization and local caching. Key features include:
- SMS transaction parsing for Colombian financial institutions
- Transaction categorization system
- Firestore cloud sync with offline support
- Provider recognition and contact matching
- Spending analytics by category/provider

## Key Technical Components

### 1. Core Architecture
```
app/
├── data/
│   ├── auth/            # Firebase authentication
│   ├── local/           # SMS processing & SharedPreferences
│   ├── model/           # Data classes (Transaction, Category)
│   ├── repository/      # Main business logic
│   └── sms/             # SMS receiver implementation
├── domain/
│   ├── usecase/         # Business logic components
│   └── util/            # Helpers (date, strings, contacts)
└── ui/
    ├── add_transaction/ # Manual entry UI
    ├── auth/            # Login screen
    ├── categories/      # Category management
    ├── dashboard/       # Spending overview
    ├── providers/       # Transaction sources analysis
    └── transaction_list/# Full transaction history
```

### 2. Data Processing Pipeline

#### SMS Extraction Rules (from `Rules for Processing.md`)
```kotlin
// Simplified processing flow:
1. SMS Filtering:
   - Exclude messages with URLs
   - Detect financial transaction patterns

2. Field Extraction:
   - Date: Regex `(\d{2}/\d{2}/\d{4})` with fallback to SMS timestamp
   - Amount: COP currency patterns with decimal handling
   - Provider: Hierarchical detection:
     a) Direct pattern matching (e.g., "de PROVIDER a tu cuenta")
     b) Contact name lookup from phone number
     c) Fallback to SMS sender

3. Transaction Classification:
   - Income detection via keywords: "recibiste", "nómina"
   - Expense default classification

4. Account Detection:
   - Bancolombia pattern: "producto [*XXXX]"
   - Nequi/Daviplata mobile wallet detection
```

### 3. Transaction Handling
```kotlin
data class TransactionData(
    val id: String,       // Stable hash of content fields
    val date: Date,
    val amount: Float,
    val isIncome: Boolean,
    val provider: String?,
    val contactName: String?,
    var categoryId: String? // Persisted in SharedPreferences
)
```
- **ID Generation:** SHA-256 hash of (date, amount, type, provider)
- **Caching:** Two-layer cache:
  1. Memory cache for quick access
  2. SharedPreferences for category assignments
- **Firestore Sync:** User-specific subcollections:
  `users/{userId}/transactions/{transactionId}`

### 4. Category System
- Default categories stored in SharedPreferences
- Custom categories sync with Firestore
- "Other" category handling:
  ```kotlin
  fun getUncategorizedCategoryPlaceholder() = Category(
      id = null, 
      name = "Other",
      color = Color.GRAY
  )
  ```
- Assignment persistence using `transactionId → categoryId` mapping

### 5. Provider Analysis
- Automatic detection from SMS patterns
- Manual override with persistence:
  ```kotlin
  fun saveCustomProviderName(key: String, name: String) {
      sharedPrefs.edit().putString("provider_$key", name).apply()
  }
  ```
- Stats aggregation by normalized provider/contact

## Firestore Schema
```javascript
// Transactions
{
  "id": "stable_hash",
  "userId": "firebase_uid",
  "date": "2024-03-15T14:30:00",
  "amount": 150000.0,
  "isIncome": false,
  "provider": "Éxito",
  "contactName": "Éxito Calle 100",
  "categoryId": "a1b2c3d4"
}

// Users
{
  "categories": {
    "a1b2c3d4": {
      "name": "Groceries",
      "color": "#FF8800"
    }
  }
}
```

## Key Architectural Patterns
1. Cache-First Strategy:
   - `TransactionRepository` serves cached data immediately
   - Manual refresh triggers full SMS rescan + Firestore sync
   
2. Dependency Injection:
   - Hilt components for ViewModel creation
   - Repository dependencies managed via constructor injection

3. Error Resiliency:
   - SMS processing error handling with retries
   - Firestore offline persistence
   - SharedPreferences fallback for critical data

## LLM Context Notes
- All monetary values in COP (Colombian Pesos)
- Date formats follow `dd/MM/yyyy`
- Primary SMS patterns from Bancolombia, Nequi, Daviplata
- Transaction IDs are content-hash stable across app restarts
