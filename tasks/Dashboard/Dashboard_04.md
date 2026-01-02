# Dashboard Responsive Design Enhancement

## Overview

Enhance the dashboard page to be fully responsive across all device sizes (mobile phones, tablets, laptops, and desktops). Eliminate horizontal scrolling on mobile devices by converting data tables into card-based layouts for smaller screens.

## Current State Analysis

### Components Requiring Responsive Updates:
1. **Dashboard.tsx** - Main page layout and summary cards
2. **DashboardNavigation.tsx** - Navigation bar (already has `hidden md:flex` for nav links)
3. **PortfolioTable.tsx** - Holdings table with 8 columns (needs mobile card view)
4. **PortfolioTableRow.tsx** - Individual holding row (needs mobile card variant)
5. **TransactionGrid.tsx** - Transaction table with 7 columns (needs mobile card view)
6. **TransactionGridRow.tsx** - Individual transaction row (needs mobile card variant)
7. **PerformanceChart.tsx** - Chart with range toggles
8. **TransactionForm.tsx** - Form fields layout

### Breakpoint Strategy (Tailwind defaults):
- `sm`: 640px+ (large phones in landscape)
- `md`: 768px+ (tablets)
- `lg`: 1024px+ (laptops)
- `xl`: 1280px+ (desktops)
- `2xl`: 1536px+ (large desktops)

---

## Phase 1: Navigation - Mobile Menu Implementation

### File: `frontend/src/components/layout/DashboardNavigation.tsx`

**Current Issue:** Navigation links hidden on mobile (`hidden md:flex`) with no alternative.

**Implementation:**

1. Add hamburger menu button for mobile (visible on `md:hidden`):
```tsx
const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
```

2. Add hamburger button next to logo:
```tsx
<button
  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
  className="md:hidden p-2 text-slate-600 hover:text-indigo-600 rounded-lg"
  aria-label="Toggle menu"
>
  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    {isMobileMenuOpen ? (
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
    ) : (
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
    )}
  </svg>
</button>
```

3. Add mobile menu dropdown (slides down below nav):
```tsx
{isMobileMenuOpen && (
  <div className="md:hidden border-t border-slate-100 bg-white">
    <div className="px-4 py-2 space-y-1">
      {navLinks.map(link => (
        <Link
          key={link.path}
          to={link.path}
          onClick={() => setIsMobileMenuOpen(false)}
          className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium ${
            isActive(link.path)
              ? 'bg-indigo-50 text-indigo-600'
              : 'text-slate-700 hover:bg-slate-50'
          }`}
        >
          {/* icon + name */}
        </Link>
      ))}
    </div>
  </div>
)}
```

4. Hide logo text on very small screens:
```tsx
<span className="hidden sm:inline text-lg font-bold text-slate-900">Stock Tracker</span>
```

---

## Phase 2: Dashboard Header - Responsive Buttons

### File: `frontend/src/pages/Dashboard/Dashboard.tsx`

**Current Issue:** Header buttons side-by-side may overflow on mobile.

**Implementation:**

1. Update header to stack on mobile:
```tsx
<header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
  <h1 className="text-xl sm:text-2xl font-bold text-slate-900">Portfolio Overview</h1>
  <div className="flex flex-wrap gap-2 sm:gap-3">
    <button
      onClick={() => setShowTransactions(!showTransactions)}
      className={`flex-1 sm:flex-none rounded-lg px-3 sm:px-4 py-2 text-sm sm:text-base font-medium transition-colors
        ${showTransactions
          ? 'bg-indigo-100 text-indigo-700'
          : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
        }`}
    >
      <span className="hidden sm:inline">{showTransactions ? 'Hide Transactions' : 'Manage Transactions'}</span>
      <span className="sm:hidden">{showTransactions ? 'Hide' : 'Transactions'}</span>
    </button>
    <button
      onClick={refresh}
      className="flex-1 sm:flex-none rounded-lg bg-indigo-600 px-3 sm:px-4 py-2 text-sm sm:text-base text-white hover:bg-indigo-700"
    >
      <span className="hidden sm:inline">Refresh Prices</span>
      <span className="sm:hidden">Refresh</span>
    </button>
  </div>
