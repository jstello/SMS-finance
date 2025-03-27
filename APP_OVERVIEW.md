# FinanzasPersonales2 App Overview

This document provides a comprehensive overview of the project structure and the role of each file and directory in the FinanzasPersonales2 Android application. This personal finance app helps users manage their expenses, track categories, and monitor their financial health.

## Project Root Directory

### Build Configuration Files
- [`build.gradle.kts`](build.gradle.kts) - Root-level build configuration file that defines project-wide settings, including the Android SDK version, build tools version, and project-level dependencies that are shared across all modules
- [`settings.gradle.kts`](settings.gradle.kts) - Configures project settings and includes the app module, defining which modules are part of the project and how they relate to each other
- [`gradle.properties`](gradle.properties) - Contains Gradle-specific properties and settings, such as memory allocation for the build process, Android Studio settings, and other performance configurations
- [`gradlew`](gradlew) and [`gradlew.bat`](gradlew.bat) - Gradle wrapper scripts that ensure consistent builds across different development environments by downloading and using the correct version of Gradle

### Important Directories
- [`app/`](app/) - Main application module containing all the source code, resources, and configurations specific to the FinanzasPersonales2 app
- [`gradle/`](gradle/) - Contains Gradle wrapper and version catalog, ensuring consistent dependency versions across the project
- [`.gradle/`](.gradle/) - Contains Gradle build cache and temporary files, improving build performance by caching compiled code and dependencies
- [`.idea/`](.idea/) - Contains IntelliJ IDEA/Android Studio project settings, including code style, run configurations, and IDE-specific settings
- [`.git/`](.git/) - Version control system directory, tracking all changes to the project files
- [`.kotlin/`](.kotlin/) - Contains Kotlin compiler cache and temporary files, improving compilation performance

### Other Files
- [`local.properties`](local.properties) - Contains local machine-specific settings, primarily the Android SDK path, which is not committed to version control as it varies by developer
- [`.gitignore`](.gitignore) - Specifies which files Git should ignore, preventing unnecessary files from being tracked in version control
- [`README.md`](README.md) - Project documentation and setup instructions, including how to set up the development environment and run the app

## App Module (`app/`)

### Build Configuration
- [`build.gradle.kts`](app/build.gradle.kts) - App-specific build configuration including dependencies, build settings, and app-specific configurations like application ID, version code, and version name
- [`proguard-rules.pro`](app/proguard-rules.pro) - ProGuard rules for code obfuscation and optimization, defining which parts of the code should be kept or modified during the release build process

### Source Code Organization
- [`src/`](app/src/) - Contains the main source code, organized following clean architecture principles
- [`components/`](app/components/) - Reusable UI components that can be shared across different screens, reducing code duplication
- [`routes/`](app/routes/) - Navigation and routing logic, defining how users move between different screens in the app
- [`build/`](app/build/) - Contains build outputs and generated files, including compiled code and resources

## Source Code Structure (`app/src/`)

### Main Source Code (`main/`)
- [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) - Application manifest file defining app configuration, including permissions, activities, services, and other app components
- [`res/`](app/src/main/res/) - Android resources directory containing:
  - Layouts: XML files defining the structure of each screen
  - Strings: Text resources in different languages
  - Drawables: Images, icons, and other visual resources
  - Colors: Color definitions used throughout the app
  - Styles: UI styling definitions
