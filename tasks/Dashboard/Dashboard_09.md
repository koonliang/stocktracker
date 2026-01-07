# Dashboard_09: UI Enhancements - Returns Display, Sorting, and UX Improvements

## Overview
Enhance the Dashboard page with improved return metrics display, sortable holdings table, UX fixes for the Transactions page, timezone-aware timestamps, and an "ALL" filter option for the portfolio performance chart.

---

## Task 1: Enhanced Total Return Display with Percentage

### 1.1 Current State
The "Total Return" summary card shows only the dollar amount (e.g., `$12,345.67`).

### 1.2 Desired Behavior
Display both dollar amount and percentage side-by-side:
- Example: `$12,345.67 (+41.58%)`

### 1.3 Implementation

#### 1.3.1 Update Dashboard.tsx SummaryCard
**File:** `frontend/src/pages/Dashboard/Dashboard.tsx`

Update the "Total Return" SummaryCard to display both values:

**Current:**
```tsx
<SummaryCard
  label="Total Return"
  value={formatCurrency(portfolio.totalReturnDollars)}
  className={getReturnColorClass(portfolio.totalReturnDollars)}
/>
```

**Updated:**
```tsx
<SummaryCard
  label="Total Return"
  value={`${formatCurrency(portfolio.totalReturnDollars)} (${formatPercent(portfolio.totalReturnPercent)})`}
  className={getReturnColorClass(portfolio.totalReturnDollars)}
/>
```

**Alternative: Create a dedicated component for better formatting control:**
```tsx
function TotalReturnCard({ dollars, percent }: { dollars: number; percent: number }) {
  const colorClass = getReturnColorClass(dollars)
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6 shadow-soft">
      <p className="text-xs sm:text-sm font-medium text-slate-600">Total Return</p>
      <div className={`mt-1 sm:mt-2 ${colorClass}`}>
        <span className="text-xl sm:text-2xl font-bold">{formatCurrency(dollars)}</span>
        <span className="ml-2 text-base sm:text-lg font-semibold">({formatPercent(percent)})</span>
      </div>
    </div>
  )
}
```

Then use in the grid:
```tsx
{portfolio && (
  <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
    <SummaryCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
    <SummaryCard label="Total Cost" value={formatCurrency(portfolio.totalCost)} />
    <TotalReturnCard 
      dollars={portfolio.totalReturnDollars} 
      percent={portfolio.totalReturnPercent} 
    />
    {/* Task 2 will update this card */}
    <SummaryCard
      label="Annualized Yield"
      value={formatPercent(portfolio.annualizedYield)}
      className={getReturnColorClass(portfolio.annualizedYield)}
    />
  </div>
)}
```

---

## Task 2: Replace "Return %" with "Annualized Yield"

### 2.1 Current State
The fourth summary card shows "Return %" which is simply `totalReturnPercent` (total return / total cost).

### 2.2 Desired Behavior
Replace with "Annualized Yield" that shows:
- The annualized return percentage
- Duration context (e.g., `+10.66% over 8 years`)

### 2.3 Annualized Return Formula
```
Annualized Return = ((1 + Total Return %) ^ (1 / Years)) - 1

Where:
- Total Return % = (Current Value - Total Cost) / Total Cost
- Years = (Today - Earliest Transaction Date) / 365.25
```

### 2.4 Backend Implementation

#### 2.4.1 Update PortfolioResponse DTO
**File:** `backend/src/main/java/com/stocktracker/dto/response/PortfolioResponse.java`

Add new fields:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private List<HoldingResponse> holdings;

    // Portfolio summary (computed on backend)
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalReturnDollars;
    private BigDecimal totalReturnPercent;

    // NEW: Annualized yield
    private BigDecimal annualizedYield;
    private BigDecimal investmentYears;  // Years since first transaction

    private LocalDateTime pricesUpdatedAt;
}
```

#### 2.4.2 Update PortfolioService
**File:** `backend/src/main/java/com/stocktracker/service/PortfolioService.java`

Add method to calculate annualized yield:
```java
/**
 * Calculate annualized yield (CAGR) for the portfolio.
 * Formula: ((1 + totalReturn) ^ (1/years)) - 1
 */
