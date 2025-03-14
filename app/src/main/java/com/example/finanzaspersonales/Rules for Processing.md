# Rules for processing text messages

## Date Parsing Logic

### Extraction Patterns
- Uses regex pattern: `(\d{2}/\d{2}/\d{4}).*?(\d{2}:\d{2}(?::\d{2})?)|(\d{2}:\d{2}(?::\d{2})?).*?(\d{2}/\d{2}/\d{4})`
- Handles two main formats:
  1. Date-first format: `dd/MM/yyyy` followed by time (`HH:mm:ss` or `HH:mm`)
  2. Time-first format: `HH:mm:ss` or `HH:mm` followed by `dd/MM/yyyy`

### Parsing Rules
1. **Date Formats**:
   - Supported date format: `dd/MM/yyyy`
   - Time formats: `HH:mm:ss` or `HH:mm`
   - Uses `SimpleDateFormat` with locale defaults

2. **Order Handling**:
   - Matches date-time combinations in either order:
     - Date first: "22/06/2023 14:30:45"
     - Time first: "14:30 22/06/2023"

3. **Fallback Strategy**:
   - First attempts to parse with seconds (`HH:mm:ss`)
   - Falls back to parsing without seconds (`HH:mm`)
   - Returns null if both parsing attempts fail

4. **Assumptions**:
   - Date and time must appear in the same message body
   - Uses day-month-year order consistently
   - Requires at least hours and minutes in time component
   - Assumes Colombian/Spanish date format conventions

### Storage
- Parsed dates are stored as `java.util.Date` objects
- Used in both `SmsMessage` and `TransactionData` classes
- Displayed in UI as `dd/MM/yy` format

## Amount Detection Logic

### Extraction Patterns
- Uses regex pattern: `(\$|COP)\s*((\d{1,3}(?:[.,]\d{3})*|\d+))(?:([.,])(\d{2}))?`
- Handles formats:
  1. Currency symbol ($ or COP) followed by:
     - Whole numbers: "COP 1.000.000" or "$500,000"
     - Decimal amounts: "COP 150.00" or "$1,250.50"

### Processing Rules
1. **Normalization**:
   - Removes thousand separators: "1.000.000" → "1000000"
   - Converts comma decimals to points: "150,00" → "150.00"
   - Preserves 2 decimal places when present

2. **Validation**:
   - Ignores ".00" endings (treats as whole numbers)
   - Requires currency indicator ($/COP) for detection
   - Prioritizes first match in message body

3. **Conversion**:
   - Uses `parseToFloat` to convert string to numeric value
   - Handles both comma and period decimal separators
   - Returns null for non-numeric values after cleaning

### Assumptions
- Colombian peso (COP) amounts only
- Decimal portion (if present) must be 2 digits
- Thousand separators can be periods or commas
- Currency symbol may be separated by whitespace
- First valid amount in message is considered primary

### Storage
- Original string stored as `amount` property
- Numeric value stored as `numericAmount` property
- Used in `TransactionData` for calculations