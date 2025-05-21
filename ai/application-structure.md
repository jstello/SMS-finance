# Application Structure Summary

This document outlines the structure of the Finanzas Personales Android application based on the contents of the `app` directory.

```
app/
├── build.gradle.kts
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
    │   │               │   ├── db
    │   │               │   │   ├── dao
    │   │               │   │   │   ├── CategoryDao.kt
    │   │               │   │   │   ├── ProviderAliasDao.kt
    │   │               │   │   │   └── TransactionDao.kt
    │   │               │   │   ├── entity
    │   │               │   │   │   ├── CategoryEntity.kt
    │   │               │   │   │   ├── ProviderAliasEntity.kt
    │   │               │   │   │   └── TransactionEntity.kt
    │   │               │   │   ├── mapper
    │   │               │   │   │   ├── CategoryMapper.kt
    │   │               │   │   │   └── TransactionMapper.kt
    │   │               │   │   └── FinanzasDatabase.kt
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
    │   │               ├── di
    │   │               │   ├── AppModule.kt
    │   │               │   ├── DatabaseModule.kt
    │   │               │   └── RepositoryModule.kt
    │   │               ├── domain
    │   │               │   ├── usecase
    │   │               │   │   ├── CategoryAssignmentUseCase.kt
    │   │               │   │   ├── ExtractTransactionDataUseCase.kt
    │   │               │   │   └── GetSpendingByCategoryUseCase.kt
    │   │               │   └── util
    │   │               │       ├── ContactsUtil.kt
    │   │               │       ├── DateTimeUtils.kt
    │   │               │       ├── StringUtils.kt
    │   │               │       └── TextExtractors.kt
    │   │               └── ui
    │   │                   ├── add_transaction
    │   │                   │   ├── AddTransactionActivity.kt
    │   │                   │   ├── AddTransactionScreen.kt
    │   │                   │   └── AddTransactionViewModel.kt
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
    │   │                   ├── raw_sms_list
    │   │                   │   ├── RawSmsListActivity.kt
    │   │                   │   ├── RawSmsListScreen.kt
    │   │                   │   └── RawSmsListViewModel.kt
    │   │                   ├── settings
    │   │                   │   ├── SettingsActivity.kt
    │   │                   │   ├── SettingsScreen.kt
    │   │                   │   └── SettingsViewModel.kt
    │   │                   ├── sms
    │   │                   │   └── SmsPermissionActivity.kt
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
*   `proguard-rules.pro`: Rules for ProGuard/R8, used for code shrinking and obfuscation.
*   `.gitignore`: Specifies intentionally untracked files that Git should ignore.
*   `src/`: Contains the application's source code and resources.
*   `build/`: Directory containing build outputs (generated by the build process).
*   `.gradle/`: Directory containing Gradle build system files and cache.

## Source Code (`app/src/main/`)

*   `AndroidManifest.xml`: The core manifest file describing essential information about the app to the Android system (permissions, components, features, etc.).
*   `java/`: Contains the Java/Kotlin source code.
    *   `com/example/finanzaspersonales/`: The main package for the application code.
        *   `data/`: Contains data source implementations (local Room DB, SMS receiver), repositories, data models, and DB entities/DAOs/mappers.
        *   `domain/`: Contains business logic (use cases) and utility classes.
        *   `usecase/`: Contains specific business logic operations, encapsulating interactions between repositories (e.g., `CategoryAssignmentUseCase`, `ExtractTransactionDataUseCase`, `GetSpendingByCategoryUseCase`).
        *   `util/`: Contains helper classes for common tasks like date/time manipulation, contact fetching, string operations, and text extraction.
        *   `ui/`: Contains UI-related code (Activities, Composables, ViewModels, themes), organized by feature (categories, dashboard, providers, raw_sms_list, sms, transaction_list).
        *   `FinanzasApp.kt`: The custom `Application` class, used for application-level initialization (e.g., Hilt).
*   `res/`: Contains application resources (drawables, layouts, menus, icons, values, XML).
*   `ic_launcher-playstore.png`: The high-resolution launcher icon for the Play Store listing.

## Summary

The project follows a standard Android structure with Gradle (Kotlin DSL) for building. It adopts a layered architecture (`data`, `domain`, `ui`) within the main package `com.example.finanzaspersonales`.
**Core changes in this iteration involve the removal of all Firebase Authentication and Firestore dependencies, migrating to a Room-based local database for all data persistence.**

Recent additions/changes:
*   **Room Database Integration**:
    *   Entities (`TransactionEntity`, `CategoryEntity`, `ProviderAliasEntity`) defined in `data/db/entity/`.
    *   DAOs (`TransactionDao`, `CategoryDao`, `ProviderAliasDao`) defined in `data/db/dao/`.
    *   Mappers (`TransactionMapper`, `CategoryMapper`) defined in `data/db/mapper/`.
    *   `FinanzasDatabase` class and Hilt `DatabaseModule` for providing DB instances.
*   Repositories (`TransactionRepositoryImpl`, `CategoryRepositoryImpl`) refactored to use Room DAOs instead of Firestore. All Firestore-related methods now throw `UnsupportedOperationException`.
*   Firebase Authentication and related UI (`LoginScreen`, `AuthViewModel`) and data components (`AuthRepository`, `AuthRepositoryImpl`) have been removed.
*   The ability to manually add transactions via the `ui/add_transaction` feature module.
*   Functionality to delete incorrectly parsed transactions from the category detail view (now operates on Room data).
*   Developer Settings screen (`ui/settings`) updated to clear user transactions from Room and resync SMS data.
*   "Raw SMS Transactions" screen (`ui/raw_sms_list`) for displaying raw SMS messages.

Key structural improvements maintained/updated:
*   SMS-related code (`SmsReceiver`, `SmsPermissionActivity`) in appropriate `data` and `ui` layers.
*   Layered architecture (`data`, `domain`, `ui`) is now centered around local Room persistence and SMS processing.
*   Use Cases (`CategoryAssignmentUseCase`, `ExtractTransactionDataUseCase`, `GetSpendingByCategoryUseCase`) operate on Room-backed repositories.

The structure is focused on local data management, improving offline capabilities and simplifying the data layer by removing remote synchronization logic.

- **Data Layer (`app/src/main/java/com/example/finanzaspersonales/data`)**
  - **Database (`data/db`)**
    - `dao/`: Contains Data Access Objects for Room (e.g., `TransactionDao`, `CategoryDao`).
    - `entity/`: Contains Room entity classes (e.g., `TransactionEntity`, `CategoryEntity`).
    - `mapper/`: Contains functions to map between Room entities and domain models.
    - `FinanzasDatabase.kt`: Defines the Room database.
  - **Local (`data/local`)**
    - `SmsDataSource.kt`: Responsible for reading SMS messages from the device.
    - `SharedPrefsManager.kt`: Manages simple key-value storage (e.g., for user preferences, though categories are now in Room).
  - **Repository (`data/repository`)**
    - `TransactionRepositoryImpl.kt`: Manages transaction data, sourcing from `SmsDataSource` and persisting to `TransactionDao`. Assigns categories via `CategoryAssignmentUseCase`.
      - The `getTransactions(forceRefresh: Boolean)` method now primarily relies on Room:
        - If `forceRefresh = true`, it calls `refreshSmsData()` to process new SMS messages and insert/update them in Room via `TransactionDao`.
        - Fetches transactions from `TransactionDao`.
    - `CategoryRepositoryImpl.kt`: Manages category data, persisting to `CategoryDao`.
      - Provides `getUncategorizedCategoryPlaceholder(): Category`.
  - **SMS (`data/sms`)**
    - `SmsReceiver.kt`: Listens for incoming SMS messages to trigger transaction processing.

- **Domain Layer (`app/src/main/java/com/example/finanzaspersonales/domain`)**
  - `usecase/ExtractTransactionDataUseCase.kt`: Converts SMS messages into `TransactionData` domain models.
  - `usecase/CategoryAssignmentUseCase.kt`: Implements rule-based category assignment for transactions.
  - `usecase/GetSpendingByCategoryUseCase.kt`: Calculates total spending for each category using data from repositories.
  - `util/`: Utility classes (DateTime, String manipulation, etc.).

- **DI Layer (`app/src/main/java/com/example/finanzaspersonales/di`)**
    - `AppModule.kt`: Provides application-level dependencies (e.g., UseCases).
    - `DatabaseModule.kt`: Provides Room database and DAO instances.
    - `RepositoryModule.kt`: Provides repository implementations.

- **UI Layer** (`app/src/main/java/com/example/finanzaspersonales/ui`)
  - **Dashboard**
    - `ui/dashboard/DashboardViewModel.kt`: Loads data from `TransactionRepository` and `CategoryRepository` for display.
  - **Categories**
    - `ui/categories/CategoriesViewModel.kt`: Manages category and transaction display, filtering, and interaction, using Room-backed repositories.
      - Uses `categoryRepository.getUncategorizedCategoryPlaceholder()` for "Other" category.
  - Other UI modules (`add_transaction`, `providers`, `raw_sms_list`, `settings`, `transaction_list`) operate on ViewModels that interact with the Room-backed repositories.