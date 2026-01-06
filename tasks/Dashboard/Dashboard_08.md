# Dashboard_08: Transaction Fees & Export CSV

## Overview
Enhance the Dashboard page Add Transaction form and Manage Transactions edit functionality to include a new optional "Broker/Clearing/Tax" field. The total cost calculation will incorporate this fee. Additionally, add visual indicators for mandatory fields and implement field-level validation with error highlighting. Also implement an Export CSV feature for transaction records and update CSV Import to support broker fee mapping.

---

## Task 1: Add Broker/Clearing/Tax Field to Transaction Forms

### 1.1 Backend Changes

#### 1.1.1 Update Transaction Entity
**File:** `backend/src/main/java/com/stocktracker/entity/Transaction.java`

Add new field:
```java
@PositiveOrZero(message = "Fees cannot be negative")
@Column(name = "broker_fee", precision = 10, scale = 2)
private BigDecimal brokerFee;
```

Update `calculateTotalAmount()` method:
```java
@PrePersist
@PreUpdate
public void calculateTotalAmount() {
    if (shares != null && pricePerShare != null) {
        this.totalAmount = shares.multiply(pricePerShare);
        if (brokerFee != null) {
            this.totalAmount = this.totalAmount.add(brokerFee);
        }
    }
}
```

#### 1.1.2 Update TransactionRequest DTO
**File:** `backend/src/main/java/com/stocktracker/dto/request/TransactionRequest.java`

Add new field:
```java
@PositiveOrZero(message = "Fees cannot be negative")
private BigDecimal brokerFee;
```

#### 1.1.3 Update TransactionResponse DTO
**File:** `backend/src/main/java/com/stocktracker/dto/response/TransactionResponse.java`

Add new field:
```java
private BigDecimal brokerFee;
```

#### 1.1.4 Update TransactionPreviewRow DTO
**File:** `backend/src/main/java/com/stocktracker/dto/response/TransactionPreviewRow.java`

Add new field for CSV import preview:
```java
private BigDecimal brokerFee;
```

#### 1.1.5 Update TransactionService
**File:** `backend/src/main/java/com/stocktracker/service/TransactionService.java`

Update the mapping logic in `createTransaction` and `updateTransaction` methods to include `brokerFee`:
- Set `brokerFee` from request when creating/updating
- Include `brokerFee` in the response mapping

#### 1.1.6 Database Migration
**File:** `backend/src/main/resources/db/migration/V{next}_add_broker_fee.sql`

```sql
ALTER TABLE transactions ADD COLUMN broker_fee DECIMAL(10,2) DEFAULT NULL;
```

---

### 1.2 Frontend Changes

#### 1.2.1 Update Transaction API Types
**File:** `frontend/src/services/api/transactionApi.ts`

Update `TransactionRequest` interface:
```typescript
export interface TransactionRequest {
  type: TransactionType
  symbol: string
  transactionDate: string
  shares: number
  pricePerShare: number
  brokerFee?: number  // NEW: Optional field
  notes?: string
}
```

Update `TransactionResponse` interface:
```typescript
export interface TransactionResponse {
  id: number
  type: TransactionType
  symbol: string
  companyName: string
  transactionDate: string
  shares: number
  pricePerShare: number
  brokerFee: number | null  // NEW
  totalAmount: number
  notes: string | null
  createdAt: string
  updatedAt: string
}
```

Update `TransactionPreviewRow` interface:
```typescript
export interface TransactionPreviewRow {
  rowNumber: number
  type: TransactionType | null
  symbol: string | null
  transactionDate: string | null
  shares: number | null
  pricePerShare: number | null
  brokerFee: number | null  // NEW
  notes: string | null
  valid: boolean
  errors: CsvImportError[]
}
```

#### 1.2.2 Update QuickAddModal (Dashboard Add Transaction)
**File:** `frontend/src/components/transactions/QuickAddModal.tsx`

**State Changes:**
```typescript
const [brokerFee, setBrokerFee] = useState('')
```

**Total Calculation Update:**
```typescript
const totalAmount = shares && price 
  ? parseFloat(shares) * parseFloat(price) + (brokerFee ? parseFloat(brokerFee) : 0)
  : 0
```

