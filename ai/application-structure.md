# Application Structure Summary

This document outlines the structure of the Finanzas Personales Android application based on the contents of the `app` directory.

```
app/
├── .gradle/
├── .gitignore
├── build/
├── build.gradle.kts
├── google-services.json
├── proguard-rules.pro
└── src/
    ├── androidTest
    │   └── java
    │       └── com
    │           └── example
    │               └── finanzaspersonales
    │                   └── ExampleInstrumentedTest.kt
    ├── main
    │   ├── AndroidManifest.xml
    │   ├── ic_launcher-playstore.png
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── finanzaspersonales
    │   │               ├── FinanzasApp.kt
    │   │               ├── data
    │   │               │   ├── auth
    │   │               │   │   ├── AuthRepository.kt
    │   │               │   │   └── AuthRepositoryImpl.kt
    │   │               │   ├── local
    │   │               │   │   ├── SharedPrefsManager.kt
    │   │               │   │   └── SmsDataSource.kt
    │   │               │   ├── model
    │   │               │   │   ├── AccountInfo.kt
    │   │               │   │   ├── Category.kt
    │   │               │   │   ├── SmsMessage.kt
    │   │               │   │   └── TransactionData.kt
    │   │               │   ├── repository
    │   │               │   │   ├── CategoryRepository.kt
    │   │               │   │   ├── CategoryRepositoryImpl.kt
    │   │               │   │   ├── TransactionRepository.kt
    │   │               │   │   └── TransactionRepositoryImpl.kt
    │   │               │   └── sms
    │   │               │       └── SmsReceiver.kt
    │   │               ├── domain
    │   │               │   ├── usecase
    │   │               │   │   ├── CategoryAssignmentUseCase.kt
    │   │               │   │   └── ExtractTransactionDataUseCase.kt
    │   │               │   └── GetSpendingByCategoryUseCase.kt
    │   │               │   └── util
    │   │               │       ├── ContactsUtil.kt
    │   │               │       ├── DateTimeUtils.kt
    │   │               │       ├── StringUtils.kt
    │   │               │       └── TextExtractors.kt
    │   │               └── ui
    │   │                   ├── add_transaction
    │   │                   │   ├── AddTransactionActivity.kt
    │   │                   │   ├── AddTransactionScreen.kt // Allows manual input of Amount, Provider, Date, Type, Category.
    │   │                   │   └── AddTransactionViewModel.kt
    │   │                   ├── auth
    │   │                   │   ├── AuthViewModel.kt
    │   │                   │   └── LoginScreen.kt
    │   │                   ├── categories
    │   │                   │   ├── CategoriesActivity.kt
    │   │                   │   ├── CategoriesScreen.kt
    │   │                   │   ├── CategoriesViewModel.kt
    │   │                   │   ├── CategoryDetailScreen.kt
    │   │                   │   ├── CategoryEditScreen.kt
    │   │                   │   └── TransactionDetailScreen.kt
    │   │                   ├── dashboard
    │   │                   │   ├── DashboardActivity.kt
    │   │                   │   ├── DashboardViewModel.kt
    │   │                   │   └── DashboardViewModelFactory.kt
    │   │                   ├── providers
    │   │                   │   ├── ProvidersActivity.kt
    │   │                   │   ├── ProvidersScreen.kt
    │   │                   │   ├── ProvidersViewModel.kt
    │   │                   │   └── ProvidersViewModelFactory.kt
    │   │                   ├── theme
    │   │                   │   ├── Color.kt
    │   │                   │   ├── Theme.kt
    │   │                   │   └── Type.kt
    │   │                   └── transaction_list
    │   │                       ├── TransactionListActivity.kt
    │   │                       ├── TransactionListScreen.kt
    │   │                       ├── TransactionListViewModel.kt
    │   │                       └── TransactionListViewModelFactory.kt
    │   └── res
    │       ├── drawable
    │       │   ├── ic_launcher_background.xml
    │       │   └── ic_launcher_foreground.xml
    │       ├── layout
    │       │   └── activity_sms_permission.xml
    │       ├── menu
    │       │   └── menu_main.xml
    │       ├── mipmap-anydpi-v26
    │       │   ├── ic_launcher.xml
    │       │   └── ic_launcher_round.xml
    │       ├── mipmap-hdpi
    │       │   ├── ic_launcher.webp
    │       │   └── ic_launcher_round.webp
    │       ├── mipmap-mdpi
    │       │   ├── ic_launcher.webp
    │       │   └── ic_launcher_round.webp
    │       ├── mipmap-xhdpi
    │       │   ├── ic_launcher.webp
    │       │   └── ic_launcher_round.webp
    │       ├── mipmap-xxhdpi
    │       │   ├── ic_launcher.webp
    │       │   └── ic_launcher_round.webp
    │       ├── mipmap-xxxhdpi
    │       │   ├── ic_launcher.webp
    │       │   └── ic_launcher_round.webp
    │       ├── raw
    │       │   ├── cancel_sound.wav
    │       │   ├── confirm_sound.wav
    │       │   ├── select_sound.wav
    │       │   └── tap_sound.wav
    │       ├── values
    │       │   ├── colors.xml
    │       │   ├── ic_launcher_background.xml
    │       │   ├── strings.xml
    │       │   └── themes.xml
    │       └── xml
    │           ├── backup_rules.xml
    │           └── data_extraction_rules.xml
    └── test
        └── java
            └── com
                └── example
                    └── finanzaspersonales
                        └── ExampleUnitTest.kt
```

