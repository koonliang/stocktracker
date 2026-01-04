# Dashboard_07: CSV Import Feature for Transactions

## Overview
Enhance the dashboard page for Transactions to allow import of CSV files for transaction records. The system should intelligently map CSV fields from various brokerage export formats and provide manual mapping options when automatic detection fails.

---

## Feature Requirements

### Core Requirements
1. **File Upload**: Allow users to upload CSV files containing transaction data
2. **Intelligent Field Mapping**: Auto-detect and map CSV columns to standard transaction fields
3. **Manual Mapping UI**: Provide interface for users to manually map fields when auto-detection fails
4. **Preview & Validation**: Show preview of mapped data before import with validation feedback
5. **Batch Import**: Import multiple transactions in a single operation
6. **Error Handling**: Clear feedback on validation errors with ability to fix or skip problematic rows

### Standard Transaction Fields (Target Schema)
Based on existing `Transaction` entity:
- `type` (BUY/SELL) - **required**
- `symbol` - **required**
- `transactionDate` - **required**
- `shares` - **required**
- `pricePerShare` - **required**
- `notes` - optional

---

## Implementation Approach

### Phase 1: Backend CSV Processing Service

#### New Files to Create

**1. DTOs for Import (`dto/request/`)**
```
CsvImportRequest.java
- List<CsvRowData> rows
- Map<String, String> fieldMappings (csvColumn -> standardField)

CsvRowData.java
- Map<String, String> values (columnName -> value)

CsvMappingSuggestionRequest.java
- List<String> headers (CSV column names)
```

**Response DTOs (`dto/response/`)**
```
CsvMappingSuggestionResponse.java
- Map<String, String> suggestedMappings
- Map<String, Double> confidenceScores
- List<String> unmappedColumns

CsvImportPreviewResponse.java
- List<TransactionPreviewRow> validRows
- List<CsvImportError> errors
- int totalRows
- int validCount
- int errorCount

CsvImportResultResponse.java
- int importedCount
- int skippedCount
- List<CsvImportError> errors
- List<TransactionResponse> importedTransactions
```

**2. Service Layer**
```
CsvImportService.java
- suggestMappings(List<String> headers): CsvMappingSuggestionResponse
- previewImport(CsvImportRequest): CsvImportPreviewResponse
- executeImport(Long userId, CsvImportRequest): CsvImportResultResponse
```

**3. Controller Endpoint**
```
TransactionController.java (add endpoints)
- POST /api/transactions/import/suggest-mapping
- POST /api/transactions/import/preview
- POST /api/transactions/import
```

#### Intelligent Field Mapping Algorithm

**Approach: Fuzzy String Matching + Pattern Recognition**

```java
// Common aliases for each standard field
Map<String, List<String>> fieldAliases = {
    "type" -> ["action", "type", "transaction type", "trade type", "buy/sell", "side", "order type"],
    "symbol" -> ["symbol", "ticker", "stock", "security", "instrument", "stock symbol", "ticker symbol"],
    "transactionDate" -> ["date", "trade date", "transaction date", "settlement date", "exec date", "execution date"],
    "shares" -> ["shares", "quantity", "qty", "units", "amount", "volume", "share quantity"],
    "pricePerShare" -> ["price", "share price", "unit price", "execution price", "trade price", "cost per share", "price per share"],
    "notes" -> ["notes", "memo", "description", "comment", "remarks"]
}
```

**Mapping Strategy**:
1. **Exact match** (case-insensitive): confidence = 1.0
2. **Contains match**: If standard field name contained in CSV header, confidence = 0.9
3. **Levenshtein distance**: For fuzzy matching, confidence = 1 - (distance / maxLength)
4. **Pattern recognition**: Detect date formats, numeric patterns to infer field types

**Value Parsing**:
- **Type Detection**: Map common values like "Buy"/"B"/"Purchase" → BUY, "Sell"/"S"/"Sale" → SELL
- **Date Parsing**: Support multiple formats (MM/DD/YYYY, YYYY-MM-DD, DD-MMM-YYYY, etc.)
- **Number Parsing**: Handle currency symbols, commas, negative values

---

### Phase 2: Frontend Implementation

#### New Components

**1. ImportModal.tsx** (Main orchestrator)
```
States:
- UPLOAD: Initial file selection
- MAPPING: Field mapping configuration
- PREVIEW: Review mapped data
- IMPORTING: Progress indicator
- COMPLETE: Success/error summary

Props:
- isOpen: boolean
- onClose: () => void
- onImportComplete: () => void
```