private void calculateAnnualizedYield(PortfolioResponse response, Long userId) {
    // Get earliest transaction date
    Optional<LocalDate> earliestDate = transactionRepository
        .findEarliestTransactionDateByUserId(userId);
    
    if (earliestDate.isEmpty() || response.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
        response.setAnnualizedYield(BigDecimal.ZERO);
        response.setInvestmentYears(BigDecimal.ZERO);
        return;
    }
    
    // Calculate years invested
    long daysBetween = ChronoUnit.DAYS.between(earliestDate.get(), LocalDate.now());
    double years = daysBetween / 365.25;
    
    if (years < 0.1) {
        // Less than ~36 days, return simple return instead of annualized
        response.setAnnualizedYield(response.getTotalReturnPercent());
        response.setInvestmentYears(BigDecimal.valueOf(years).setScale(2, RoundingMode.HALF_UP));
        return;
    }
    
    // Calculate total return as decimal (e.g., 0.4158 for 41.58%)
    BigDecimal totalReturnDecimal = response.getTotalReturnPercent()
        .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
    
    // Annualized return: ((1 + r) ^ (1/n)) - 1
    double onePlusReturn = 1 + totalReturnDecimal.doubleValue();
    double annualizedDecimal = Math.pow(onePlusReturn, 1.0 / years) - 1;
    
    // Convert back to percentage
    BigDecimal annualizedPercent = BigDecimal.valueOf(annualizedDecimal * 100)
        .setScale(2, RoundingMode.HALF_UP);
    
    response.setAnnualizedYield(annualizedPercent);
    response.setInvestmentYears(BigDecimal.valueOf(years).setScale(1, RoundingMode.HALF_UP));
}
```

Update `buildPortfolioResponse` method to call this:
```java
private PortfolioResponse buildPortfolioResponse(List<HoldingResponse> holdings, Long userId) {
    // ... existing total calculations ...
    
    PortfolioResponse response = PortfolioResponse.builder()
        .holdings(holdings)
        .totalValue(totalValue)
        .totalCost(totalCost)
        .totalReturnDollars(totalReturnDollars)
        .totalReturnPercent(totalReturnPercent)
        .pricesUpdatedAt(LocalDateTime.now())
        .build();
    
    // Calculate annualized yield
    calculateAnnualizedYield(response, userId);
    
    return response;
}
```

**Note:** The `buildPortfolioResponse` method signature needs to accept `userId` parameter.

#### 2.4.3 Update TransactionRepository
**File:** `backend/src/main/java/com/stocktracker/repository/TransactionRepository.java`

Add query for earliest transaction date:
```java
@Query("SELECT MIN(t.transactionDate) FROM Transaction t WHERE t.user.id = :userId")
Optional<LocalDate> findEarliestTransactionDateByUserId(@Param("userId") Long userId);
```

### 2.5 Frontend Implementation

#### 2.5.1 Update Portfolio API Types
**File:** `frontend/src/services/api/portfolioApi.ts`

Update `PortfolioResponse` interface:
```typescript
export interface PortfolioResponse {
  holdings: HoldingResponse[]
  totalValue: number
  totalCost: number
  totalReturnDollars: number
  totalReturnPercent: number
  
  // NEW: Annualized yield
  annualizedYield: number
  investmentYears: number
  
