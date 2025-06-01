# LLM Tool-Calling Setup for Finanzas Personales

## Overview

This document outlines the implementation of LLM tool-calling functionality in the Finanzas Personales app. We've started with a **Transaction Analysis Tool** that wraps existing SMS processing capabilities in an LLM-compatible interface.

## Why Start with Transaction Analysis?

1. **Existing Functionality**: Leverages your robust `TextExtractors` utility
2. **Clear Value**: LLMs can help improve transaction categorization and provider detection
3. **Self-Contained**: Independent functionality that's easy to test
4. **JSON Interface**: Perfect for LLM tool-calling patterns

## Implementation Structure

### 1. Core Tool Class: `TransactionAnalysisTool`

**Location**: `app/src/main/java/com/example/finanzaspersonales/domain/tools/TransactionAnalysisTool.kt`

**Key Features**:
- JSON input/output for LLM compatibility
- Wraps existing `TextExtractors` functionality
- Provides confidence scores and analysis notes
- Multiple analysis modes (full, provider, amount, classification)
- Error handling with graceful fallbacks

**Main Functions**:
```kotlin
// Primary tool function - designed for LLM calling
fun analyzeTransaction(inputJson: String): String

// Simplified functions for specific tasks
fun extractProvider(smsText: String): String
fun extractAmount(smsText: String): String

// Tool metadata for LLM integration
fun getToolMetadata(): String
```

### 2. Test Suite: `TransactionAnalysisToolTest`

**Location**: `app/src/test/java/com/example/finanzaspersonales/domain/tools/TransactionAnalysisToolTest.kt`

**Purpose**: Demonstrates tool usage independent of LLMs, showing:
- How to construct JSON inputs
- Expected output formats
- Error handling
- LLM tool-calling patterns

### 3. Debug UI: `ToolTestingScreen`

**Location**: `app/src/main/java/com/example/finanzaspersonales/ui/debug/ToolTestingScreen.kt`

**Purpose**: Interactive testing interface for developers to:
- Test tool functionality with real SMS text
- Experiment with different analysis types
- View tool metadata
- Understand JSON input/output formats

### 4. Dependency Injection

**Updated**: `app/src/main/java/com/example/finanzaspersonales/di/AppModule.kt`

Added providers for:
- `Gson` for JSON serialization
- `TransactionAnalysisTool` as a singleton

## How LLMs Would Use This Tool

### 1. Tool Discovery
LLM calls `getToolMetadata()` to understand available functions:

```json
{
  "name": "transaction_analysis",
  "description": "Analyzes SMS text to extract financial transaction information",
  "parameters": {
    "type": "object",
    "properties": {
      "sms_text": {
        "type": "string",
        "description": "The SMS message text to analyze"
      },
      "analysis_type": {
        "type": "string",
        "enum": ["full", "provider", "amount", "classification"],
        "description": "Type of analysis to perform"
      },
      "include_contact_lookup": {
        "type": "boolean",
        "description": "Whether to perform contact name lookup"
      }
    },
    "required": ["sms_text"]
  }
}
```

### 2. Tool Execution
LLM constructs function call:

```json
{
  "sms_text": "Bancolombia: Pagaste $75000 a UBER desde producto *9876",
  "analysis_type": "full",
  "include_contact_lookup": false
}
```

### 3. Tool Response
Tool returns structured data:

```json
{
  "is_valid_transaction": true,
  "is_promotional": false,
  "extracted_amount": "$75000",
  "numeric_amount": 75000.0,
  "detected_provider": "UBER",
  "is_income": false,
  "account_info": {
    "detected_account": null,
    "source_account": "*9876"
  },
  "contact_name": null,
  "phone_number": null,
  "confidence_score": 0.9,
  "analysis_notes": [
    "Amount extracted: $75000",
    "Provider detected: UBER",
    "Transaction type: Expense"
  ]
}
```

## Testing the Tool

### Run Unit Tests
```bash
./gradlew test --tests TransactionAnalysisToolTest
```

### Use Debug Screen
1. Navigate to Debug section in app
2. Access "Tool Testing" screen
3. Input SMS text and test different analysis modes
4. View JSON responses

### Manual Testing Examples

**Expense Transaction**:
```
Input: "Bancolombia: Pagaste $45000 a RAPPI desde producto *1234"
Expected: Valid transaction, RAPPI provider, $45000 amount, expense type
```

**Income Transaction**:
```
Input: "Bancolombia: Recibiste $120000 de JUAN PEREZ a tu cuenta *5678"
Expected: Valid transaction, JUAN PEREZ provider, $120000 amount, income type
```

**Promotional Message**:
```
Input: "Bancolombia: ¡Aprovecha nuestra promoción! Visita www.bancolombia.com"
Expected: Invalid transaction, promotional flag true, zero confidence
```

## Next Steps for LLM Integration

### 1. Additional Tools to Develop

**Category Suggestion Tool**:
- Input: Transaction data
- Output: Suggested categories with confidence scores
- Uses existing `CategoryAssignmentUseCase` logic

**Spending Analysis Tool**:
- Input: Date range, category filters
- Output: Spending patterns and insights
- Uses existing `GetSpendingByCategoryUseCase`

**Provider Normalization Tool**:
- Input: Raw provider names
- Output: Normalized/standardized provider names
- Could improve provider-category mapping accuracy

### 2. LLM Integration Options

**Local LLM Integration**:
- Use libraries like `llama.cpp` for Android
- Keep processing on-device for privacy
- Integrate with existing Room database

**Cloud LLM Integration**:
- OpenAI API, Anthropic Claude, or Google Gemini
- Requires network connectivity
- Consider data privacy implications

**Hybrid Approach**:
- Local tools for sensitive data processing
- Cloud LLMs for complex reasoning tasks
- Best of both worlds

### 3. Tool Registry Pattern

Create a central registry for all tools:

```kotlin
object ToolRegistry {
    private val tools = mapOf(
        "transaction_analysis" to TransactionAnalysisTool::class,
        "category_suggestion" to CategorySuggestionTool::class,
        "spending_analysis" to SpendingAnalysisTool::class
    )
    
    fun getAvailableTools(): List<String>
    fun getToolMetadata(toolName: String): String
    fun executeTool(toolName: String, input: String): String
}
```

### 4. Security Considerations

- **Input Validation**: Sanitize all JSON inputs
- **Rate Limiting**: Prevent abuse of tool endpoints
- **Data Privacy**: Ensure sensitive financial data stays local when possible
- **Error Handling**: Never expose internal system details in error messages

## Benefits of This Approach

1. **Incremental Development**: Start simple, add complexity gradually
2. **Reusable Logic**: Leverages existing, tested SMS processing code
3. **Testable**: Each tool can be tested independently
4. **Flexible**: Works with any LLM that supports function calling
5. **Privacy-Friendly**: Can run entirely on-device if needed

## Conclusion

The `TransactionAnalysisTool` provides a solid foundation for LLM tool-calling in your app. It demonstrates how to:
- Wrap existing functionality in LLM-compatible interfaces
- Provide structured JSON input/output
- Handle errors gracefully
- Test tool functionality independently

This approach sets the stage for expanding tool-calling capabilities while maintaining the robustness and privacy of your existing financial data processing. 