# Spending Insights Tool for LLM Integration

## Overview

The **SpendingInsightsTool** is designed to answer real user questions about their finances. Instead of technical SMS parsing, this tool focuses on practical insights that users actually want to know about their spending habits.

## Why This Tool?

This tool addresses **real user needs**:
- "How close am I to my budget for food this month?"
- "Am I spending more on restaurants than last month?"
- "What's my biggest expense category?"
- "Is my total spending higher this month?"

These are the questions users actually ask, making this tool much more valuable for LLM integration.

## Tool Capabilities

### 1. Budget Tracking
**User Question**: *"How close am I to my $600 grocery budget this month?"*

**Tool Response**:
```json
{
  "question_type": "budget_check",
  "answer_summary": "You're on track. You've used 75.0% of your Groceries budget.",
  "current_amount": 450.0,
  "budget_amount": 600.0,
  "percentage_of_budget": 75.0,
  "amount_remaining": 150.0,
  "recommendations": [
    "Great job staying within budget!",
    "You have room for additional Groceries expenses if needed"
  ]
}
```

### 2. Month-to-Month Comparisons
**User Question**: *"Am I spending more on restaurants than last month?"*

**Tool Response**:
```json
{
  "question_type": "monthly_comparison",
  "answer_summary": "Your Restaurants spending increased by 20.0% compared to last month.",
  "current_amount": 300.0,
  "comparison_amount": 250.0,
  "percentage_change": 20.0,
  "trend_direction": "increasing",
  "recommendations": [
    "Consider reviewing what caused the increase in Restaurants spending"
  ]
}
```

### 3. Spending Analysis
**User Question**: *"What's my biggest expense this month?"*

**Tool Response**:
```json
{
  "question_type": "category_analysis",
  "answer_summary": "Your biggest expense this month is Groceries at $450.00 (42.9% of total spending).",
  "current_amount": 1050.0,
  "category_breakdown": [
    {
      "category_name": "Groceries",
      "amount": 450.0,
      "percentage_of_total": 42.9
    },
    {
      "category_name": "Restaurants",
      "amount": 300.0,
      "percentage_of_total": 28.6
    }
  ]
}
```

## Implementation Details

### Core Tool Class: `SpendingInsightsTool`

**Location**: `app/src/main/java/com/example/finanzaspersonales/domain/tools/SpendingInsightsTool.kt`

**Key Methods**:
```kotlin
// Main LLM-callable function
fun getSpendingInsight(inputJson: String): String

// Convenience methods for common questions
fun checkCategoryBudget(categoryName: String, budgetAmount: Float): String
fun compareToLastMonth(categoryName: String? = null): String
fun getCurrentMonthBreakdown(): String
```

### Question Types Supported

1. **`budget_check`** - Compare spending against budgets
2. **`monthly_comparison`** - Compare current vs previous periods
3. **`category_analysis`** - Analyze spending breakdown
4. **`spending_trend`** - Identify spending patterns over time

### Input Format for LLMs

```json
{
  "question_type": "budget_check",
  "category_name": "Groceries",
  "budget_amount": 600.0,
  "time_period": "current_month"
}
```

### Output Format

All responses include:
- **`answer_summary`**: Human-readable answer
- **`current_amount`**: Relevant spending amount
- **`insights`**: Key data points
- **`recommendations`**: Actionable advice

## Testing the Tool

### Interactive Testing
Use the `SpendingInsightsTestScreen` to test real user scenarios:

1. Navigate to Debug section in app
2. Access "Spending Insights Testing" screen
3. Try quick questions or create custom queries
4. View user-friendly answers and recommendations

### Unit Tests
Run comprehensive tests covering real user scenarios:

```bash
./gradlew test --tests SpendingInsightsToolTest
```

### Example Test Scenarios

**Budget Scenarios**:
- Under budget (< 50%)
- On track (50-80%)
- Getting close (80-100%)
- Over budget (> 100%)

**Comparison Scenarios**:
- Spending increased significantly (> 20%)
- Spending decreased (< -5%)
- Spending stable (-5% to +5%)