  pricesUpdatedAt: string
}
```

#### 2.5.2 Update Dashboard.tsx
**File:** `frontend/src/pages/Dashboard/Dashboard.tsx`

Create a dedicated component for annualized yield display:
```tsx
function AnnualizedYieldCard({ yield: yieldPercent, years }: { yield: number; years: number }) {
  const colorClass = getReturnColorClass(yieldPercent)
  const yearsDisplay = years < 1 
    ? `${Math.round(years * 12)} months` 
    : years === 1 
      ? '1 year'
      : `${years.toFixed(1)} years`
  
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6 shadow-soft">
      <p className="text-xs sm:text-sm font-medium text-slate-600">Annualized Yield</p>
      <div className="mt-1 sm:mt-2">
        <span className={`text-xl sm:text-2xl font-bold ${colorClass}`}>
          {formatPercent(yieldPercent)}
        </span>
        <span className="ml-2 text-xs sm:text-sm text-slate-500">
          over {yearsDisplay}
        </span>
      </div>
    </div>
  )
}
```

Update the summary cards grid:
```tsx
{portfolio && (
  <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
    <SummaryCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
    <SummaryCard label="Total Cost" value={formatCurrency(portfolio.totalCost)} />
    <TotalReturnCard 
      dollars={portfolio.totalReturnDollars} 
      percent={portfolio.totalReturnPercent} 
    />
    <AnnualizedYieldCard 
      yield={portfolio.annualizedYield} 
      years={portfolio.investmentYears} 
    />
  </div>
)}
```

---

## Task 3: Sortable Holdings Table

### 3.1 Current State
The holdings table displays holdings in a fixed order (by symbol ascending from backend).

### 3.2 Desired Behavior
Allow users to sort the holdings table by clicking column headers:
- Symbol (A-Z / Z-A)
- Last Price (Low-High / High-Low)
- 7D Return % (Low-High / High-Low)
- Total Return % (Low-High / High-Low)
- Value (Low-High / High-Low)
- Weight % (Low-High / High-Low)
- Avg Price (Low-High / High-Low)

Visual indicators:
- Clickable headers with hover effect
- Sort direction indicator (↑ / ↓)

### 3.3 Implementation

#### 3.3.1 Create Sort Hook
**File:** `frontend/src/hooks/useSort.ts`

```typescript
import { useState, useMemo, useCallback } from 'react'

export type SortDirection = 'asc' | 'desc'

export interface SortConfig<T> {
  key: keyof T | null
  direction: SortDirection
}

export function useSort<T>(data: T[], defaultKey: keyof T | null = null) {
  const [sortConfig, setSortConfig] = useState<SortConfig<T>>({
    key: defaultKey,
    direction: 'asc',
  })

  const sortedData = useMemo(() => {
    if (!sortConfig.key) return data

    return [...data].sort((a, b) => {
      const aValue = a[sortConfig.key!]
      const bValue = b[sortConfig.key!]

      // Handle null/undefined
      if (aValue == null && bValue == null) return 0
      if (aValue == null) return sortConfig.direction === 'asc' ? 1 : -1
      if (bValue == null) return sortConfig.direction === 'asc' ? -1 : 1

      // Compare values
      let comparison = 0
      if (typeof aValue === 'string' && typeof bValue === 'string') {
        comparison = aValue.localeCompare(bValue)
      } else if (typeof aValue === 'number' && typeof bValue === 'number') {
        comparison = aValue - bValue
      }

      return sortConfig.direction === 'asc' ? comparison : -comparison
    })
  }, [data, sortConfig])

  const requestSort = useCallback((key: keyof T) => {
    setSortConfig(prev => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }))
  }, [])

  const resetSort = useCallback(() => {
    setSortConfig({ key: defaultKey, direction: 'asc' })
  }, [defaultKey])

  return { sortedData, sortConfig, requestSort, resetSort }
}
```

#### 3.3.2 Create SortableHeader Component
**File:** `frontend/src/components/dashboard/SortableHeader.tsx`

```typescript
import type { SortDirection } from '../../hooks/useSort'

interface SortableHeaderProps {
  label: string
  sortKey: string
  currentSortKey: string | null
  sortDirection: SortDirection
  onSort: (key: string) => void
  className?: string
  align?: 'left' | 'right' | 'center'
}

export function SortableHeader({
  label,
  sortKey,
  currentSortKey,
  sortDirection,
  onSort,
  className = '',
  align = 'left',
}: SortableHeaderProps) {
  const isActive = currentSortKey === sortKey
  const alignClass = align === 'right' ? 'justify-end' : align === 'center' ? 'justify-center' : 'justify-start'

  return (
    <th
      className={`px-4 lg:px-6 py-4 text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600 cursor-pointer hover:bg-slate-100 transition-colors select-none ${className}`}
      onClick={() => onSort(sortKey)}
    >
      <div className={`flex items-center gap-1 ${alignClass}`}>
        <span>{label}</span>
        <span className={`text-xs ${isActive ? 'text-indigo-600' : 'text-slate-400'}`}>
          {isActive ? (sortDirection === 'asc' ? '↑' : '↓') : '↕'}
        </span>
      </div>
    </th>
  )
}
```

#### 3.3.3 Update PortfolioTable Component
**File:** `frontend/src/components/dashboard/PortfolioTable.tsx`

```typescript
import { useState } from 'react'
import type { HoldingResponse } from '../../services/api/portfolioApi'
import { PortfolioTableRow } from './PortfolioTableRow'
import { HoldingCard } from './HoldingCard'
import { SortableHeader } from './SortableHeader'
import { useSort } from '../../hooks/useSort'