## Top-Level Files and Directories (`app/`)

*   `build.gradle.kts`: The main build script for the application module, using Kotlin DSL. Defines dependencies, plugins, and build configurations.
*   `google-services.json`: Configuration file for integrating Firebase services.
*   `proguard-rules.pro`: Rules for ProGuard/R8, used for code shrinking and obfuscation.
*   `.gitignore`: Specifies intentionally untracked files that Git should ignore.
*   `src/`: Contains the application's source code and resources.
*   `build/`: Directory containing build outputs (generated by the build process).
*   `.gradle/`: Directory containing Gradle build system files and cache.

## Source Code (`app/src/main/`)

*   `AndroidManifest.xml`: The core manifest file describing essential information about the app to the Android system (permissions, components, features, etc.).
*   `java/`: Contains the Java/Kotlin source code.
    *   `com/example/finanzaspersonales/`: The main package for the application code.
        *   `data/`: Contains data source implementations (local, remote, SMS receiver), repositories, and data models.
        *   `domain/`: Contains business logic (use cases) and utility classes.
        *   `usecase/`: Contains specific business logic operations, encapsulating interactions between repositories (e.g., `CategoryAssignmentUseCase`, `ExtractTransactionDataUseCase`, `GetSpendingByCategoryUseCase`).
        *   `util/`: Contains helper classes for common tasks like date/time manipulation, contact fetching, string operations, and text extraction.
        *   `ui/`: Contains UI-related code (Activities, Composables, ViewModels, themes), organized by feature (accounts, auth, categories, dashboard, providers, sms, transaction_list).
        *   `FinanzasApp.kt`: The custom `Application` class, used for application-level initialization.
*   `res/`: Contains application resources (drawables, layouts, menus, icons, values, XML).
*   `ic_launcher-playstore.png`: The high-resolution launcher icon for the Play Store listing.

## Summary

The project follows a standard Android structure with Gradle (Kotlin DSL) for building. It adopts a layered architecture (`data`, `domain`, `ui`) within the main package `com.example.finanzaspersonales`.
Recent additions include:
*   User registration functionality (email/password sign-up).
*   The ability to manually add transactions via the new `ui/add_transaction` feature module (includes Amount, Provider, Date, Type, Category fields).
*   Functionality to delete incorrectly parsed transactions from the category detail view.
*   Refactored repositories to break a dependency cycle: removed direct dependency of `CategoryRepositoryImpl` on `TransactionRepository`. Methods requiring both repositories (like calculating spending by category) are moved into dedicated Use Cases (e.g., `GetSpendingByCategoryUseCase`).

Key structural improvements made previously:
*   SMS-related code (`SmsReceiver`, `SmsPermissionActivity`) moved into appropriate `data` and `ui` layers (`data/sms`, `ui/sms`).
*   `CategoriesActivity` moved into the `ui/categories` package.
*   Legacy/temporary files (`.py`, `.txt`) removed from the source set.
*   Empty top-level directories (`components`, `routes`) removed.
*   Documentation files (`.md`) previously in the source set are located in the `ai/` directory.