**2. FileUploadStep.tsx**
```
- Drag-and-drop zone
- File input for CSV
- File validation (extension, size limit)
- Parse CSV and extract headers
```

**3. FieldMappingStep.tsx**
```
- Display detected mappings with confidence indicators
- Dropdown selectors for each CSV column
- Visual feedback (green checkmark for mapped, yellow warning for low confidence)
- "Skip this column" option
- Required field validation
```

**4. ImportPreviewStep.tsx**
```
- Table showing first N rows with mapped values
- Validation status per row (valid/error)
- Error details expandable
- Row checkbox to include/exclude
- Summary: "X valid, Y errors, Z to import"
```

**5. ImportProgressStep.tsx**
```
- Progress bar
- Current status text
- Cancel option (if supported)
```

**6. ImportResultStep.tsx**
```
- Success count
- Error summary with downloadable error report
- "Import More" / "Done" actions
```

#### API Integration (`services/api/transactionApi.ts`)

```typescript
// Add to transactionApi
suggestMapping: async (headers: string[]): Promise<MappingSuggestionResponse>
previewImport: async (request: ImportPreviewRequest): Promise<ImportPreviewResponse>
importTransactions: async (request: ImportRequest): Promise<ImportResultResponse>
```

#### UI Integration Points

**Transactions.tsx**:
- Add "Import CSV" button in toolbar (next to filters)
- Trigger ImportModal on click
- Refresh transaction list on successful import

**Dashboard.tsx** (optional):
- Could add quick import button in transaction card

---

### Phase 3: Brokerage-Specific Presets (Enhancement)

**Known Brokerage Formats**:

| Brokerage | Date Format | Type Values | Notable Columns |
|-----------|-------------|-------------|-----------------|
| Fidelity | MM/DD/YYYY | "YOU BOUGHT"/"YOU SOLD" | "Settlement Date", "Symbol", "Quantity", "Price" |
| Charles Schwab | MM/DD/YYYY | "Buy"/"Sell" | "Date", "Symbol", "Quantity", "Price" |
| TD Ameritrade | MM/DD/YYYY | "BUY"/"SELL" | "DATE", "SYMBOL", "QUANTITY", "PRICE" |
| E*Trade | MM/DD/YYYY | "Bought"/"Sold" | "TransactionDate", "Symbol", "Quantity", "Amount" |
| Robinhood | YYYY-MM-DD | "buy"/"sell" | "Activity Date", "Instrument", "Quantity", "Price" |
| Interactive Brokers | YYYYMMDD | "BOT"/"SLD" | "TradeDate", "Symbol", "Quantity", "T. Price" |
| Vanguard | MM/DD/YYYY | "Buy"/"Sell" | "Trade Date", "Symbol", "Shares", "Share Price" |

**Implementation**:
- Store preset configurations in database or config file
- Auto-detect brokerage from CSV header patterns
- Apply preset mappings when detected

---

## Detailed Component Designs

### ImportModal State Machine

```
┌─────────┐     upload      ┌─────────┐    confirm    ┌─────────┐
│ UPLOAD  │ ───────────────>│ MAPPING │──────────────>│ PREVIEW │
└─────────┘                 └─────────┘               └─────────┘
     ^                           │                         │
     │                           │ back                    │ confirm
     │                           v                         v
     │                      ┌─────────┐              ┌───────────┐
     └──────────────────────│ (back)  │              │ IMPORTING │
                            └─────────┘              └───────────┘
                                                          │
                                                          v
                                                    ┌──────────┐
                                                    │ COMPLETE │
                                                    └──────────┘
```

### Field Mapping UI Mockup

```
┌─────────────────────────────────────────────────────────────┐
│ Map CSV Columns to Transaction Fields                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  CSV Column          →    Transaction Field    Confidence   │
│  ─────────────────────────────────────────────────────────  │
│  "Trade Date"        →    [Transaction Date ▼]    ✓ 95%    │
│  "Ticker"            →    [Symbol ▼]              ✓ 90%    │
│  "Action"            →    [Type (BUY/SELL) ▼]     ✓ 85%    │
│  "Qty"               →    [Shares ▼]              ✓ 80%    │
│  "Execution Price"   →    [Price Per Share ▼]     ⚠ 70%    │
│  "Commission"        →    [Skip this column ▼]    —        │
│  "Account"           →    [Skip this column ▼]    —        │
│                                                             │
│  Required fields: ✓ Type  ✓ Symbol  ✓ Date  ✓ Shares  ✓ Price │
│                                                             │
│  [← Back]                              [Preview Import →]   │
└─────────────────────────────────────────────────────────────┘
```

### Preview Table Design