**Analysis Scenarios**:
- Multiple categories with spending
- Single dominant category
- No spending data

## LLM Integration Examples

### 1. OpenAI Function Calling
```javascript
const tools = [
  {
    type: "function",
    function: {
      name: "spending_insights",
      description: "Get insights about user spending patterns and budgets",
      parameters: {
        type: "object",
        properties: {
          question_type: {
            type: "string",
            enum: ["budget_check", "monthly_comparison", "category_analysis", "spending_trend"]
          },
          category_name: { type: "string" },
          budget_amount: { type: "number" }
        },
        required: ["question_type"]
      }
    }
  }
];
```

### 2. Anthropic Claude Tool Use
```json
{
  "name": "spending_insights",
  "description": "Analyze user spending and provide financial insights",
  "input_schema": {
    "type": "object",
    "properties": {
      "question_type": {
        "type": "string",
        "enum": ["budget_check", "monthly_comparison", "category_analysis", "spending_trend"]
      },
      "category_name": { "type": "string" },
      "budget_amount": { "type": "number" }
    },
    "required": ["question_type"]
  }
}
```

## Real User Conversation Examples

### Example 1: Budget Check
**User**: "How am I doing with my food budget this month? I set aside $600."

**LLM**: I'll check your grocery spending against your $600 budget.

*[Calls tool with budget_check, category="Groceries", budget_amount=600]*

**Tool Response**: You're doing great! You've spent 75.0% of your Groceries budget.

**LLM**: Great news! You've spent $450 out of your $600 grocery budget this month, which is 75% of your budget. You're staying well within your limits and have $150 remaining for the rest of the month. Keep up the good work!

### Example 2: Spending Comparison
**User**: "I feel like I've been eating out more lately. Am I spending more on restaurants than last month?"

**LLM**: Let me compare your restaurant spending this month to last month.

*[Calls tool with monthly_comparison, category="Restaurants"]*

**Tool Response**: Your Restaurants spending increased by 20.0% compared to last month.

**LLM**: Yes, your instinct is correct. You've spent $300 on restaurants this month compared to $250 last month - that's a 20% increase. You might want to review what caused this increase and consider if you want to adjust your dining out habits for the rest of the month.

### Example 3: Spending Overview
**User**: "What's my biggest expense this month?"

**LLM**: I'll analyze your spending breakdown for this month.

*[Calls tool with category_analysis]*

**Tool Response**: Your biggest expense this month is Groceries at $450.00 (42.9% of total spending).

**LLM**: Your biggest expense this month is groceries at $450, which represents about 43% of your total spending. Your other major expenses are restaurants ($300, 29%) and transportation ($200, 19%). This seems like a healthy spending pattern with groceries being your largest expense, which is typical for most households.

## Benefits Over Technical Tools

1. **User-Focused**: Answers questions users actually ask
2. **Actionable**: Provides recommendations, not just data
3. **Contextual**: Understands spending patterns and trends
4. **Conversational**: Responses work well in chat interfaces
5. **Practical**: Helps users make financial decisions

## Next Steps

### Additional Question Types
- **Savings Goals**: "How much should I save this month?"
- **Spending Predictions**: "Will I go over budget this month?"
- **Category Optimization**: "Which expenses should I reduce?"

### Enhanced Analysis
- Seasonal spending patterns
- Unusual spending detection
- Budget recommendations based on income
- Goal tracking and progress

### Integration Options
- Voice assistants (Alexa, Google Assistant)
- Chatbots in banking apps
- Financial planning tools
- Expense tracking notifications

## Conclusion

The SpendingInsightsTool transforms your existing financial data processing into a conversational, user-friendly interface that answers real questions users have about their money. This approach makes LLM integration immediately valuable and practical for end users, rather than just a technical demonstration.

The tool leverages your robust existing infrastructure (`GetSpendingByCategoryUseCase`, category management, transaction processing) while presenting it in a way that's natural for both users and LLMs to interact with. 