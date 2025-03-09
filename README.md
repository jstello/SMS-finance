# Finanzas Personales

A Colombian SMS-based finance tracker for Bancolombia & Nequi transactions

## ✅ Implemented Features

- **SMS Processing**
  - Reads Bancolombia/Nequi messages
  - Extracts COP amounts with regex
  - Parses dates from message text
  - Detects income via "recibiste" keyword

- **Transaction UI**
  - Material3-styled lists
  - Sort by date/amount (asc/desc)
  - Filter by year/month
  - Basic total calculations

- **Technical Base**
  - Jetpack Compose (Material3 1.2.0)
  - Kotlin 2.0 + JDK 17
  - Gradle Version Catalog
  - SMS permission runtime request

## 📥 Installation

```bash
git clone https://github.com/yourusername/finanzas-personales.git
```
**Requirements:**
- Android Studio 2022.1+
- API 24+ device/emulator

## ⚙️ Code Verified

- **Dependencies**
  ```kotlin
  implementation(libs.androidx.material3) // 1.2.0-alpha11
  implementation(libs.androidx.material.icons.extended)
  ```

- **Architecture**
  - Single Activity
  - Composable-based UI
  - State hoisting
  - Remember/MutableState

## 🚧 Current Limitations

- **Currency**
  - COP only
  - No USD support

- **Analysis**
  - No charts/graphs
  - Basic totals only

- **Localization**
  - Spanish SMS only
  - App UI in Spanish

## 📜 Version History

1.1.0 - Material3 UI, Filter Chips, Sorting  
1.0.1 - Stability fixes, Dependency cleanup  
1.0.0 - Core SMS parsing & list UI 