- [`java/`](app/src/main/java/) - Kotlin/Java source code
  - [`com.example.finanzaspersonales/`](app/src/main/java/com/example/finanzaspersonales/) - Main package containing all app code
    - [`CategoriesActivity.kt`](app/src/main/java/com/example/finanzaspersonales/CategoriesActivity.kt) - Main application entry point, implementing the modern, refactored version of the app with clean architecture principles
    - [`MainActivity.kt`](app/src/main/java/com/example/finanzaspersonales/MainActivity.kt) - Legacy entry point (for reference only), containing the original monolithic implementation that was refactored into the current architecture
    - [`ui/`](app/src/main/java/com/example/finanzaspersonales/ui/) - User interface components organized by feature
      - [`categories/`](app/src/main/java/com/example/finanzaspersonales/ui/categories/) - Categories-related UI components for managing expense categories
      - [`transaction/`](app/src/main/java/com/example/finanzaspersonales/ui/transaction/) - Transaction-related UI components for adding and editing transactions
      - [`components/`](app/src/main/java/com/example/finanzaspersonales/ui/components/) - Reusable UI components like buttons, cards, and input fields
      - [`dashboard/`](app/src/main/java/com/example/finanzaspersonales/ui/dashboard/) - Dashboard UI components showing financial overview and statistics
      - [`accounts/`](app/src/main/java/com/example/finanzaspersonales/ui/accounts/) - Account management UI for handling different financial accounts
      - [`home/`](app/src/main/java/com/example/finanzaspersonales/ui/home/) - Home screen UI showing the main overview and quick actions
      - [`transactions/`](app/src/main/java/com/example/finanzaspersonales/ui/transactions/) - Transaction list UI displaying all financial transactions
      - [`theme/`](app/src/main/java/com/example/finanzaspersonales/ui/theme/) - App theme and styling, defining the visual appearance of the app
    - [`domain/`](app/src/main/java/com/example/finanzaspersonales/domain/) - Business logic layer containing the core functionality
      - [`usecase/`](app/src/main/java/com/example/finanzaspersonales/domain/usecase/) - Use cases for business operations, implementing specific features like adding transactions or calculating expenses
      - [`util/`](app/src/main/java/com/example/finanzaspersonales/domain/util/) - Utility functions and helpers for common operations like date formatting and currency calculations
    - [`data/`](app/src/main/java/com/example/finanzaspersonales/data/) - Data layer handling all data operations
      - [`repository/`](app/src/main/java/com/example/finanzaspersonales/data/repository/) - Repository implementations that coordinate between local and remote data sources
      - [`local/`](app/src/main/java/com/example/finanzaspersonales/data/local/) - Local data storage using Room database for offline access
      - [`model/`](app/src/main/java/com/example/finanzaspersonales/data/model/) - Data models and entities representing the app's data structures

### Test Directories
- [`androidTest/`](app/src/androidTest/) - Android instrumented tests that run on an Android device or emulator, testing UI interactions and app behavior
- [`test/`](app/src/test/) - Unit tests that run on the local JVM, testing business logic and data operations

## Gradle Configuration (`gradle/`)

### Version Management
- [`libs.versions.toml`](gradle/libs.versions.toml) - Version catalog file defining dependency versions and libraries, ensuring consistent versions across the project and simplifying dependency management

### Wrapper
- [`wrapper/`](gradle/wrapper/) - Contains Gradle wrapper files that ensure all developers use the same version of Gradle, preventing version-related issues

## Development Tools
- [`.cursor/`](.cursor/) - Cursor IDE specific settings and cache, improving development experience
- [`app_logs.txt`](app_logs.txt) - Application logs for debugging, helping developers track issues and app behavior
- [`material_product_sounds/`](material_product_sounds/) - Sound resources for the application, providing audio feedback for user interactions
- [`material_product_sounds.zip`](material_product_sounds.zip) - Compressed sound resources for efficient storage and distribution

## Build Outputs and Temporary Files
- `hs_err_pid*.log` - JVM crash logs that help diagnose application crashes
- `-p/` - Temporary directory for build processes, containing intermediate build files

## Note
This project uses Kotlin DSL for Gradle build scripts (`.kts` files) instead of the traditional Groovy-based Gradle files, providing better IDE support and type safety. The project follows modern Android development practices with a modular structure and clear separation of concerns. The source code is organized following clean architecture principles with clear separation between UI, domain, and data layers, making the codebase maintainable and testable.

The application has been refactored from a monolithic structure (MainActivity.kt) to a modern, maintainable architecture (CategoriesActivity.kt) following clean architecture principles. The legacy MainActivity.kt is kept for reference purposes only. 