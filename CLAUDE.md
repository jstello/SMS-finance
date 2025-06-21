# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SMS Finance is an Android application that automatically tracks Colombian financial transactions from SMS messages (Bancolombia, Nequi). All data is stored locally using Room database with offline-first architecture. The app features transaction categorization, provider recognition, spending analytics, and visualization charts.

## Essential Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run on connected device/emulator
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

### Development Commands
```bash
# Check for lint issues
./gradlew lint

# Check dependencies
./gradlew dependencies

# Generate debug APK
./gradlew assembleDebug

# Generate release APK (requires keystore.properties)
./gradlew assembleRelease
```

## Architecture Overview

### Data Layer (Offline-First)
- **Room Database**: Primary data storage with `TransactionEntity` and `CategoryEntity`
- **SharedPreferences**: Provider-category mapping rules via `SharedPrefsManager`
- **SMS Processing**: Real-time SMS parsing through `SmsReceiver` and `SmsDataSource`
- **Repositories**: `TransactionRepositoryImpl` and `CategoryRepositoryImpl` manage data access

### Domain Layer
- **Use Cases**: 
  - `ExtractTransactionDataUseCase`: Converts SMS to transaction data
  - `CategoryAssignmentUseCase`: Applies categorization rules
  - `GetSpendingByCategoryUseCase`: Calculates spending summaries
  - `GetSpendingHistoryUseCase`: Provides time-series data for charts
- **Utilities**: Date parsing, text extraction, contact lookup

### UI Layer (Jetpack Compose)
- **Key Screens**: Dashboard, Transaction List, Categories, Providers, Visualizations
- **Architecture**: Single Activity with Compose navigation
- **State Management**: ViewModels with StateFlow/LiveData
- **Dependency Injection**: Hilt for all components

## Key Implementation Details

### Transaction Processing Pipeline
1. **SMS Detection**: Filter financial SMS from Bancolombia/Nequi
2. **Data Extraction**: Parse amount (COP), date, provider, account info
3. **Provider Resolution**: Contact lookup or pattern matching
4. **Category Assignment**: Rule-based categorization
5. **Room Storage**: Persist with stable MD5-based IDs

### Database Schema
- `transactions` table: ID, amount, date, provider, category, account info
- `categories` table: ID, name, color
- Provider-category rules stored in SharedPreferences

### Testing Strategy
- Unit tests for ViewModels using Mockito
- Focus on `TransactionListViewModel` and domain use cases
- Tests located in `app/src/test/java/com/example/finanzaspersonales/`

## Development Guidelines

### Code Style
- Kotlin with Compose for UI
- Hilt dependency injection throughout
- Repository pattern with Room DAOs
- StateFlow for reactive data
- Material 3 design system

### Key Dependencies
- Kotlin 2.0 + JDK 17 (targeting JVM 11)
- Compose BOM 2024.06.00 + Material3 1.2.1
- Room 2.6.1 for local database
- Hilt 2.51.1 for dependency injection  
- MPAndroidChart v3.1.0 for visualizations
- Vico charts for additional chart types

### SMS Processing Rules
- Primary focus on Bancolombia patterns ("Bancolombia:")
- Amount extraction: COP currency patterns with $ or COP prefix
- Income detection: "recibiste" keyword in Spanish
- Provider extraction: ALL CAPS sequences or contact lookup
- Date extraction: SMS timestamp or parsed from message body

### Testing Requirements
- Run unit tests before committing: `./gradlew test`
- Mock repositories and use cases in ViewModel tests
- Test SMS parsing logic in `ExtractTransactionDataUseCase`
- Verify categorization rules in `CategoryAssignmentUseCase`

## File Structure Highlights

```
app/src/main/java/com/example/finanzaspersonales/
├── data/
│   ├── local/room/          # Room entities, DAOs, database
│   ├── repository/          # Data access layer
│   └── sms/                 # SMS processing
├── domain/
│   ├── usecase/             # Business logic
│   └── util/                # Utility functions
├── ui/
│   ├── dashboard/           # Main overview screen
│   ├── transaction_list/    # Transaction management
│   ├── categories/          # Category management
│   ├── visualizations/      # Spending charts
│   └── providers/           # Provider analysis
└── di/                      # Hilt modules
```

## Common Issues & Solutions

### Build Issues
- Ensure Android SDK 34 is installed
- Check that `keystore.properties` exists for release builds
- Clean and rebuild if Room schema changes

### SMS Processing
- SMS permissions must be granted at runtime
- Test SMS parsing with `TransactionDebugActivity`
- Provider-category rules are stored in SharedPreferences

### Database
- Room database migrations handled automatically
- Clear app data to reset database during development
- Use `StatsActivity` to verify transaction counts

## Current Branch Context
Working on `fix-filtering-providers-transactions` branch focusing on transaction filtering and provider management improvements.