interface PortfolioTableProps {
  holdings: HoldingResponse[]
}

type SortKey = keyof HoldingResponse

export function PortfolioTable({ holdings }: PortfolioTableProps) {
  const { sortedData, sortConfig, requestSort } = useSort<HoldingResponse>(holdings, 'symbol')

  return (
    <>
      {/* Mobile Card View - with sorted data */}
      <div className="md:hidden space-y-4">
        {sortedData.map(holding => (
          <HoldingCard key={holding.id} holding={holding} />
        ))}
      </div>

      {/* Desktop Table View */}
      <div className="hidden md:block overflow-x-auto rounded-xl border border-border bg-white shadow-soft">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <SortableHeader
                label="Symbol"
                sortKey="symbol"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="left"
              />
              <SortableHeader
                label="Last Price"
                sortKey="lastPrice"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="7D Return"
                sortKey="sevenDayReturnPercent"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="Total Return"
                sortKey="totalReturnPercent"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="Value / Cost"
                sortKey="currentValue"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="right"
              />
              <SortableHeader
                label="Weight / Shares"
                sortKey="weight"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="right"
                className="hidden lg:table-cell"
              />
              <SortableHeader
                label="Avg Price"
                sortKey="averageCost"
                currentSortKey={sortConfig.key as string}
                sortDirection={sortConfig.direction}
                onSort={(key) => requestSort(key as SortKey)}
                align="right"
                className="hidden lg:table-cell"
              />
              <th className="hidden xl:table-cell px-4 lg:px-6 py-4 text-center text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                1Y Chart
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sortedData.map(holding => (
              <PortfolioTableRow key={holding.id} holding={holding} />
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
```

#### 3.3.4 Mobile Sort Dropdown (Optional Enhancement)
For mobile view, add a sort dropdown above the cards:

```tsx
{/* Mobile Sort Dropdown */}
<div className="md:hidden mb-4">
  <label className="block text-sm font-medium text-slate-700 mb-1">Sort by</label>
  <select
    value={`${sortConfig.key}-${sortConfig.direction}`}
    onChange={(e) => {
      const [key, direction] = e.target.value.split('-') as [SortKey, 'asc' | 'desc']
      // Update sort config
    }}
    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
  >
    <option value="symbol-asc">Symbol (A-Z)</option>
    <option value="symbol-desc">Symbol (Z-A)</option>
    <option value="currentValue-desc">Value (High-Low)</option>
    <option value="currentValue-asc">Value (Low-High)</option>
    <option value="totalReturnPercent-desc">Return (High-Low)</option>
    <option value="totalReturnPercent-asc">Return (Low-High)</option>
    <option value="weight-desc">Weight (High-Low)</option>
  </select>
</div>
```

---

## Task 4: FAB in Manage Transactions Page

### 4.1 Current State
The Transactions page (`/dashboard/transactions`) has Import/Export buttons but no quick-add FAB.

### 4.2 Desired Behavior
Add the same floating action button (FAB) as the Dashboard page to allow quick transaction entry.

### 4.3 Implementation

#### 4.3.1 Update Transactions.tsx
**File:** `frontend/src/pages/Transactions/Transactions.tsx`

Add state and modal:
```typescript
import { QuickAddModal } from '../../components/transactions/QuickAddModal'
import { useModal } from '../../hooks/useModal'

const Transactions = () => {
  // ... existing state ...
  
  const { isOpen: showQuickAdd, open: openQuickAdd, close: closeQuickAdd } = useModal()
  
  const handleQuickAddSuccess = async () => {
    await refreshTransactions()
    refresh()
  }
  
  // ... existing code ...
  
  return (
    <div className="min-h-screen bg-background">
      {/* ... existing content ... */}
      
      {/* Quick Add Modal */}
      <QuickAddModal
        isOpen={showQuickAdd}
        onClose={closeQuickAdd}
        onSuccess={handleQuickAddSuccess}
      />

      {/* Floating Action Button */}
      <button
        onClick={openQuickAdd}
        className="fixed bottom-6 right-6 h-14 w-14 rounded-full bg-indigo-600 text-white shadow-lg
                   transition-all duration-200 hover:bg-indigo-700 hover:shadow-xl hover:scale-110
                   focus:outline-none focus:ring-4 focus:ring-indigo-300
                   sm:h-16 sm:w-16"
        aria-label="Add transaction"
        title="Add Transaction"
      >
        <svg
          className="h-6 w-6 sm:h-8 sm:w-8 mx-auto"
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2.5"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path d="M12 4v16m8-8H4" />
        </svg>
      </button>
    </div>
  )
}
```

---

## Task 5: Fix "Add your first transaction" Button

### 5.1 Current State
When there are no transactions, the "Add your first transaction" button navigates to `/dashboard` instead of opening the Add Transaction modal.

**Current code:**
```tsx
<Link
  to="/dashboard"
  className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2
           text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
>
  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
  </svg>
  Add your first transaction
</Link>
```

### 5.2 Desired Behavior
The button should open the QuickAddModal directly.

### 5.3 Implementation

#### 5.3.1 Update Transactions.tsx Empty State
**File:** `frontend/src/pages/Transactions/Transactions.tsx`

Change from `<Link>` to `<button>`:
```tsx
{filteredTransactions.length === 0 ? (
  <div className="text-center py-12">
    {hasActiveFilters ? (
      // ... filter empty state (unchanged) ...
    ) : (
      <>
        <svg
          className="mx-auto h-12 w-12 text-slate-300 mb-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
        <p className="text-slate-600 mb-4">No transactions yet</p>
        <button
          onClick={openQuickAdd}
          className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2
                   text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add your first transaction
        </button>
      </>
    )}
  </div>
) : (
  // ... transaction grid ...
)}
```

---

## Task 6: Display Local Time for "Prices Updated"

### 6.1 Current State
The "Prices updated" timestamp displays server time (UTC) without timezone context.

**Current code:**
```tsx
<span className="text-sm text-slate-500">
  Prices updated:{' '}
  {portfolio?.pricesUpdatedAt
    ? new Date(portfolio.pricesUpdatedAt).toLocaleTimeString()
    : '-'}
</span>
```

### 6.2 Issue
While `toLocaleTimeString()` does convert to local time, the server returns `LocalDateTime` which doesn't include timezone info. This can cause ambiguity.

### 6.3 Desired Behavior
Display the time clearly in the user's local timezone with an indication.

### 6.4 Implementation

#### 6.4.1 Backend: Return UTC Timestamp
**File:** `backend/src/main/java/com/stocktracker/service/PortfolioService.java`

Change from `LocalDateTime` to `Instant` or `ZonedDateTime`:
```java
// In buildPortfolioResponse method
return PortfolioResponse.builder()
    // ... other fields ...
    .pricesUpdatedAt(Instant.now())  // Use Instant for UTC
    .build();
```

**Alternative:** Keep `LocalDateTime` but serialize with timezone in JSON config.

#### 6.4.2 Backend: Update DTO
**File:** `backend/src/main/java/com/stocktracker/dto/response/PortfolioResponse.java`

Option A - Use Instant:
```java
private Instant pricesUpdatedAt;
```

Option B - Keep LocalDateTime with explicit timezone annotation:
```java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
private LocalDateTime pricesUpdatedAt;
```

#### 6.4.3 Frontend: Create Date Formatter Utility
**File:** `frontend/src/utils/dateFormatters.ts`

```typescript
/**
 * Format a UTC timestamp to local time with clear indication.
 * Example: "2:30:45 PM (local time)"
 */
export function formatLocalTime(utcTimestamp: string | null | undefined): string {
  if (!utcTimestamp) return '-'
  
  const date = new Date(utcTimestamp)
  
  // Check if valid date
  if (isNaN(date.getTime())) return '-'
  
  return date.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  })
}

/**
 * Format timestamp with relative time indicator.
 * Example: "2:30 PM (3 min ago)"
 */
export function formatTimeWithRelative(utcTimestamp: string | null | undefined): string {
  if (!utcTimestamp) return '-'
  
  const date = new Date(utcTimestamp)
  if (isNaN(date.getTime())) return '-'
  
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  
  const timeStr = date.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  })
  
  if (diffMins < 1) {
    return `${timeStr} (just now)`
  } else if (diffMins < 60) {
    return `${timeStr} (${diffMins} min ago)`
  } else {
    return timeStr
  }
}
```

#### 6.4.4 Update Dashboard.tsx
**File:** `frontend/src/pages/Dashboard/Dashboard.tsx`

```typescript
import { formatTimeWithRelative } from '../../utils/dateFormatters'

