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
    │   │                   │   ├── AddTransactionScreen.kt
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
*   The ability to manually add transactions via the new `ui/add_transaction` feature module.
*   Functionality to delete incorrectly parsed transactions from the category detail view.
*   Refactored repositories to break a dependency cycle: removed direct dependency of `CategoryRepositoryImpl` on `TransactionRepository`. Methods requiring both repositories (like calculating spending by category) are moved into dedicated Use Cases (e.g., `GetSpendingByCategoryUseCase`).

Key structural improvements made previously:
*   SMS-related code (`SmsReceiver`, `SmsPermissionActivity`) moved into appropriate `data` and `ui` layers (`data/sms`, `ui/sms`).
*   `CategoriesActivity` moved into the `ui/categories` package.
*   Legacy/temporary files (`.py`, `.txt`) removed from the source set.
*   Empty top-level directories (`components`, `routes`) removed.
*   Documentation files (`.md`) previously in the source set are located in the `ai/` directory.

The structure is now cleaner and better adheres to layered architecture principles, improving modularity and maintainability. 