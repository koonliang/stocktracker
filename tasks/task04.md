# Task 04: Dashboard Stock Portfolio Table

## Objective

Update the dashboard page to display a stock portfolio table showing the user's holdings with real-time pricing information.

---

## Table Specification

| Column | Description |
|--------|-------------|
| **Symbol** | Stock ticker symbol (e.g., AAPL, TSLA, GOOGL) |
| **Last Price** | The most recent trading price for the stock |
| **Total Return** | Personal gain/loss shown as both percentage (%) and dollar ($) amount |
| **Value/Cost** | Current market value vs. total cost basis (e.g., $5,240 / $4,800) |

---

## Implementation Phases

### Phase 1: TypeScript Interfaces (Frontend Mock)

**File**: `frontend/src/types/stock.ts`

```typescript
export interface StockHolding {
  id: number
  symbol: string
  companyName: string
  shares: number
  averageCost: number      // Cost per share
  lastPrice: number        // Current market price
  previousClose?: number   // For daily change calculation
}

export interface PortfolioSummary {
  totalValue: number
  totalCost: number
  totalReturn: number
  totalReturnPercent: number
}

// Computed helper type for table display
export interface StockTableRow {
  symbol: string
  companyName: string
  lastPrice: number
  totalReturnDollars: number   // (lastPrice - avgCost) * shares
  totalReturnPercent: number   // ((lastPrice - avgCost) / avgCost) * 100
  currentValue: number         // lastPrice * shares
  costBasis: number            // avgCost * shares
}
```

---

### Phase 2: Mock Data

**File**: `frontend/src/mocks/portfolioData.ts`

Create realistic mock data for 5-8 holdings:

| Symbol | Company | Shares | Avg Cost | Last Price |
|--------|---------|--------|----------|------------|
| AAPL | Apple Inc. | 50 | $142.50 | $178.25 |
| MSFT | Microsoft Corp | 25 | $285.00 | $378.50 |
| GOOGL | Alphabet Inc | 10 | $125.30 | $142.80 |
| TSLA | Tesla Inc | 15 | $248.00 | $185.60 |
| NVDA | NVIDIA Corp | 20 | $450.00 | $875.40 |
| AMZN | Amazon.com | 30 | $135.00 | $178.90 |

---

### Phase 3: Utility Functions

**File**: `frontend/src/utils/stockCalculations.ts`

```typescript
// Calculate derived values for table display
export function calculateStockTableRow(holding: StockHolding): StockTableRow

// Format currency with proper symbols and decimals
export function formatCurrency(value: number): string

// Format percentage with sign indicator
export function formatPercent(value: number): string

// Get CSS class for positive/negative returns
export function getReturnColorClass(value: number): string
```

---

### Phase 4: Dashboard Components

#### 4.1 Portfolio Table Component

**File**: `frontend/src/components/dashboard/PortfolioTable.tsx`

Structure:
```
<div className="portfolio-table-container">
  <table>
    <thead>
      <tr>
        <th>Symbol</th>
        <th>Last Price</th>
        <th>Total Return</th>
        <th>Value / Cost</th>
      </tr>
    </thead>
    <tbody>
      {holdings.map(holding => <PortfolioTableRow />)}
    </tbody>
  </table>
</div>
```

Features:
- Responsive design (horizontal scroll on mobile)
- Hover states on rows (lift effect per design system)
- Color-coded returns (green for gains, red for losses)
- Company name shown as subtitle under symbol

#### 4.2 Portfolio Table Row Component

**File**: `frontend/src/components/dashboard/PortfolioTableRow.tsx`

Layout for each column:
1. **Symbol Column**:
   - Primary: Ticker symbol (bold, larger)
   - Secondary: Company name (muted, smaller)

2. **Last Price Column**:
   - Formatted currency: `$178.25`

3. **Total Return Column**:
   - Two lines:
     - Percentage: `+25.08%` (green) or `-25.08%` (red)
     - Dollar amount: `+$1,787.50` or `-$1,787.50`

4. **Value/Cost Column**:
   - Format: `$8,912.50 / $7,125.00`
   - Current value (bold) / Cost basis (muted)