// In the component:
<span className="text-sm text-slate-500">
  Prices updated: {formatTimeWithRelative(portfolio?.pricesUpdatedAt)}
</span>
```

---

## Task 7: Add "ALL" Filter to Performance Chart

### 7.1 Current State
The PerformanceChart has time range options: 7D, 1M, 3M, YTD, 1Y

### 7.2 Desired Behavior
Add an "ALL" option that shows the complete portfolio performance history from the first transaction.

### 7.3 Implementation

#### 7.3.1 Update TimeRange Type
**File:** `frontend/src/services/api/portfolioApi.ts`

```typescript
export type TimeRange = '7d' | '1mo' | '3mo' | 'ytd' | '1y' | 'all'
```

#### 7.3.2 Update Backend Controller
**File:** `backend/src/main/java/com/stocktracker/controller/PortfolioController.java`

The endpoint already accepts `range` as a String, so no changes needed to the controller.

#### 7.3.3 Update Backend Service
**File:** `backend/src/main/java/com/stocktracker/service/PortfolioService.java`

Update `getPerformanceHistory` to handle "all" range:
```java
public List<PortfolioPerformancePoint> getPerformanceHistory(Long userId, String range) {
    log.debug("getPerformanceHistory.. userId={}, range={}", userId, range);
    List<Holding> holdings = holdingRepository.findByUserIdOrderBySymbolAsc(userId);

    if (holdings.isEmpty()) {
        return Collections.emptyList();
    }

    List<String> symbols = holdings.stream()
        .map(Holding::getSymbol)
        .collect(Collectors.toList());

    // For "all" range, calculate the range from earliest transaction
    String effectiveRange = range;
    if ("all".equalsIgnoreCase(range)) {
        effectiveRange = calculateAllTimeRange(userId);
    }

    Map<String, HistoricalData> historicalDataMap = yahooFinanceClient.getHistoricalDataBatch(symbols, effectiveRange);
    return aggregatePortfolioPerformance(userId, holdings, historicalDataMap);
}