The structure is now cleaner and better adheres to layered architecture principles, improving modularity and maintainability. 

- **Repository**
  - `repository/TransactionRepositoryImpl.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/data/repository/TransactionRepositoryImpl.kt): Extracts, caches, and assigns categories to transactions.
    - Manages an in-memory cache (`cachedTransactions`).
    - The `getTransactions(forceRefresh: Boolean)` method implements a cache-first strategy:
      - If `forceRefresh = false` and the cache is populated, a processed copy of the cache is returned.
      - If `forceRefresh = true` or the cache is empty, it performs a full data retrieval:
        1. Fetches transactions from Firestore.
        2. Scans all local SMS messages (internally calling `refreshSmsData(0)` which processes SMS and updates its portion of the cache).
        3. Merges the Firestore and SMS-derived transactions.
        4. Applies category assignments (from SharedPreferences) and provider fallbacks.
        5. Updates the main `cachedTransactions` with this merged and processed list.
  - `repository/CategoryRepositoryImpl.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/data/repository/CategoryRepositoryImpl.kt): Loads categories, assigns categories to transactions, calculates spending.
    - Manages category persistence (SharedPreferences as fallback, with Firestore as primary for logged-in users).
    - Provides `getUncategorizedCategoryPlaceholder(): Category` which returns a standard `Category` object (e.g., with `id = null`, `name = "Other"`) to represent uncategorized transactions.

- **Domain Layer** (`app/src/main/java/com/example/finanzaspersonales/domain`)
  - `usecase/ExtractTransactionDataUseCase.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/domain/usecase/ExtractTransactionDataUseCase.kt): Converts SMS into `TransactionData`.
  - `usecase/CategoryAssignmentUseCase.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/domain/usecase/CategoryAssignmentUseCase.kt): Rule‐based category assignment.
  - `usecase/GetSpendingByCategoryUseCase.kt`: (Path not provided) Calculates total spending for each category.
    - Note: Should be updated to use `CategoryRepository.getUncategorizedCategoryPlaceholder()` when creating the summary for uncategorized transactions to ensure consistency with how "Other" is handled in detail views.
  - `util/DateTimeUtils.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/domain/util/DateTimeUtils.kt): Date helpers.

- **UI Layer** (`app/src/main/java/com/example/finanzaspersonales/ui`)
  - **Dashboard**
    - `ui/dashboard/DashboardViewModel.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/dashboard/DashboardViewModel.kt)
      - Note: Should be updated to use the cache-first approach (`getTransactions(forceRefresh = false)` for initial load, `getTransactions(forceRefresh = true)` for explicit refresh) similar to `CategoriesViewModel`.
  - **Categories**
    - `ui/categories/CategoriesActivity.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/categories/CategoriesActivity.kt)
    - `ui/categories/CategoriesViewModel.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/categories/CategoriesViewModel.kt)
      - Implements cache-first loading: calls `transactionRepository.getTransactions(forceRefresh = false)` for initial data population.
      - Explicit refresh actions (e.g., refresh button) call `transactionRepository.getTransactions(forceRefresh = true)` to get fresh data.
      - Uses `categoryRepository.getUncategorizedCategoryPlaceholder()` to identify and fetch details for the "Other" category (i.e., transactions with `categoryId = null` or empty).
    - `ui/categories/TransactionDetailScreen.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/categories/TransactionDetailScreen.kt)
  - **Transactions List**
    - `ui/transaction_list/TransactionListActivity.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/transaction_list/TransactionListActivity.kt)
    - `ui/transaction_list/TransactionListViewModel.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/transaction_list/TransactionListViewModel.kt)
    - `ui/transaction_list/TransactionListScreen.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/transaction_list/TransactionListScreen.kt)
    - `ui/transaction_list/TransactionListViewModel.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/transaction_list/TransactionListViewModel.kt)
    - `ui/transaction_list/TransactionListViewModelFactory.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/transaction_list/TransactionListViewModelFactory.kt)
    - `ui/transaction_list/TransactionListScreen.kt` (mdc:app/src/main/java/com/example/finanzaspersonales/ui/transaction_list/TransactionListScreen.kt)

The structure is now cleaner and better adheres to layered architecture principles, improving modularity and maintainability. 