</header>
```

---

## Phase 3: Summary Cards - Responsive Grid

### File: `frontend/src/pages/Dashboard/Dashboard.tsx`

**Current State:** Uses `grid gap-4 sm:grid-cols-2 lg:grid-cols-4` - already responsive.

**Enhancement - Compact Mobile Cards:**

1. Update `SummaryCard` component for mobile:
```tsx
function SummaryCard({ label, value, className = '' }: SummaryCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6 shadow-soft">
      <p className="text-xs sm:text-sm font-medium text-slate-600">{label}</p>
      <p className={`mt-1 sm:mt-2 text-xl sm:text-2xl font-bold ${className || 'text-slate-900'}`}>
        {value}
      </p>
    </div>
  )
}
```

---

## Phase 4: Performance Chart - Responsive Design

### File: `frontend/src/components/dashboard/PerformanceChart.tsx`

**Current Issues:**
- Range toggle buttons may overflow on mobile
- Chart height fixed at 280px
- Header layout may wrap awkwardly

**Implementation:**

1. Responsive header with stacking:
```tsx
<div className="mb-4 sm:mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
  <div>
    <h2 className="text-base sm:text-lg font-semibold text-slate-900">Portfolio Performance</h2>
    <div className={`text-xl sm:text-2xl font-bold ${isPositive ? 'text-emerald-600' : 'text-red-500'}`}>
      {isPositive ? '+' : ''}
      {formatCurrency(periodReturn.dollars)}
      <span className="ml-1 sm:ml-2 text-sm sm:text-base font-medium">
        ({isPositive ? '+' : ''}
        {periodReturn.percent.toFixed(2)}%)
      </span>
    </div>
  </div>

  {/* Scrollable time range buttons on mobile */}
  <div className="flex gap-1 rounded-lg bg-slate-100 p-1 overflow-x-auto">
    {TIME_RANGES.map(({ value, label }) => (
      <button
        key={value}
        onClick={() => onRangeChange(value)}
        className={`flex-shrink-0 px-2.5 sm:px-3 py-1 sm:py-1.5 text-xs sm:text-sm font-medium rounded-md transition-colors
          ${range === value
            ? 'bg-white text-slate-900 shadow-sm'
            : 'text-slate-600 hover:text-slate-900'
          }`}
      >
        {label}
      </button>
    ))}
  </div>
</div>
```

2. Responsive chart height:
```tsx
<ResponsiveContainer width="100%" height={window.innerWidth < 640 ? 200 : 280}>
```

Better approach - use a custom hook or CSS:
```tsx
// Alternative: use className with aspect ratio
<div className="h-48 sm:h-64 md:h-72">
  <ResponsiveContainer width="100%" height="100%">
    {/* chart content */}
  </ResponsiveContainer>
</div>
```

3. Reduce Y-axis width on mobile:
```tsx
<YAxis
  tickFormatter={value => `$${(value / 1000).toFixed(0)}k`}
  tick={{ fontSize: window.innerWidth < 640 ? 10 : 12, fill: '#64748b' }}
  axisLine={false}
  tickLine={false}
  width={window.innerWidth < 640 ? 45 : 60}
/>
```

Better approach - use a responsive hook:
```tsx
// Create hook in hooks/useBreakpoint.ts
import { useState, useEffect } from 'react'

export function useBreakpoint() {
  const [isMobile, setIsMobile] = useState(window.innerWidth < 640)

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 640)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return { isMobile }
}
```

---

## Phase 5: Holdings - Card Layout for Mobile

### File: `frontend/src/components/dashboard/PortfolioTable.tsx`

**Current Issue:** 8-column table requires horizontal scrolling on mobile.

**Strategy:** Show card view on mobile (`< md`), table view on desktop (`>= md`).

**Implementation:**

1. Create new component `HoldingCard.tsx`:
```tsx
// frontend/src/components/dashboard/HoldingCard.tsx
import type { HoldingResponse } from '../../services/api/portfolioApi'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters'
import { Sparkline } from './Sparkline'

interface HoldingCardProps {
  holding: HoldingResponse
}