**Form Field Addition (after Price field, before Total):**
```tsx
{/* Broker/Clearing/Tax */}
<div>
  <label className="block text-sm font-medium text-slate-700 mb-1">
    Broker/Clearing/Tax
  </label>
  <input
    type="number"
    value={brokerFee}
    onChange={e => setBrokerFee(e.target.value)}
    placeholder="0.00"
    min="0"
    step="0.01"
    className="block w-full rounded-lg border border-slate-300 px-3 py-2
             focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
  />
</div>
```

**Submit Handler Update:**
```typescript
const request: TransactionRequest = {
  type,
  symbol: symbol.toUpperCase(),
  transactionDate: date,
  shares: sharesNum,
  pricePerShare: priceNum,
  brokerFee: brokerFee ? parseFloat(brokerFee) : undefined,
  notes: notes || undefined,
}
```

**Reset Form Update:**
```typescript
setBrokerFee('')
```

#### 1.2.3 Update TransactionForm (Used in TransactionModal)
**File:** `frontend/src/components/transactions/TransactionForm.tsx`

Apply same changes as QuickAddModal:
- Add `brokerFee` state
- Update total calculation
- Add form field
- Update submit handler
- Add to initialData handling

#### 1.2.4 Update TransactionGridRow (Edit Mode in Manage Transactions)
**File:** `frontend/src/components/transactions/TransactionGridRow.tsx`

**State Changes:**
```typescript
const [brokerFee, setBrokerFee] = useState(transaction.brokerFee?.toString() || '')
```

**Total Calculation Update:**
```typescript
const totalAmount = parseFloat(shares || '0') * parseFloat(price || '0') + parseFloat(brokerFee || '0')
```

**Edit Mode Form Update:**
Add broker fee field in the grid (adjust grid columns from `grid-cols-12` as needed):
```tsx
{/* Broker Fee */}
<div className="col-span-2">
  <label className="block text-xs font-medium text-slate-600 mb-1">Fees</label>
  <input
    type="number"
    value={brokerFee}
    onChange={e => setBrokerFee(e.target.value)}
    min="0"
    step="0.01"
    className="w-full rounded border border-slate-300 px-2 py-1.5 text-right text-sm"
  />
</div>
```

**Save Handler Update:**
```typescript
await onSave({
  type,
  symbol,
  transactionDate: date,
  shares: parseFloat(shares),
  pricePerShare: parseFloat(price),
  brokerFee: brokerFee ? parseFloat(brokerFee) : undefined,
})
```

**Display Mode Update:**
Consider adding a column to show broker fees in the transaction table, or include it in the total display.

---

## Task 2: Add Mandatory Field Indicators (Asterisks)

### 2.1 QuickAddModal
**File:** `frontend/src/components/transactions/QuickAddModal.tsx`

Update labels for mandatory fields to include asterisk:
```tsx
<label className="block text-sm font-medium text-slate-700 mb-1">
  Type <span className="text-red-500">*</span>
</label>

<label className="block text-sm font-medium text-slate-700 mb-1">
  Ticker <span className="text-red-500">*</span>
</label>

<label className="block text-sm font-medium text-slate-700 mb-1">
  Date <span className="text-red-500">*</span>
</label>

<label className="block text-sm font-medium text-slate-700 mb-1">
  Shares <span className="text-red-500">*</span>
</label>

<label className="block text-sm font-medium text-slate-700 mb-1">
  Price <span className="text-red-500">*</span>
</label>
```

Optional fields (no asterisk):
- Broker/Clearing/Tax
- Notes

Consider adding a legend at the top or bottom of the form:
```tsx
<p className="text-xs text-slate-500 mb-4">
  <span className="text-red-500">*</span> Required fields
</p>
```

### 2.2 TransactionForm
**File:** `frontend/src/components/transactions/TransactionForm.tsx`

Apply same asterisk pattern to mandatory field labels.

### 2.3 TransactionGridRow (Edit Mode)
**File:** `frontend/src/components/transactions/TransactionGridRow.tsx`

Apply same asterisk pattern to mandatory field labels in edit mode.