/**
 * Calculate the range string for "all" time based on earliest transaction.
 * Returns a range like "5y" or "10y" based on how long the user has been investing.
 */
private String calculateAllTimeRange(Long userId) {
    Optional<LocalDate> earliestDate = transactionRepository.findEarliestTransactionDateByUserId(userId);
    
    if (earliestDate.isEmpty()) {
        return "1y"; // Default fallback
    }
    
    long daysBetween = ChronoUnit.DAYS.between(earliestDate.get(), LocalDate.now());
    long years = daysBetween / 365;
    
    // Round up to nearest year and add buffer
    if (years < 1) return "1y";
    if (years < 2) return "2y";
    if (years < 5) return "5y";
    if (years < 10) return "10y";
    return "max"; // Yahoo Finance supports "max" for all available data
}
```

#### 7.3.4 Update Yahoo Finance Client (if needed)
**File:** `backend/src/main/java/com/stocktracker/client/YahooFinanceClient.java`

Ensure the client can handle extended ranges:
```java
private String convertRangeToPeriod(String range) {
    return switch (range.toLowerCase()) {
        case "7d" -> "5d";  // Yahoo uses 5d
        case "1mo" -> "1mo";
        case "3mo" -> "3mo";
        case "ytd" -> "ytd";
        case "1y" -> "1y";
        case "2y" -> "2y";
        case "5y" -> "5y";
        case "10y" -> "10y";
        case "max" -> "max";
        default -> "1y";
    };
}
```

#### 7.3.5 Update PerformanceChart Component
**File:** `frontend/src/components/dashboard/PerformanceChart.tsx`

Add "ALL" to the time range options:
```typescript
const TIME_RANGES: { value: TimeRange; label: string }[] = [
  { value: '7d', label: '7D' },
  { value: '1mo', label: '1M' },
  { value: '3mo', label: '3M' },
  { value: 'ytd', label: 'YTD' },
  { value: '1y', label: '1Y' },
  { value: 'all', label: 'ALL' },
]
```

Update date formatting for "all" range:
```typescript
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  if (range === '7d') {
    return date.toLocaleDateString('en-US', { weekday: 'short' })
  }
  if (range === '1mo' || range === '3mo') {
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  }
  if (range === 'all') {
    // For long time ranges, show year prominently
    return date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
  }
  return date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
}
```

---

## Summary of Files to Modify

### Backend
1. `backend/src/main/java/com/stocktracker/dto/response/PortfolioResponse.java` - Add annualizedYield, investmentYears fields
2. `backend/src/main/java/com/stocktracker/service/PortfolioService.java` - Add annualized yield calculation, handle "all" time range
3. `backend/src/main/java/com/stocktracker/repository/TransactionRepository.java` - Add findEarliestTransactionDateByUserId query
4. `backend/src/main/java/com/stocktracker/client/YahooFinanceClient.java` - Support extended date ranges (2y, 5y, 10y, max)

### Frontend
1. `frontend/src/pages/Dashboard/Dashboard.tsx` - Update summary cards, add TotalReturnCard, AnnualizedYieldCard
2. `frontend/src/components/dashboard/PortfolioTable.tsx` - Add sorting functionality
3. `frontend/src/components/dashboard/SortableHeader.tsx` - New component for sortable column headers
4. `frontend/src/hooks/useSort.ts` - New hook for sorting logic
5. `frontend/src/pages/Transactions/Transactions.tsx` - Add FAB, fix empty state button
6. `frontend/src/components/dashboard/PerformanceChart.tsx` - Add "ALL" time range option
7. `frontend/src/services/api/portfolioApi.ts` - Update PortfolioResponse interface, TimeRange type
8. `frontend/src/utils/dateFormatters.ts` - New utility for local time formatting

---

## Testing Checklist

### Task 1: Total Return with Percentage
- [ ] Total Return card shows both dollar amount and percentage
- [ ] Positive returns show green color with "+" prefix
- [ ] Negative returns show red color with "-" prefix
- [ ] Layout looks good on mobile and desktop

### Task 2: Annualized Yield
- [ ] Backend calculates annualized yield correctly
- [ ] Shows years/months duration text
- [ ] Handles edge cases: new portfolio (< 1 month), 0 cost, no transactions
- [ ] Formula verified: CAGR calculation is accurate

### Task 3: Sortable Holdings Table
- [ ] Each sortable column header is clickable
- [ ] Sort direction indicator (↑/↓) appears on active column
- [ ] Sorting works correctly for all data types (string, number)
- [ ] Sorting persists when data refreshes
- [ ] Mobile card view also respects sort order

### Task 4: FAB in Transactions Page
- [ ] FAB appears in bottom-right of Transactions page
- [ ] Clicking FAB opens QuickAddModal
- [ ] After adding transaction, list refreshes

### Task 5: "Add your first transaction" Button
- [ ] Button opens QuickAddModal instead of navigating away
- [ ] After adding transaction, the new transaction appears in the list

### Task 6: Local Time Display
- [ ] Timestamp shows in user's local timezone
- [ ] Format is clear and readable (e.g., "2:30 PM (3 min ago)")
- [ ] Works correctly across different timezones

### Task 7: "ALL" Time Range
- [ ] "ALL" button appears in Performance Chart
- [ ] Clicking "ALL" shows complete history from first transaction
- [ ] Chart handles long date ranges gracefully (5+ years)
- [ ] X-axis labels are appropriately spaced for long ranges
