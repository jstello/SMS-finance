# Unit Testing in SMS Finance App

This document explains the unit testing setup for the SMS Finance Android application, focusing on the `TransactionListViewModel`.

## Overview

Unit testing is a software development process in which the smallest testable parts of an application, called units, are individually and independently scrutinized for proper operation. In our project, we use unit tests to verify the business logic within our ViewModels, ensuring they behave as expected without needing to run the app on a physical device or emulator.

This approach provides several benefits:
- **Speed**: JVM tests run much faster than instrumentation tests.
- **Reliability**: Tests are isolated and not affected by UI or device-specific issues.
- **Maintainability**: It's easier to pinpoint and fix bugs in isolated components.

## Running the Tests

You can execute the unit tests via the command line using the Gradle wrapper.

### Run a Specific Test File

To run only the tests within a specific file, such as `TransactionListViewModelTest`, use the `--tests` filter. This is highly efficient during development.

```bash
./gradlew :app:testDebugUnitTest --tests="*TransactionListViewModelTest*"
```

**Command Breakdown:**
- `./gradlew`: The Gradle Wrapper script.
- `:app:testDebugUnitTest`: The Gradle task to run unit tests for the `debug` build variant of the `app` module.
- `--tests="*TransactionListViewModelTest*"`: A filter to match and run only the specified test class.

### Run All Unit Tests

To execute the entire suite of unit tests for the `app` module, run the following command:

```bash
./gradlew :app:testDebugUnitTest
```

## Test Implementation Details

Our tests are written in Kotlin and leverage several key libraries and patterns.

### Key Libraries

- **JUnit 4**: The underlying framework for running tests on the JVM.
- **Mockito**: A mocking framework used to create "fake" objects for our ViewModel's dependencies (e.g., `TransactionRepository`). This allows us to isolate the ViewModel and control the behavior of its dependencies during tests.
- **`kotlinx-coroutines-test`**: A library providing tools to test Kotlin Coroutines, which are used extensively in our ViewModels for asynchronous operations.

### Test Structure: Given-When-Then

We structure our tests using the **Given-When-Then** pattern for clarity:

1.  **Given**: Set up the initial state and preconditions for the test. This often involves mocking dependencies to return specific data.
2.  **When**: Execute the specific action or method that is being tested.
3.  **Then**: Verify that the outcome is as expected. This involves checking the ViewModel's state and asserting that the correct methods were called on its dependencies.

### Example Test: `assignCategoryToTransaction success`

Here is an example from `TransactionListViewModelTest.kt`:

```kotlin
@Test
fun `assignCategoryToTransaction success updates state correctly`() = runTest {
    // GIVEN: Setup the test conditions
    // We tell our mock repository to return 'true' when this method is called
    whenever(transactionRepository.assignCategoryToTransaction(eq("1"), eq("cat1")))
        .thenReturn(true)
    
    // Initialize the ViewModel with our mock dependencies
    viewModel = TransactionListViewModel(
        transactionRepository,
        categoryRepository,
        savedStateHandle
    )
    
    // WHEN: Perform the action we want to test
    viewModel.assignCategoryToTransaction("1", "cat1")
    // Ensure all background coroutines complete
    testDispatcher.scheduler.advanceUntilIdle()
    
    // THEN: Verify the outcome is correct
    // Check that the correct repository method was called
    verify(transactionRepository).assignCategoryToTransaction("1", "cat1")
    // Assert that the ViewModel's state has been updated correctly
    assertFalse(viewModel.isAssigningCategory.value)
    assertTrue(viewModel.assignmentResult.value?.isSuccess == true)
}
```

### Mocking Android Framework Dependencies

Unit tests run on a standard JVM, not on an Android device. Therefore, Android framework code (like `android.util.Log`) is not available and will cause tests to crash. To solve this, we've configured our `build.gradle.kts` file to return default values for any Android framework calls made during a unit test:

```kotlin
// in app/build.gradle.kts
android {
    // ...
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}
```

This setting allows our tests to run without crashing, even if the code under test contains Android-specific logging or other framework calls.