---

## Task 3: Field-Level Validation with Error Highlighting

### 3.1 Create Field Error State Structure
Define field-level error tracking:
```typescript
interface FieldErrors {
  type?: string
  symbol?: string
  date?: string
  shares?: string
  price?: string
  brokerFee?: string
}

const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
```

### 3.2 Validation Function
Create a validation function that checks each field:
```typescript
const validateFields = (): boolean => {
  const errors: FieldErrors = {}
  
  if (!type) errors.type = 'Type is required'
  
  if (!symbol) {
    errors.symbol = 'Ticker is required'
  } else if (validation && !validation.valid) {
    errors.symbol = validation.errorMessage || 'Invalid ticker'
  }
  
  if (!date) {
    errors.date = 'Date is required'
  } else if (new Date(date) > new Date()) {
    errors.date = 'Date cannot be in the future'
  }
  
  if (!shares) {
    errors.shares = 'Shares is required'
  } else if (parseFloat(shares) <= 0) {
    errors.shares = 'Shares must be positive'
  }
  
  if (!price) {
    errors.price = 'Price is required'
  } else if (parseFloat(price) <= 0) {
    errors.price = 'Price must be positive'
  }
  
  if (brokerFee && parseFloat(brokerFee) < 0) {
    errors.brokerFee = 'Fees cannot be negative'
  }
  
  setFieldErrors(errors)
  return Object.keys(errors).length === 0
}
```

### 3.3 Error Styling Component/Pattern
Create a reusable input wrapper or pattern:
```tsx
{/* Example for a field with error state */}
<div>
  <label className="block text-sm font-medium text-slate-700 mb-1">
    Shares <span className="text-red-500">*</span>
  </label>
  <input
    type="number"
    value={shares}
    onChange={e => {
      setShares(e.target.value)
      // Clear error when user starts typing
      if (fieldErrors.shares) {
        setFieldErrors(prev => ({ ...prev, shares: undefined }))
      }
    }}
    className={`block w-full rounded-lg border px-3 py-2
             focus:outline-none focus:ring-2
             ${fieldErrors.shares 
               ? 'border-red-300 focus:border-red-500 focus:ring-red-500' 
               : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500'
             }`}
  />
  {fieldErrors.shares && (
    <p className="mt-1 text-xs text-red-600">{fieldErrors.shares}</p>
  )}
</div>
```

### 3.4 Apply to All Forms
Apply field-level validation pattern to:
- `QuickAddModal.tsx`
- `TransactionForm.tsx`
- `TransactionGridRow.tsx` (edit mode)

### 3.5 Submit Handler Update
Update submit handlers to use field validation:
```typescript
const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault()
  
  if (!validateFields()) {
    return // Don't submit if validation fails
  }
  
  // ... rest of submit logic
}
```

---

## Task 4: Export CSV Feature

### 4.1 Backend Implementation

#### 4.1.1 Add Export Endpoint
**File:** `backend/src/main/java/com/stocktracker/controller/TransactionController.java`

Add new endpoint:
```java
@GetMapping("/export")
@Operation(summary = "Export all transactions as CSV")
public ResponseEntity<byte[]> exportTransactions(
        @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    byte[] csvData = transactionService.exportTransactionsAsCsv(userId);
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDispositionFormData("attachment", "transactions.csv");
    
    return ResponseEntity.ok()
            .headers(headers)
            .body(csvData);
}
```

#### 4.1.2 Add Export Service Method
**File:** `backend/src/main/java/com/stocktracker/service/TransactionService.java`

Add export method:
```java
public byte[] exportTransactionsAsCsv(Long userId) {
    List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    
    StringBuilder csv = new StringBuilder();
    // Header row
    csv.append("Type,Symbol,Company Name,Date,Shares,Price Per Share,Broker Fee,Total Amount,Notes\n");
    
    // Data rows
    for (Transaction tx : transactions) {
        csv.append(String.format("%s,%s,\"%s\",%s,%s,%s,%s,%s,\"%s\"\n",
            tx.getType(),
            tx.getSymbol(),
            escapeCSV(tx.getCompanyName()),
            tx.getTransactionDate(),
            tx.getShares(),
            tx.getPricePerShare(),
            tx.getBrokerFee() != null ? tx.getBrokerFee() : "",
            tx.getTotalAmount(),
            escapeCSV(tx.getNotes())
        ));
    }
    
    return csv.toString().getBytes(StandardCharsets.UTF_8);
}

private String escapeCSV(String value) {
    if (value == null) return "";
    return value.replace("\"", "\"\"");
}
```

