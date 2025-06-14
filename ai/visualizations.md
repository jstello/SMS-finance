# Time Series Visualization Plan

This document outlines the plan for implementing time series visualizations for spending data.

## Progress Checklist

- [x] **Phase 1: Data Preparation & ViewModel**
    - [x] Create a new package `com.example.finanzaspersonales.ui.visualizations`.
    - [x] Create a `VisualizationViewModel`.
    - [x] Implement a `getSpendingHistoryUseCase` in the domain layer.
    - [x] Update `TransactionRepository` to fetch transactions within a date range.
    - [x] Implement logic in the ViewModel to calculate cumulative spending for different time periods (current month, previous month).
- [x] **Phase 2: UI Implementation**
    - [x] Add a charting library dependency (e.g., `com.github.PhilJay:MPAndroidChart`).
    - [x] Create a `CumulativeSpendingChart` Composable.
    - [x] Integrate the chart into a new screen or an existing one.
- [x] **Phase 3: Refinement & Testing**
    - [x] Add loading and error states to the chart.
    - [x] Remove tooltips and value labels from the chart for a cleaner look.
    - [ ] Write unit tests for the ViewModel logic.
    - [x] Perform manual testing on different devices and with different data sets.

## Plan Description

### 1. Understanding the Data

The core data model for our visualizations is `TransactionData`.

```kotlin
data class TransactionData(
    val id: String? = null,
    val userId: String? = null,
    val date: Date,
    val amount: Float,
    val isIncome: Boolean,
    val description: String? = null,
    val provider: String? = null,
    val contactName: String? = null,
    val accountInfo: String? = null,
    var categoryId: String? = null
)
```

The key fields are `date`, `amount`, and `isIncome`. We will be focusing on transactions where `isIncome` is `false` to analyze spending.

### 2. Data Transformations

To create the "Cumulative Spending Comparison" chart, we need to perform the following transformations:

1.  **Fetch Spending Data:**
    *   Query all transactions where `isIncome == false` for the last 12 months.
    *   This required a new method in the `TransactionRepository` (and corresponding `TransactionDao` if using Room) to fetch transactions within a given date range.

2.  **Process Data for Charting:**
    *   This logic resides in the `VisualizationViewModel`.
    *   For a given day of the month `d`:
        *   **Current Month's Cumulative Spending:** Calculate the sum of spending from day 1 to `d` of the current month.
        *   **Previous Month's Cumulative Spending:** Calculate the sum of spending from day 1 to `d` of the previous month.

    *   The result will be two lists of data points, one for each line on the chart (current month, previous month). Each list will have approximately 30 data points (one for each day of the month).

### 3. Visualization Implementation

1.  **Charting Library:** We will need to add a charting library to the project. `MPAndroidChart` is a popular and versatile choice for Android. We'll add the dependency to `app/build.gradle.kts`.

2.  **ViewModel:** A `VisualizationViewModel` will be created to encapsulate the data fetching and transformation logic described above. It will expose the chart data to the UI using `StateFlow` or `LiveData`.

3.  **Composable:** A new Composable function, `CumulativeSpendingChart`, will be created. This Composable will:
    *   Observe the data from the `VisualizationViewModel`.
    *   Use the charting library to render a line chart with two lines.
    *   Handle UI states like loading and empty/error states.
    *   Be placed in a new screen accessible from the main navigation of the app. 