---

### Phase 5: Dashboard Page Layout

**File**: `frontend/src/pages/Dashboard/Dashboard.tsx`

```
<Dashboard>
  <header className="dashboard-header">
    <h1>Portfolio Overview</h1>
    <PortfolioSummaryCard />  <!-- Optional: Total value, total return -->
  </header>

  <main className="dashboard-content">
    <section className="holdings-section">
      <div className="section-header">
        <h2>Your Holdings</h2>
        <!-- Future: Add Stock button -->
      </div>
      <PortfolioTable holdings={mockHoldings} />
    </section>
  </main>
</Dashboard>
```

---

### Phase 6: Styling (Following Design System)

Use Tailwind CSS following "Corporate Trust" design system:

**Table Container:**
```css
- Background: white (#FFFFFF)
- Border radius: rounded-xl
- Shadow: shadow-soft (indigo-tinted)
- Border: border border-border
```

**Table Header:**
```css
- Background: bg-slate-50
- Text: text-slate-600 font-semibold text-sm uppercase tracking-wide
- Padding: px-6 py-4
```

**Table Rows:**
```css
- Border bottom: border-b border-slate-100
- Hover: hover:bg-slate-50 transition-colors
- Padding: px-6 py-4
```

**Return Colors:**
```css
- Positive: text-emerald-600 (accent color)
- Negative: text-red-500
- Neutral: text-slate-600
```

---

## File Structure Summary

```
frontend/src/
├── types/
│   └── stock.ts                    # NEW: Stock/Portfolio interfaces
├── mocks/
│   └── portfolioData.ts            # NEW: Mock holdings data
├── utils/
│   └── stockCalculations.ts        # NEW: Formatting & calculations
├── components/
│   └── dashboard/
│       ├── index.ts                # NEW: Barrel export
│       ├── PortfolioTable.tsx      # NEW: Main table component
│       └── PortfolioTableRow.tsx   # NEW: Table row component
└── pages/
    └── Dashboard/
        └── Dashboard.tsx           # UPDATE: Integrate table
```

---

## Implementation Order

1. **Create type definitions** (`types/stock.ts`)
2. **Create utility functions** (`utils/stockCalculations.ts`)
3. **Create mock data** (`mocks/portfolioData.ts`)
4. **Build PortfolioTableRow component** (`components/dashboard/PortfolioTableRow.tsx`)
5. **Build PortfolioTable component** (`components/dashboard/PortfolioTable.tsx`)
6. **Create barrel export** (`components/dashboard/index.ts`)
7. **Update Dashboard page** (`pages/Dashboard/Dashboard.tsx`)
8. **Test responsive behavior** (mobile, tablet, desktop)

---

## Design Considerations

### Responsive Behavior
- Desktop: Full table with all columns visible
- Tablet: Slight padding reduction
- Mobile: Horizontal scroll with sticky first column (symbol)

### Accessibility
- Proper semantic table markup (`<table>`, `<thead>`, `<tbody>`)
- Screen reader labels for return values (e.g., "gain" or "loss")
- Sufficient color contrast (especially for red/green values)
- Focus states for interactive elements

### Performance
- Memoize row calculations if list grows large
- Consider virtualization for 50+ holdings (future)

---

## Future Enhancements (Out of Scope)

- [ ] Sortable columns (by symbol, return %, value)
- [ ] Search/filter holdings
- [ ] Add/Edit/Delete holdings
- [ ] Real-time price updates via WebSocket
- [ ] Pagination for large portfolios
- [ ] Export to CSV functionality
- [ ] Connect to backend API (replace mock data)

---

## Acceptance Criteria

- [ ] Dashboard displays portfolio table with 4 columns
- [ ] Mock data shows at least 5 stock holdings
- [ ] Positive returns display in green (emerald-600)
- [ ] Negative returns display in red
- [ ] Table follows "Corporate Trust" design system
- [ ] Responsive on mobile devices
- [ ] Total Return shows both % and $ values
- [ ] Value/Cost shows both values with clear distinction