### 4.2 Frontend Implementation

#### 4.2.1 Add Export API Method
**File:** `frontend/src/services/api/transactionApi.ts`

Add export method:
```typescript
/**
 * Export all transactions as CSV file.
 */
exportTransactions: async (): Promise<Blob> => {
  const response = await axiosInstance.get('/transactions/export', {
    responseType: 'blob'
  })
  return response.data
}
```

#### 4.2.2 Add Export Button to Transactions Page
**File:** `frontend/src/pages/Transactions/Transactions.tsx`

Add state for export loading:
```typescript
const [exporting, setExporting] = useState(false)
```

Add export handler:
```typescript
const handleExport = async () => {
  setExporting(true)
  try {
    const blob = await transactionApi.exportTransactions()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `transactions_${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Export failed:', error)
    // Show error notification if you have a toast system
  } finally {
    setExporting(false)
  }
}
```

Add Export button next to Import CSV button in the toolbar:
```tsx
<button
  onClick={handleExport}
  disabled={exporting || transactions.length === 0}
  className="inline-flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg
           hover:bg-slate-200 transition-colors text-sm font-medium whitespace-nowrap
           disabled:opacity-50 disabled:cursor-not-allowed"
>
  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth={2}
      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
    />
  </svg>
  {exporting ? 'Exporting...' : 'Export CSV'}
</button>
```

---

## Task 5: CSV Import - Broker Fee Field Mapping

### 5.1 Backend Changes

#### 5.1.1 Update CsvImportService Field Aliases
**File:** `backend/src/main/java/com/stocktracker/service/CsvImportService.java`

Add broker fee to `FIELD_ALIASES` map:
```java
private static final Map<String, List<String>> FIELD_ALIASES = Map.ofEntries(
    Map.entry("type", Arrays.asList("action", "type", "transaction type", "trade type", "buy/sell", "side", "order type", "transaction")),
    Map.entry("symbol", Arrays.asList("symbol", "ticker", "stock", "security", "instrument", "stock symbol", "ticker symbol", "code")),
    Map.entry("exchange", Arrays.asList("exchange", "listing exchange", "market", "listingexchange", "stock exchange")),
    Map.entry("transactionDate", Arrays.asList("date", "trade date", "transaction date", "settlement date", "exec date", "execution date", "activity date", "tradedate")),
    Map.entry("shares", Arrays.asList("shares", "quantity", "qty", "units", "amount", "volume", "share quantity")),
    Map.entry("pricePerShare", Arrays.asList("price", "share price", "unit price", "execution price", "trade price", "cost per share", "price per share", "t. price")),
    Map.entry("brokerFee", Arrays.asList(
        "ibcommission",           // IBKR: IBCommission
        "commission",             // Generic
        "fee", "fees",            // Generic
        "broker fee",             // Generic
        "brokerage",              // Generic
        "brokerage fee",          // Generic
        "trading fee",            // Generic
        "transaction fee",        // Generic
        "clearing fee",           // Generic
        "tax",                    // Tax
        "stamp duty",             // UK tax
        "transaction cost",       // Generic
        "cost"                    // Generic (lower priority)
    )),
    Map.entry("notes", Arrays.asList("notes", "memo", "description", "comment", "remarks"))
);
```

#### 5.1.2 Update validateAndMapRow Method
**File:** `backend/src/main/java/com/stocktracker/service/CsvImportService.java`

Update the `validateAndMapRow` method to extract and parse broker fee:
```java
private TransactionPreviewRow validateAndMapRow(CsvRowData rowData, Map<String, String> fieldMappings) {
    // ... existing code ...
    
    // Parse broker fee (optional)
    BigDecimal brokerFee = parseBrokerFee(mappedValues.get("brokerFee"), rowData.getRowNumber(), rowErrors);
    
    // ... existing field parsing ...
    
    previewRow.setBrokerFee(brokerFee);
    
    // ... rest of method ...
}
```

#### 5.1.3 Add parseBrokerFee Method
**File:** `backend/src/main/java/com/stocktracker/service/CsvImportService.java`

Add new parsing method:
```java
/**
 * Parse broker fee (optional, can be negative in some exports - convert to absolute value).
 */
