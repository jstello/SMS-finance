# Finanzas Personales Android App Documentation (Offline-Only Edition)

## Overview
A personal finance management Android application that automatically tracks transactions from SMS messages and manual entries, with **all data stored locally in an on-device SQL database**. Key features include:
- SMS transaction parsing for Colombian financial institutions
- Transaction categorization system
- **Local data persistence using Room**
- Provider recognition and contact matching
- Spending analytics by category/provider

## Key Technical Components

### 1. Core Architecture (Offline-First)
```
app/
├── data/
│   ├── local/           # SMS processing & DataStore (for preferences)
│   ├── db/              # Room Database: DAOs, Entities, TypeConverters
│   ├── model/           # Data classes (Transaction, Category)
│   ├── repository/      # Main business logic (interacts with Room & SMS)
│   └── sms/             # SMS receiver implementation
├── domain/
│   ├── usecase/         # Business logic components
│   └── util/            # Helpers (date, strings, contacts)
└── ui/
    ├── add_transaction/ # Manual entry UI
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

### 3. Transaction Handling (Local Persistence)
```kotlin
data class TransactionData(
    val id: String,       // Stable hash of content fields
    val date: Date,
    val amount: Float,
    val isIncome: Boolean,
    val provider: String?,
    val contactName: String?,
    var categoryId: String? // Persisted in Room, mapped to CategoryEntity
)
```
- **ID Generation:** SHA-256 hash of (date, amount, type, provider)
- **Caching:**
  1. Memory cache for quick access (managed by Repositories)
  2. **Room database** serves as the persistent source of truth.
- **Data Storage:** All transaction data is stored in local Room tables.

### 4. Category System (Local Persistence)
- Default categories can be pre-populated in Room on first launch.
- Custom categories are stored in the Room `categories` table.
- "Other" category handling:
  ```kotlin
  fun getUncategorizedCategoryPlaceholder() = Category(
      id = null, // Or a special constant ID for 'Other'
      name = "Other",
      color = Color.GRAY
  )
  ```
- Assignment persistence using `transactionId → categoryId` mapping within the `TransactionEntity`.

### 5. Provider Analysis (Local Persistence)
- Automatic detection from SMS patterns.
- Manual override with persistence:
  ```kotlin
  // Example of how this might be stored, perhaps in a dedicated Provider table or DataStore
  fun saveCustomProviderName(key: String, name: String) {
      // dataStore.edit { preferences -> preferences["provider_$key"] = name }
      // Or, update a ProviderEntity in Room
  }
  ```
- Stats aggregation by normalized provider/contact, queried from Room.

## Local Database Schema (Room)

### `TransactionEntity`
```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,      // SHA-256 hash
    val date: Long,                  // Store as timestamp for easier querying
    val amount: Float,
    val isIncome: Boolean,
    val providerName: String?,       // Derived/Resolved provider name
    val originalSmsSender: String?,  // Raw sender from SMS for reference
    val contactName: String?,        // Matched contact, if any
    val accountInfo: String?,        // Extracted account identifier (e.g., *XXXX)
    var categoryId: String?,         // Foreign key to CategoryEntity
    val rawSmsContent: String?       // Full SMS message text for debugging/reprocessing
)
```

### `CategoryEntity`
```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Auto-generated or predefined
    val name: String,
    val color: Int, // Store as ARGB Int
    val isDefault: Boolean = false // To distinguish user-created from pre-populated
)
```

### `ProviderAliasEntity` (Optional, for managing provider name normalization)
```kotlin
@Entity(tableName = "provider_aliases")
data class ProviderAliasEntity(
    @PrimaryKey val originalName: String, // e.g., "BCO Davivienda", "DAVIVIENDA S.A."
    val normalizedName: String            // e.g., "Davivienda"
)
```

## Key Architectural Patterns (Offline-First)
1. **Single Source of Truth (Room):**
   - `TransactionRepository` and `CategoryRepository` interact primarily with Room DAOs.
   - SMS data is processed and immediately inserted/updated in Room.
   - UI observes LiveData/Flows from Repositories, which are backed by Room queries.

2. **Dependency Injection (Hilt):**
   - Hilt components for ViewModel creation.
   - Repository and DAO dependencies managed via constructor injection.
   - A `DatabaseModule` provides Room database and DAO instances.

3. **Error Resiliency:**
   - SMS processing error handling with potential for failed messages to be stored for retry.
   - Data consistency managed by Room transactions.

## Required Dependencies (for `app/build.gradle.kts`)
```gradle
// Room
def room_version = "2.6.1"
implementation("androidx.room:room-runtime:$room_version")
kapt("androidx.room:room-compiler:$room_version") // Or ksp for Kotlin Symbol Processing
implementation("androidx.room:room-ktx:$room_version")

// DataStore (for app preferences, replacing SharedPreferences)
def datastore_version = "1.1.0"
implementation("androidx.datastore:datastore-preferences:$datastore_version")

// Kotlin Coroutines (essential for Room and modern Android development)
def coroutines_version = "1.8.1"
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

// ViewModel and LiveData (likely already present)
def lifecycle_version = "2.8.0"
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")

// Hilt (likely already present)
def hilt_version = "2.51.1"
implementation("com.google.dagger:hilt-android:$hilt_version")
kapt("com.google.dagger:hilt-compiler:$hilt_version") // Or ksp

// Optional: SQLCipher for database encryption
// implementation("net.zetetic:android-database-sqlcipher:4.5.4")
// implementation("androidx.sqlite:sqlite-ktx:2.4.0") // For SQLCipher support factory
```

## LLM Context Notes
- This document describes a **fully offline application**. All Firebase/cloud features are removed.
- All monetary values in COP (Colombian Pesos).
- Date formats follow `dd/MM/yyyy` in UI, but stored as `Long` (timestamp) in Room.
- Primary SMS patterns from Bancolombia, Nequi, Daviplata remain relevant.
- Transaction IDs are content-hash stable across app restarts.