export function HoldingCard({ holding }: HoldingCardProps) {
  const returnClass = getReturnColorClass(holding.totalReturnDollars)
  const sevenDayReturnClass = getReturnColorClass(holding.sevenDayReturnDollars)

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
      {/* Header: Symbol & Company */}
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="font-semibold text-slate-900 text-lg">{holding.symbol}</div>
          <div className="text-sm text-slate-500 truncate max-w-[180px]">{holding.companyName}</div>
        </div>
        <div className="text-right">
          <div className="font-semibold text-slate-900">{formatCurrency(holding.currentValue)}</div>
          <div className={`text-sm font-medium ${returnClass}`}>
            {formatPercent(holding.totalReturnPercent)}
          </div>
        </div>
      </div>

      {/* Sparkline */}
      <div className="mb-3 flex justify-center">
        <Sparkline data={holding.sparklineData} width={280} height={40} />
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 gap-3 text-sm">
        <div className="flex justify-between">
          <span className="text-slate-500">Last Price</span>
          <span className="font-medium text-slate-900">{formatCurrency(holding.lastPrice)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Avg Cost</span>
          <span className="font-medium text-slate-900">{formatCurrency(holding.averageCost)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">7D Return</span>
          <span className={`font-medium ${sevenDayReturnClass}`}>
            {formatPercent(holding.sevenDayReturnPercent)}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Total Return</span>
          <span className={`font-medium ${returnClass}`}>
            {formatCurrency(holding.totalReturnDollars)}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Shares</span>
          <span className="font-medium text-slate-900">{holding.shares.toFixed(2)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Weight</span>
          <span className="font-medium text-slate-900">{holding.weight.toFixed(1)}%</span>
        </div>
        <div className="flex justify-between col-span-2 pt-2 border-t border-slate-100">
          <span className="text-slate-500">Cost Basis</span>
          <span className="font-medium text-slate-700">{formatCurrency(holding.costBasis)}</span>
        </div>
      </div>
    </div>
  )
}
```

2. Update `PortfolioTable.tsx` to switch between views:
```tsx
import type { HoldingResponse } from '../../services/api/portfolioApi'
import { PortfolioTableRow } from './PortfolioTableRow'
import { HoldingCard } from './HoldingCard'

interface PortfolioTableProps {
  holdings: HoldingResponse[]
}

export function PortfolioTable({ holdings }: PortfolioTableProps) {
  return (
    <>
      {/* Mobile Card View */}
      <div className="md:hidden space-y-4">
        {holdings.map(holding => (
          <HoldingCard key={holding.id} holding={holding} />
        ))}
      </div>

      {/* Desktop Table View */}
      <div className="hidden md:block overflow-x-auto rounded-xl border border-border bg-white shadow-soft">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 lg:px-6 py-4 text-left text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Symbol
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Last Price
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                7D Return
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Total Return
              </th>
              <th className="px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Value / Cost
              </th>
              <th className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Weight / Shares
              </th>
              <th className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                Avg Price
              </th>
              <th className="hidden xl:table-cell px-4 lg:px-6 py-4 text-center text-xs lg:text-sm font-semibold uppercase tracking-wide text-slate-600">
                1Y Chart
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {holdings.map(holding => (
              <PortfolioTableRow key={holding.id} holding={holding} />
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
```

3. Update `PortfolioTableRow.tsx` for responsive columns:
```tsx
// Add responsive hiding for less critical columns
<td className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right">
  {/* Weight/Shares - hidden on tablet */}
</td>

<td className="hidden lg:table-cell px-4 lg:px-6 py-4 text-right font-medium text-slate-900">
  {/* Avg Price - hidden on tablet */}
</td>

<td className="hidden xl:table-cell px-4 lg:px-6 py-4">
  {/* Sparkline - hidden until xl */}
</td>
```

4. Update `Sparkline.tsx` to accept responsive dimensions:
```tsx
interface SparklineProps {
  data: number[]
  width?: number
  height?: number
}

export function Sparkline({ data, width = 120, height = 32 }: SparklineProps) {
  // ... existing logic using width/height props
}
```

---

## Phase 6: Transactions - Card Layout for Mobile

### File: `frontend/src/components/transactions/TransactionGrid.tsx`

**Current Issue:** 7-column table requires horizontal scrolling on mobile.

**Implementation:**

1. Create new component `TransactionCard.tsx`:
```tsx
// frontend/src/components/transactions/TransactionCard.tsx
import { useState } from 'react'
import type { TransactionResponse, TransactionRequest, TransactionType } from '../../services/api/transactionApi'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionCardProps {
  transaction: TransactionResponse
  isEditing: boolean
  onEdit: () => void
  onSave: (request: TransactionRequest) => Promise<void>
  onCancel: () => void
  onDelete: () => void
}

export function TransactionCard({
  transaction,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onDelete,
}: TransactionCardProps) {
  const [type, setType] = useState<TransactionType>(transaction.type)
  const [symbol, setSymbol] = useState(transaction.symbol)
  const [date, setDate] = useState(transaction.transactionDate)
  const [shares, setShares] = useState(transaction.shares.toString())
  const [price, setPrice] = useState(transaction.pricePerShare.toString())
  const [saving, setSaving] = useState(false)

  const { validating, validation, validateTicker } = useTickerValidation()

  const handleSymbolChange = async (value: string) => {
    const upperValue = value.toUpperCase()
    setSymbol(upperValue)
    if (upperValue !== transaction.symbol) {
      await validateTicker(upperValue)
    }
  }

  const handleSave = async () => {
    if (symbol !== transaction.symbol && validation && !validation.valid) return
    setSaving(true)
    try {
      await onSave({
        type,
        symbol,
        transactionDate: date,
        shares: parseFloat(shares),
        pricePerShare: parseFloat(price),
      })
    } finally {
      setSaving(false)
    }
  }

  if (!isEditing) {
    // Display Mode Card
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold
              ${transaction.type === 'BUY' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}
            >
              {transaction.type}
            </span>
            <div>
              <span className="font-semibold text-slate-900">{transaction.symbol}</span>
              <span className="text-xs text-slate-500 ml-1 hidden sm:inline">{transaction.companyName}</span>
            </div>
          </div>
          <div className="text-right">
            <div className="font-semibold text-slate-900">{formatCurrency(transaction.totalAmount)}</div>
            <div className="text-xs text-slate-500">
              {new Date(transaction.transactionDate).toLocaleDateString()}
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between text-sm">
          <div className="text-slate-600">
            <span className="font-medium">{transaction.shares.toFixed(4).replace(/\.?0+$/, '')}</span>
            <span className="text-slate-400"> shares @ </span>
            <span className="font-medium">{formatCurrency(transaction.pricePerShare)}</span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={onEdit}
              className="rounded px-2 py-1 text-sm text-indigo-600 hover:bg-indigo-50"
            >
              Edit
            </button>
            <button
              onClick={onDelete}
              className="rounded px-2 py-1 text-sm text-red-600 hover:bg-red-50"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    )
  }

  // Edit Mode Card
  const totalAmount = parseFloat(shares || '0') * parseFloat(price || '0')

  return (
    <div className="rounded-xl border-2 border-indigo-300 bg-indigo-50 p-4">
      <div className="space-y-3">
        {/* Type & Symbol Row */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Type</label>
            <select
              value={type}
              onChange={e => setType(e.target.value as TransactionType)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Symbol</label>
            <input
              type="text"
              value={symbol}
              onChange={e => handleSymbolChange(e.target.value)}
              className={`w-full rounded border px-3 py-2 text-sm uppercase
                ${validation?.valid === false ? 'border-red-300' : 'border-slate-300'}`}
            />
            {validating && <span className="text-xs text-slate-400">Validating...</span>}
            {validation?.valid === false && (
              <span className="text-xs text-red-500">{validation.errorMessage}</span>
            )}
          </div>
        </div>

        {/* Date Row */}
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Date</label>
          <input
            type="date"
            value={date}
            onChange={e => setDate(e.target.value)}
            max={new Date().toISOString().split('T')[0]}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        {/* Shares & Price Row */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Shares</label>
            <input
              type="number"
              value={shares}
              onChange={e => setShares(e.target.value)}
              min="0.0001"
              step="0.0001"
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Price</label>
            <input
              type="number"
              value={price}
              onChange={e => setPrice(e.target.value)}
              min="0.01"
              step="0.01"
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
        </div>

        {/* Total & Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-indigo-200">
          <div className="text-sm">
            <span className="text-slate-600">Total: </span>
            <span className="font-semibold text-slate-900">{formatCurrency(totalAmount)}</span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={onCancel}
              className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-white"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={saving || validating}
              className="rounded bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
```

2. Update `TransactionGrid.tsx` to switch between views:
```tsx
import { TransactionCard } from './TransactionCard'

// In the component, after toolbar and before table:
return (
  <div className="space-y-4">
    {/* Toolbar */}
    <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
      <div className="flex items-center gap-3">
        <select
          value={filterSymbol}
          onChange={e => setFilterSymbol(e.target.value)}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">All Symbols</option>
          {uniqueSymbols.map(symbol => (
            <option key={symbol} value={symbol}>{symbol}</option>
          ))}
        </select>
        <span className="text-sm text-slate-500">
          {sortedTransactions.length} transaction{sortedTransactions.length !== 1 ? 's' : ''}
        </span>
      </div>
      <div className="text-sm text-slate-600">
        <span className="text-green-600">Bought: {formatCurrency(totalBuys)}</span>
        <span className="mx-2">|</span>
        <span className="text-red-600">Sold: {formatCurrency(totalSells)}</span>
      </div>
    </div>

    {/* Mobile Card View */}
    <div className="md:hidden space-y-3">
      {sortedTransactions.map(transaction => (
        <TransactionCard
          key={transaction.id}
          transaction={transaction}
          isEditing={editingId === transaction.id}
          onEdit={() => setEditingId(transaction.id)}
          onSave={request => handleSave(transaction.id, request)}
          onCancel={() => setEditingId(null)}
          onDelete={() => handleDelete(transaction.id)}
        />
      ))}

      {/* Add New Card */}
      {showAddRow ? (
        <div className="rounded-xl border-2 border-dashed border-indigo-300 bg-indigo-50 p-4">
          <h3 className="text-sm font-medium text-indigo-900 mb-3">New Transaction</h3>
          <TransactionForm onSubmit={handleCreate} onCancel={() => setShowAddRow(false)} />
        </div>
      ) : (
        <button
          onClick={() => setShowAddRow(true)}
          className="w-full flex items-center justify-center gap-2 rounded-xl border-2 border-dashed
                   border-slate-300 py-4 text-slate-500 transition-colors hover:border-indigo-400
                   hover:text-indigo-600"
        >
          <span className="text-xl">+</span>
          <span>Add Transaction</span>
        </button>
      )}
    </div>

    {/* Desktop Table View */}
    <div className="hidden md:block overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-soft">
      {/* existing table code */}
    </div>
  </div>
)
```

---

## Phase 7: Transaction Form - Responsive Layout

### File: `frontend/src/components/transactions/TransactionForm.tsx`

**Enhancement:** Ensure form fields stack properly on mobile.

**Implementation:**
```tsx
// Update form grid classes
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 sm:gap-4">
  {/* Fields */}
</div>

// For button row
<div className="flex flex-col-reverse sm:flex-row gap-2 sm:gap-3 pt-3">
  <button onClick={onCancel} className="w-full sm:w-auto ...">
    Cancel
  </button>
  <button type="submit" className="w-full sm:w-auto ...">
    Add Transaction
  </button>
</div>
```

---

## Phase 8: Create Responsive Utility Hook

### File: `frontend/src/hooks/useBreakpoint.ts`

Create a reusable hook for responsive behavior:

```tsx
import { useState, useEffect } from 'react'

type Breakpoint = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'

const breakpoints = {
  xs: 0,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536,
}

export function useBreakpoint() {
  const [breakpoint, setBreakpoint] = useState<Breakpoint>(() => {
    if (typeof window === 'undefined') return 'md'
    const width = window.innerWidth
    if (width >= breakpoints['2xl']) return '2xl'
    if (width >= breakpoints.xl) return 'xl'
    if (width >= breakpoints.lg) return 'lg'
    if (width >= breakpoints.md) return 'md'
    if (width >= breakpoints.sm) return 'sm'
    return 'xs'
  })

  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth
      if (width >= breakpoints['2xl']) setBreakpoint('2xl')
      else if (width >= breakpoints.xl) setBreakpoint('xl')
      else if (width >= breakpoints.lg) setBreakpoint('lg')
      else if (width >= breakpoints.md) setBreakpoint('md')
      else if (width >= breakpoints.sm) setBreakpoint('sm')
      else setBreakpoint('xs')
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return {
    breakpoint,
    isMobile: breakpoint === 'xs' || breakpoint === 'sm',
    isTablet: breakpoint === 'md',
    isDesktop: breakpoint === 'lg' || breakpoint === 'xl' || breakpoint === '2xl',
    isAtLeast: (bp: Breakpoint) => breakpoints[breakpoint] >= breakpoints[bp],
    isAtMost: (bp: Breakpoint) => breakpoints[breakpoint] <= breakpoints[bp],
  }
}
```

Update hooks index:
```tsx
// frontend/src/hooks/index.ts
export { useBreakpoint } from './useBreakpoint'
```

---

## Phase 9: Update Index Exports

### File: `frontend/src/components/dashboard/index.ts`

```tsx
export { PortfolioTable } from './PortfolioTable'
export { PortfolioTableRow } from './PortfolioTableRow'
export { HoldingCard } from './HoldingCard'
export { PerformanceChart } from './PerformanceChart'
export { Sparkline } from './Sparkline'
```

### File: `frontend/src/components/transactions/index.ts`

```tsx
export { TransactionGrid } from './TransactionGrid'
export { TransactionGridRow } from './TransactionGridRow'
export { TransactionCard } from './TransactionCard'
export { TransactionForm } from './TransactionForm'
```

---

## Phase 10: Global CSS Refinements

### File: `frontend/src/index.css`

Add any needed utility classes:

```css
/* Prevent horizontal overflow on mobile */
@layer utilities {
  .overflow-x-hidden-mobile {
    @apply overflow-x-hidden sm:overflow-x-visible;
  }
}

/* Safe area padding for notched devices */
@supports (padding: max(0px)) {
  .safe-area-inset {
    padding-left: max(1rem, env(safe-area-inset-left));
    padding-right: max(1rem, env(safe-area-inset-right));
    padding-bottom: max(1rem, env(safe-area-inset-bottom));
  }
}
```

---

## Testing Checklist

### Mobile (< 640px):
- [ ] Navigation hamburger menu works
- [ ] Summary cards show 2x2 grid
- [ ] Holdings display as cards
- [ ] Transactions display as cards
- [ ] Performance chart fits without overflow
- [ ] No horizontal scrolling anywhere
- [ ] Touch targets are at least 44x44px
- [ ] Forms are easily usable with mobile keyboard

### Tablet (768px - 1023px):
- [ ] Navigation shows full links
- [ ] Holdings table shows core columns (hide Weight, Avg Price, Sparkline)
- [ ] Transactions table works properly
- [ ] Performance chart toggles fit in one row

### Desktop (1024px+):
- [ ] All table columns visible
- [ ] Full feature set available
- [ ] Layout uses available space efficiently

### Cross-device:
- [ ] Orientation changes handled smoothly
- [ ] No content jumps or layout shifts
- [ ] Loading states display correctly at all sizes

---

## Files to Create

1. `frontend/src/components/dashboard/HoldingCard.tsx`
2. `frontend/src/components/transactions/TransactionCard.tsx`
3. `frontend/src/hooks/useBreakpoint.ts`

## Files to Modify

1. `frontend/src/components/layout/DashboardNavigation.tsx` - Mobile menu
2. `frontend/src/pages/Dashboard/Dashboard.tsx` - Responsive header, container
3. `frontend/src/components/dashboard/PortfolioTable.tsx` - Card/table switch
4. `frontend/src/components/dashboard/PortfolioTableRow.tsx` - Responsive columns
5. `frontend/src/components/dashboard/Sparkline.tsx` - Accept dimensions props
6. `frontend/src/components/dashboard/PerformanceChart.tsx` - Responsive layout
7. `frontend/src/components/transactions/TransactionGrid.tsx` - Card/table switch
8. `frontend/src/components/transactions/TransactionForm.tsx` - Responsive form layout
9. `frontend/src/components/dashboard/index.ts` - Export HoldingCard
10. `frontend/src/components/transactions/index.ts` - Export TransactionCard
11. `frontend/src/hooks/index.ts` - Export useBreakpoint (create if needed)