private BigDecimal parseBrokerFee(String value, Integer rowNumber, List<CsvImportError> errors) {
    if (value == null || value.isEmpty()) {
        return null; // Optional field
    }

    try {
        // Remove currency symbols, commas, and whitespace
        String cleaned = value.replaceAll("[$€£,\\s]", "");
        BigDecimal fee = new BigDecimal(cleaned);
        
        // IBKR and some brokers report fees as negative values
        // Convert to positive (absolute value) for storage
        fee = fee.abs();
        
        // Validation: fee cannot be negative after abs() - this handles any edge cases
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new CsvImportError(
                rowNumber,
                "brokerFee",
                "Broker fee cannot be negative",
                value
            ));
            return null;
        }
        
        return fee;
    } catch (NumberFormatException e) {
        errors.add(new CsvImportError(
            rowNumber,
            "brokerFee",
            "Invalid number format for broker fee",
            value
        ));
        return null;
    }
}
```

#### 5.1.4 Update executeImport Method
**File:** `backend/src/main/java/com/stocktracker/service/CsvImportService.java`

Update the transaction request builder to include broker fee:
```java
TransactionRequest transactionRequest = TransactionRequest.builder()
    .type(previewRow.getType())
    .symbol(previewRow.getSymbol())
    .transactionDate(previewRow.getTransactionDate())
    .shares(previewRow.getShares())
    .pricePerShare(previewRow.getPricePerShare())
    .brokerFee(previewRow.getBrokerFee())  // NEW
    .notes(previewRow.getNotes())
    .build();
```

### 5.2 Frontend Changes

#### 5.2.1 Update FieldMappingStep Standard Fields
**File:** `frontend/src/components/import/FieldMappingStep.tsx`

Add broker fee to `STANDARD_FIELDS` array:
```typescript
const STANDARD_FIELDS: StandardField[] = [
  {
    value: 'type',
    label: 'Type (BUY/SELL)',
    required: false,
    hint: 'Optional - can be inferred from quantity sign',
  },
  { value: 'symbol', label: 'Symbol', required: true },
  {
    value: 'exchange',
    label: 'Exchange/Market',
    required: false,
    hint: 'Optional - adds suffix to symbol (e.g., LSEETF → .L)',
  },
  { value: 'transactionDate', label: 'Transaction Date', required: true },
  {
    value: 'shares',
    label: 'Shares/Quantity',
    required: true,
    hint: 'Negative values indicate SELL',
  },
  { value: 'pricePerShare', label: 'Price Per Share', required: true },
  {
    value: 'brokerFee',
    label: 'Broker/Clearing/Tax',
    required: false,
    hint: 'Optional - Maps to IBKR IBCommission, commission, fees, etc.',
  },
  { value: 'notes', label: 'Notes', required: false },
  { value: 'skip', label: 'Skip this column', required: false },
]
```

#### 5.2.2 Update ImportPreviewStep (if showing preview table)
**File:** `frontend/src/components/import/ImportPreviewStep.tsx`

If the preview step shows a table of transactions to be imported, add a column for broker fee:
```tsx
{/* In table header */}
<th>Broker Fee</th>