```
┌──────────────────────────────────────────────────────────────────────────┐
│ Preview Import (25 transactions)                                         │
├──────────────────────────────────────────────────────────────────────────┤
│ ☑ │ Status │ Type │ Symbol │ Date       │ Shares │ Price    │ Notes     │
│───┼────────┼──────┼────────┼────────────┼────────┼──────────┼───────────│
│ ☑ │   ✓    │ BUY  │ AAPL   │ 2024-01-15 │ 10     │ $185.50  │           │
│ ☑ │   ✓    │ BUY  │ MSFT   │ 2024-01-16 │ 5      │ $402.25  │           │
│ ☐ │   ✗    │ SELL │ ???    │ Invalid    │ 100    │ $50.00   │ [Expand]  │
│   │        │      │        │            │        │          │ Error:    │
│   │        │      │        │            │        │          │ Invalid   │
│   │        │      │        │            │        │          │ symbol    │
│ ☑ │   ✓    │ SELL │ GOOGL  │ 2024-01-18 │ 2      │ $142.80  │           │
├──────────────────────────────────────────────────────────────────────────┤
│ Summary: 24 valid, 1 error | Selected: 24 transactions                   │
│                                                                          │
│ [← Back to Mapping]                              [Import 24 Transactions]│
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Error Handling Strategy

### Validation Errors (Per Row)
| Error Type | Message | Resolution |
|------------|---------|------------|
| Missing required field | "Symbol is required" | User can edit or skip row |
| Invalid ticker | "XXXX is not a valid ticker" | Validate against Yahoo Finance |
| Invalid date | "Cannot parse date: ..." | Show expected formats |
| Future date | "Transaction date cannot be in the future" | User corrects |
| Negative shares | "Shares must be positive" | User corrects |
| Invalid type | "Type must be BUY or SELL" | Show original value, let user map |

### System Errors
- File too large (set limit, e.g., 5MB or 1000 rows)
- Malformed CSV (encoding issues, missing delimiters)
- Network errors during import
- Partial import failure (show what succeeded)

---

## Testing Strategy

### Backend Tests
1. **Unit Tests for CsvImportService**
   - Field mapping algorithm accuracy
   - Various date format parsing
   - Type value normalization
   - Edge cases (empty rows, special characters)

2. **Integration Tests**
   - Full import flow with mock CSVs
   - Validation enforcement
   - Transaction creation accuracy

### Frontend Tests
1. **Component Tests**
   - Each step component in isolation
   - State transitions in ImportModal

2. **E2E Tests**
   - Happy path: upload → map → preview → import
   - Error handling: invalid file, validation errors
   - Large file handling

### Test Data
Create sample CSV files for:
- Each major brokerage format
- Edge cases (special characters, unicode)
- Various date formats
- Mixed valid/invalid rows

---

## Implementation Order

### Sprint 1: Core Backend
1. Create DTOs
2. Implement CsvImportService with basic mapping
3. Add controller endpoints
4. Unit tests for service

### Sprint 2: Basic Frontend
1. ImportModal with all steps
2. FileUploadStep with CSV parsing (use Papa Parse)
3. FieldMappingStep with dropdown selectors
4. Basic API integration

### Sprint 3: Preview & Import
1. ImportPreviewStep with validation display
2. Backend preview endpoint
3. Batch import execution
4. ImportResultStep

### Sprint 4: Polish & Enhancement
1. Improve mapping algorithm (fuzzy matching)
2. Add brokerage presets
3. Better error messages
4. Performance optimization for large files
5. E2E tests

---

## Dependencies

### Backend
- Already have: Spring Boot, JPA, validation
- May need: OpenCSV or Apache Commons CSV for parsing

### Frontend
- Already have: React, TypeScript, Tailwind
- Need to add: `papaparse` for CSV parsing in browser
- Already have: `react-dropzone` or can use native file input

---

## Security Considerations

1. **File Size Limits**: Enforce max file size (5MB suggested)
2. **Row Limits**: Cap at 1000 transactions per import
3. **Input Sanitization**: Sanitize all parsed values before DB insertion
4. **Rate Limiting**: Prevent abuse of import endpoint
5. **Validation**: All imported transactions go through same validation as manual entry

---

## Future Enhancements (Out of Scope for Initial)

1. **Save Mapping Templates**: Let users save custom mappings for reuse
2. **Import History**: Track past imports with undo capability  
3. **Duplicate Detection**: Warn if transaction appears to be duplicate
4. **Multi-file Import**: Merge multiple CSV files in one session
5. **Export to CSV**: Complement import with export functionality
6. **Scheduled Imports**: Connect to brokerage APIs for automatic sync