{/* In table body */}
<td>{row.brokerFee !== null ? formatCurrency(row.brokerFee) : '-'}</td>
```

---

## Summary of Files to Modify

### Backend
1. `backend/src/main/java/com/stocktracker/entity/Transaction.java` - Add brokerFee field
2. `backend/src/main/java/com/stocktracker/dto/request/TransactionRequest.java` - Add brokerFee field
3. `backend/src/main/java/com/stocktracker/dto/response/TransactionResponse.java` - Add brokerFee field
4. `backend/src/main/java/com/stocktracker/dto/response/TransactionPreviewRow.java` - Add brokerFee field
5. `backend/src/main/java/com/stocktracker/service/TransactionService.java` - Update mapping, add export method
6. `backend/src/main/java/com/stocktracker/service/CsvImportService.java` - Add brokerFee aliases, parsing, and mapping
7. `backend/src/main/java/com/stocktracker/controller/TransactionController.java` - Add export endpoint
8. `backend/src/main/resources/db/migration/V{next}_add_broker_fee.sql` - Database migration

### Frontend
1. `frontend/src/services/api/transactionApi.ts` - Update types, add export method
2. `frontend/src/components/transactions/QuickAddModal.tsx` - Add field, asterisks, validation
3. `frontend/src/components/transactions/TransactionForm.tsx` - Add field, asterisks, validation
4. `frontend/src/components/transactions/TransactionGridRow.tsx` - Add field in edit mode, asterisks, validation
5. `frontend/src/pages/Transactions/Transactions.tsx` - Add Export CSV button
6. `frontend/src/components/import/FieldMappingStep.tsx` - Add brokerFee to standard fields
7. `frontend/src/components/import/ImportPreviewStep.tsx` - Add brokerFee column to preview table (if applicable)

---

## Supported Broker Fee Column Names (CSV Import)

The following column names will be automatically recognized and mapped to the broker fee field:

| Source | Column Names |
|--------|--------------|
| IBKR (Interactive Brokers) | `IBCommission` |
| Generic | `commission`, `fee`, `fees`, `broker fee`, `brokerage`, `brokerage fee` |
| Trading | `trading fee`, `transaction fee`, `transaction cost` |
| Clearing | `clearing fee` |
| Tax | `tax`, `stamp duty` |
| Other | `cost` (lower priority) |

**Note:** Broker fees are often exported as negative values (e.g., IBKR's IBCommission shows `-1.50` for a $1.50 fee). The import process automatically converts these to positive values for storage.

---

## Testing Checklist

### Task 1: Broker Fee Field
- [ ] Backend: New field persists to database
- [ ] Backend: Total amount calculation includes broker fee
- [ ] Backend: Validation allows null/empty broker fee
- [ ] Backend: Validation rejects negative broker fee
- [ ] Frontend QuickAddModal: Field displays and accepts input
- [ ] Frontend QuickAddModal: Total reflects broker fee
- [ ] Frontend TransactionForm: Field displays and accepts input
- [ ] Frontend TransactionGridRow: Edit mode shows/saves broker fee
- [ ] Frontend: Existing transactions without broker fee display correctly

### Task 2: Mandatory Field Indicators
- [ ] QuickAddModal: Asterisks on Type, Ticker, Date, Shares, Price
- [ ] TransactionForm: Asterisks on mandatory fields
- [ ] TransactionGridRow: Asterisks in edit mode
- [ ] No asterisk on optional fields (Broker Fee, Notes)

### Task 3: Field Validation
- [ ] Each mandatory field shows error when empty on submit
- [ ] Field borders turn red on validation error
- [ ] Error message displays below field
- [ ] Errors clear when user corrects input
- [ ] Form does not submit with validation errors
- [ ] Broker fee shows error if negative

### Task 4: Export CSV
- [ ] Export button visible in Transactions page
- [ ] Export button disabled when no transactions
- [ ] CSV downloads with correct filename
- [ ] CSV contains all transaction fields
- [ ] CSV properly escapes special characters
- [ ] CSV includes broker fee column

### Task 5: CSV Import - Broker Fee Mapping
- [ ] Broker fee field appears in field mapping step
- [ ] `IBCommission` column auto-maps to broker fee (IBKR CSV)
- [ ] `commission` column auto-maps to broker fee
- [ ] `fee`/`fees` columns auto-map to broker fee
- [ ] Negative fee values converted to positive on import
- [ ] Preview step shows broker fee values
- [ ] Imported transactions include broker fee in total calculation
- [ ] Empty broker fee values import as null (not zero)
- [ ] Invalid broker fee format shows error in preview
