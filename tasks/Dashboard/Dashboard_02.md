# Task 02: Dashboard Enhancements - Charts & Additional Columns

## Objective

Enhance the dashboard page with:
1. A portfolio performance chart widget with time period toggles (7D, 1M, 3M, YTD, 1Y)
2. New table columns: 7D % Return, Weight/Shares, Avg Price, and 1Y sparkline chart

---

## Architecture Overview

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│    Frontend     │ ──API── │    Backend      │ ──HTTP──│  Yahoo Finance  │
│  React + Chart  │         │  Spring Boot    │         │   (Chart API)   │
└─────────────────┘         └─────────────────┘         └─────────────────┘
        │                           │
        │ Recharts                  │ Historical Data
        ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│  Performance    │         │   /v8/chart     │
│  Chart Widget   │         │   Historical    │
└─────────────────┘         └─────────────────┘
```

**Key Design Decisions:**
- Historical price data → Backend (via Yahoo Finance v8 chart endpoint)
- 7D return calculation → Backend (requires historical price from 7 days ago)
- Sparkline data → Backend (returns array of closing prices)
- Portfolio performance aggregation → Backend
- Charting → Frontend (Recharts library - lightweight, React-native)
- Weight calculation → Backend (holding value / total portfolio value)

---

## Requirements Breakdown

### Feature 1: Portfolio Performance Chart Widget

| Aspect | Specification |
|--------|---------------|
| **Position** | Above the holdings table |
| **Time Periods** | 7D, 1M, 3M, YTD, 1Y (default: 1Y) |
| **Chart Type** | Area chart with gradient fill |
| **Data Points** | Daily closing prices aggregated for portfolio |
| **Y-Axis** | Portfolio total value in USD |
| **X-Axis** | Date (formatted based on period) |
| **Interactivity** | Hover tooltip showing date and value |

### Feature 2: New Table Columns

| Column | Position | Data Source | Description |
|--------|----------|-------------|-------------|
| **7D % Return** | Before "Total Return" | Backend calculated | Percentage return over last 7 days |
| **Weight/Shares** | After "Value/Cost" | Backend calculated | Portfolio weight % and share count |
| **Avg Price** | After "Weight/Shares" | Existing field | Average cost per share (already in DB) |
| **1Y Chart** | After "Avg Price" | Backend historical | Small sparkline showing 1Y price trend |

---

## Yahoo Finance Historical Data

### Endpoint Details

The v8 chart endpoint already in use supports historical data with additional parameters:

```
GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}
    ?interval={interval}
    &range={range}
```

| Range | Interval | Data Points (approx) | Use Case |
|-------|----------|---------------------|----------|
| 7d | 1d | 5-7 | 7D chart, 7D return |
| 1mo | 1d | ~22 | 1M chart |
| 3mo | 1d | ~65 | 3M chart |
| ytd | 1d | varies | YTD chart |
| 1y | 1d | ~252 | 1Y chart, sparklines |

### Response Structure (Simplified)

```json
{
  "chart": {
    "result": [{
      "meta": {
        "symbol": "AAPL",
        "regularMarketPrice": 178.25,
        "chartPreviousClose": 176.50
      },
      "timestamp": [1704067200, 1704153600, ...],
      "indicators": {
        "quote": [{
          "close": [185.50, 186.25, 184.75, ...],
          "open": [...],
          "high": [...],
          "low": [...],
          "volume": [...]
        }]
      }
    }]
  }
}
```

---

## Implementation Phases

### Phase 1: Backend - Historical Data DTOs

**File**: `backend/src/main/java/com/stocktracker/client/dto/HistoricalPrice.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPrice {
    private LocalDate date;
    private BigDecimal close;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private Long volume;
}
```

**File**: `backend/src/main/java/com/stocktracker/client/dto/HistoricalData.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalData {
    private String symbol;
    private List<HistoricalPrice> prices;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
}
```

---

### Phase 2: Backend - Enhanced Yahoo Finance Client

**File**: `backend/src/main/java/com/stocktracker/client/YahooFinanceClient.java`

Add new method for fetching historical data:

```java
/**
 * Fetch historical price data for a symbol.
 * @param symbol Stock symbol
 * @param range Time range (7d, 1mo, 3mo, ytd, 1y)
 * @return HistoricalData with daily prices
 */
public HistoricalData getHistoricalData(String symbol, String range) {
    String url = chartUrl + "/" + symbol + "?interval=1d&range=" + range;
    // Parse response and extract timestamp + close prices
    // Convert timestamps to LocalDate
    // Return HistoricalData object
}

/**
 * Fetch historical data for multiple symbols in parallel.
 * @param symbols List of stock symbols
 * @param range Time range
 * @return Map of symbol to HistoricalData
 */
public Map<String, HistoricalData> getHistoricalDataBatch(List<String> symbols, String range) {
    // Use CompletableFuture for parallel requests (same pattern as getQuotes)
}
```

---

### Phase 3: Backend - Enhanced DTOs

**File**: `backend/src/main/java/com/stocktracker/dto/response/HoldingResponse.java`

Add new fields:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {
    // Existing fields
    private Long id;
    private String symbol;
    private String companyName;
    private BigDecimal shares;
    private BigDecimal averageCost;
    private BigDecimal lastPrice;
    private BigDecimal previousClose;
    private BigDecimal totalReturnDollars;
    private BigDecimal totalReturnPercent;
    private BigDecimal currentValue;
    private BigDecimal costBasis;

    // NEW: 7D Return
    private BigDecimal sevenDayReturnPercent;    // Percentage return over last 7 days
    private BigDecimal sevenDayReturnDollars;    // Dollar return over last 7 days

    // NEW: Weight
    private BigDecimal weight;                    // Portfolio weight as percentage (0-100)

    // NEW: Sparkline data (1Y daily closes, downsampled to ~52 points for weekly)
    private List<BigDecimal> sparklineData;      // Array of closing prices for chart
}
```

**File**: `backend/src/main/java/com/stocktracker/dto/response/PortfolioResponse.java`

Add new fields:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    // Existing fields
    private List<HoldingResponse> holdings;
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalReturnDollars;
    private BigDecimal totalReturnPercent;
    private LocalDateTime pricesUpdatedAt;

    // NEW: Portfolio performance chart data
    private List<PortfolioPerformancePoint> performanceHistory;
}
```

**File**: `backend/src/main/java/com/stocktracker/dto/response/PortfolioPerformancePoint.java` (NEW)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPerformancePoint {
    private LocalDate date;
    private BigDecimal totalValue;       // Sum of (shares * closePrice) for all holdings
    private BigDecimal dailyChange;      // Change from previous day
    private BigDecimal dailyChangePercent;
}
```

---

### Phase 4: Backend - New Endpoint for Performance History

**File**: `backend/src/main/java/com/stocktracker/controller/PortfolioController.java`

Add new endpoint:

```java
@GetMapping("/performance")
@Operation(summary = "Get portfolio performance history for charting")
public ResponseEntity<ApiResponse<List<PortfolioPerformancePoint>>> getPerformanceHistory(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(defaultValue = "1y") String range) {
    Long userId = ((CustomUserDetails) userDetails).getId();
    List<PortfolioPerformancePoint> performance = portfolioService.getPerformanceHistory(userId, range);
    return ResponseEntity.ok(ApiResponse.success(performance));
}
```

**Supported range values:** `7d`, `1mo`, `3mo`, `ytd`, `1y`

---

### Phase 5: Backend - Portfolio Service Enhancements

**File**: `backend/src/main/java/com/stocktracker/service/PortfolioService.java`

Add new methods:

```java
/**
 * Calculate 7-day return for a holding.
 * Requires fetching historical price from 7 days ago.
 */
private void calculate7DayReturn(HoldingResponse holding, HistoricalData historicalData) {
    if (historicalData == null || historicalData.getPrices().isEmpty()) {
        holding.setSevenDayReturnPercent(BigDecimal.ZERO);
        holding.setSevenDayReturnDollars(BigDecimal.ZERO);
        return;
    }

    List<HistoricalPrice> prices = historicalData.getPrices();
    BigDecimal currentPrice = holding.getLastPrice();
    BigDecimal price7DaysAgo = prices.get(0).getClose(); // First element is oldest

    BigDecimal change = currentPrice.subtract(price7DaysAgo);
    BigDecimal changePercent = price7DaysAgo.compareTo(BigDecimal.ZERO) > 0
        ? change.divide(price7DaysAgo, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
        : BigDecimal.ZERO;

    holding.setSevenDayReturnPercent(changePercent);
    holding.setSevenDayReturnDollars(change.multiply(holding.getShares()));
}

/**
 * Calculate portfolio weight for a holding.
 */
private void calculateWeight(HoldingResponse holding, BigDecimal totalPortfolioValue) {
    if (totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal weight = holding.getCurrentValue()
            .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        holding.setWeight(weight);
    } else {
        holding.setWeight(BigDecimal.ZERO);
    }
}

/**
 * Extract sparkline data from historical prices (downsample to 52 weekly points).
 */
private List<BigDecimal> extractSparklineData(HistoricalData historicalData) {
    if (historicalData == null || historicalData.getPrices().isEmpty()) {
        return Collections.emptyList();
    }

    List<HistoricalPrice> prices = historicalData.getPrices();
    // Downsample to ~52 points (weekly) for 1Y data
    int step = Math.max(1, prices.size() / 52);
    List<BigDecimal> sparkline = new ArrayList<>();

    for (int i = 0; i < prices.size(); i += step) {
        sparkline.add(prices.get(i).getClose());
    }

    return sparkline;
}

/**
 * Get portfolio performance history for charting.
 */
@Cacheable(value = "performanceHistory", key = "#userId + '-' + #range")
public List<PortfolioPerformancePoint> getPerformanceHistory(Long userId, String range) {
    List<Holding> holdings = holdingRepository.findByUserIdOrderBySymbolAsc(userId);

    if (holdings.isEmpty()) {
        return Collections.emptyList();
    }

    // Fetch historical data for all symbols
    List<String> symbols = holdings.stream()
        .map(Holding::getSymbol)
        .collect(Collectors.toList());

    Map<String, HistoricalData> historicalDataMap = yahooFinanceClient.getHistoricalDataBatch(symbols, range);

    // Aggregate daily portfolio values
    // 1. Find common dates across all holdings
    // 2. For each date, calculate: sum of (shares * closePrice) for each holding
    // 3. Return sorted list of PortfolioPerformancePoint

    return aggregatePortfolioPerformance(holdings, historicalDataMap);
}

/**
 * Aggregate historical prices into portfolio performance points.
 */
private List<PortfolioPerformancePoint> aggregatePortfolioPerformance(
        List<Holding> holdings,
        Map<String, HistoricalData> historicalDataMap) {

    // Build a map of date -> sum of (shares * close price)
    Map<LocalDate, BigDecimal> dailyValues = new TreeMap<>();

    for (Holding holding : holdings) {
        HistoricalData data = historicalDataMap.get(holding.getSymbol());
        if (data == null || data.getPrices().isEmpty()) continue;

        for (HistoricalPrice price : data.getPrices()) {
            BigDecimal holdingValue = holding.getShares().multiply(price.getClose());
            dailyValues.merge(price.getDate(), holdingValue, BigDecimal::add);
        }
    }

    // Convert to PortfolioPerformancePoint list with daily changes
    List<PortfolioPerformancePoint> performance = new ArrayList<>();
    BigDecimal previousValue = null;

    for (Map.Entry<LocalDate, BigDecimal> entry : dailyValues.entrySet()) {
        BigDecimal currentValue = entry.getValue();
        BigDecimal dailyChange = previousValue != null
            ? currentValue.subtract(previousValue)
            : BigDecimal.ZERO;
        BigDecimal dailyChangePercent = previousValue != null && previousValue.compareTo(BigDecimal.ZERO) > 0
            ? dailyChange.divide(previousValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        performance.add(PortfolioPerformancePoint.builder()
            .date(entry.getKey())
            .totalValue(currentValue)
            .dailyChange(dailyChange)
            .dailyChangePercent(dailyChangePercent)
            .build());

        previousValue = currentValue;
    }

    return performance;
}
```

---

### Phase 6: Backend - Update getPortfolio Method

Modify the existing `getPortfolio` method to include the new fields:

```java
@Cacheable(value = "portfolio", key = "#userId", unless = "#result == null")
public PortfolioResponse getPortfolio(Long userId) {
    // ... existing code ...

    // Fetch 7-day historical data for all symbols
    Map<String, HistoricalData> historicalData7d = yahooFinanceClient.getHistoricalDataBatch(symbols, "7d");

    // Fetch 1-year historical data for sparklines
    Map<String, HistoricalData> historicalData1y = yahooFinanceClient.getHistoricalDataBatch(symbols, "1y");

    // Build holding responses with new fields
    List<HoldingResponse> holdingResponses = holdings.stream()
        .map(h -> {
            HoldingResponse response = buildHoldingResponse(h, quotes.get(h.getSymbol()));

            // Add 7D return
            calculate7DayReturn(response, historicalData7d.get(h.getSymbol()));

            // Add sparkline data
            response.setSparklineData(extractSparklineData(historicalData1y.get(h.getSymbol())));

            return response;
        })
        .collect(Collectors.toList());

    // Calculate total portfolio value first
    BigDecimal totalValue = holdingResponses.stream()
        .map(HoldingResponse::getCurrentValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Add weight to each holding
    holdingResponses.forEach(h -> calculateWeight(h, totalValue));

    // Build and return portfolio response
    return buildPortfolioResponse(holdingResponses);
}
```

---

### Phase 7: Backend - Caching for Historical Data

**File**: `backend/src/main/java/com/stocktracker/config/CacheConfig.java`

Add cache for historical data (longer TTL since historical data doesn't change):

```java
@Bean
public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();

    cacheManager.setCaches(Arrays.asList(
        // Existing portfolio cache (2 minutes)
        new CaffeineCache("portfolio",
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(2))
                .maximumSize(1000)
                .build()),

        // NEW: Historical data cache (30 minutes - data doesn't change often)
        new CaffeineCache("historicalData",
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(500)
                .build()),

        // NEW: Performance history cache (10 minutes)
        new CaffeineCache("performanceHistory",
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(200)
                .build())
    ));

    return cacheManager;
}
```

---

### Phase 8: Frontend - Install Recharts

Recharts is a lightweight, composable charting library built on React and D3.

```bash
cd frontend
npm install recharts
```

**Why Recharts:**
- React-first design with composable components
- Lightweight (~50KB gzipped)
- Good TypeScript support
- Responsive by default
- SVG-based (crisp rendering)

---

### Phase 9: Frontend - API Client Updates

**File**: `frontend/src/services/api/portfolioApi.ts`

Update interfaces and add new endpoint:

```typescript
// Existing interface updates
export interface HoldingResponse {
  id: number;
  symbol: string;
  companyName: string;
  shares: number;
  averageCost: number;
  lastPrice: number;
  previousClose: number | null;
  totalReturnDollars: number;
  totalReturnPercent: number;
  currentValue: number;
  costBasis: number;

  // NEW fields
  sevenDayReturnPercent: number;
  sevenDayReturnDollars: number;
  weight: number;
  sparklineData: number[];
}

// NEW: Performance history point
export interface PortfolioPerformancePoint {
  date: string;           // ISO date string
  totalValue: number;
  dailyChange: number;
  dailyChangePercent: number;
}

// NEW: Time range type
export type TimeRange = '7d' | '1mo' | '3mo' | 'ytd' | '1y';

export const portfolioApi = {
  // Existing methods...

  // NEW: Get performance history
  getPerformanceHistory: async (range: TimeRange = '1y'): Promise<PortfolioPerformancePoint[]> => {
    const response = await axiosInstance.get(`/portfolio/performance?range=${range}`);
    return response.data.data;
  }
};
```

---

### Phase 10: Frontend - Custom Hook for Performance Data

**File**: `frontend/src/hooks/usePortfolioPerformance.ts` (NEW)

```typescript
import { useState, useEffect, useCallback } from 'react';
import { portfolioApi, PortfolioPerformancePoint, TimeRange } from '../services/api/portfolioApi';

export function usePortfolioPerformance(initialRange: TimeRange = '1y') {
  const [range, setRange] = useState<TimeRange>(initialRange);
  const [data, setData] = useState<PortfolioPerformancePoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPerformance = useCallback(async (selectedRange: TimeRange) => {
    try {
      setLoading(true);
      setError(null);
      const result = await portfolioApi.getPerformanceHistory(selectedRange);
      setData(result);
    } catch (err) {
      setError('Failed to load performance data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPerformance(range);
  }, [range, fetchPerformance]);

  const changeRange = useCallback((newRange: TimeRange) => {
    setRange(newRange);
  }, []);

  return { data, range, loading, error, changeRange };
}
```

---

### Phase 11: Frontend - Performance Chart Component

**File**: `frontend/src/components/dashboard/PerformanceChart.tsx` (NEW)

```typescript
import { useMemo } from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import type { TimeRange, PortfolioPerformancePoint } from '../../services/api/portfolioApi';
import { formatCurrency } from '../../utils/stockFormatters';

interface PerformanceChartProps {
  data: PortfolioPerformancePoint[];
  range: TimeRange;
  onRangeChange: (range: TimeRange) => void;
  loading?: boolean;
}

const TIME_RANGES: { value: TimeRange; label: string }[] = [
  { value: '7d', label: '7D' },
  { value: '1mo', label: '1M' },
  { value: '3mo', label: '3M' },
  { value: 'ytd', label: 'YTD' },
  { value: '1y', label: '1Y' },
];

export function PerformanceChart({ data, range, onRangeChange, loading }: PerformanceChartProps) {
  // Format date based on range
  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    if (range === '7d') {
      return date.toLocaleDateString('en-US', { weekday: 'short' });
    }
    if (range === '1mo' || range === '3mo') {
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }
    return date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' });
  };

  // Calculate period return
  const periodReturn = useMemo(() => {
    if (data.length < 2) return { dollars: 0, percent: 0 };
    const first = data[0].totalValue;
    const last = data[data.length - 1].totalValue;
    const dollars = last - first;
    const percent = first > 0 ? (dollars / first) * 100 : 0;
    return { dollars, percent };
  }, [data]);

  const isPositive = periodReturn.dollars >= 0;
  const chartColor = isPositive ? '#10b981' : '#ef4444'; // emerald-500 / red-500
  const gradientId = 'performanceGradient';

  return (
    <div className="rounded-xl border border-border bg-white p-6 shadow-soft">
      {/* Header with range toggles */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Portfolio Performance</h2>
          <div className={`text-2xl font-bold ${isPositive ? 'text-emerald-600' : 'text-red-500'}`}>
            {isPositive ? '+' : ''}{formatCurrency(periodReturn.dollars)}
            <span className="ml-2 text-base font-medium">
              ({isPositive ? '+' : ''}{periodReturn.percent.toFixed(2)}%)
            </span>
          </div>
        </div>

        {/* Time range toggle buttons */}
        <div className="flex gap-1 rounded-lg bg-slate-100 p-1">
          {TIME_RANGES.map(({ value, label }) => (
            <button
              key={value}
              onClick={() => onRangeChange(value)}
              className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors
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

      {/* Chart */}
      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <span className="text-slate-500">Loading chart...</span>
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={chartColor} stopOpacity={0.3} />
                <stop offset="100%" stopColor={chartColor} stopOpacity={0.05} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
            <XAxis
              dataKey="date"
              tickFormatter={formatDate}
              tick={{ fontSize: 12, fill: '#64748b' }}
              axisLine={{ stroke: '#e2e8f0' }}
              tickLine={false}
            />
            <YAxis
              tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`}
              tick={{ fontSize: 12, fill: '#64748b' }}
              axisLine={false}
              tickLine={false}
              width={60}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'white',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              }}
              formatter={(value: number) => [formatCurrency(value), 'Portfolio Value']}
              labelFormatter={(label) => new Date(label).toLocaleDateString('en-US', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            />
            <Area
              type="monotone"
              dataKey="totalValue"
              stroke={chartColor}
              strokeWidth={2}
              fill={`url(#${gradientId})`}
            />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
```

---

### Phase 12: Frontend - Sparkline Component

**File**: `frontend/src/components/dashboard/Sparkline.tsx` (NEW)

```typescript
import { useMemo } from 'react';
import { LineChart, Line, ResponsiveContainer } from 'recharts';

interface SparklineProps {
  data: number[];
  width?: number;
  height?: number;
}

export function Sparkline({ data, width = 80, height = 32 }: SparklineProps) {
  const chartData = useMemo(() =>
    data.map((value, index) => ({ index, value })),
    [data]
  );

  // Determine color based on trend (first vs last value)
  const isPositive = data.length >= 2 && data[data.length - 1] >= data[0];
  const strokeColor = isPositive ? '#10b981' : '#ef4444';

  if (data.length < 2) {
    return <div className="w-20 h-8 bg-slate-100 rounded" />;
  }

  return (
    <div style={{ width, height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData}>
          <Line
            type="monotone"
            dataKey="value"
            stroke={strokeColor}
            strokeWidth={1.5}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
```

---

### Phase 13: Frontend - Update Portfolio Table Components

**File**: `frontend/src/components/dashboard/PortfolioTable.tsx`

Update table headers for new columns:

```typescript
import type { HoldingResponse } from '../../services/api/portfolioApi';
import { PortfolioTableRow } from './PortfolioTableRow';

interface PortfolioTableProps {
  holdings: HoldingResponse[];
}

export function PortfolioTable({ holdings }: PortfolioTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-white shadow-soft">
      <table className="w-full">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-6 py-4 text-left text-sm font-semibold uppercase tracking-wide text-slate-600">
              Symbol
            </th>
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Last Price
            </th>
            {/* NEW: 7D Return column */}
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              7D Return
            </th>
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Total Return
            </th>
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Value / Cost
            </th>
            {/* NEW: Weight/Shares column */}
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Weight / Shares
            </th>
            {/* NEW: Avg Price column */}
            <th className="px-6 py-4 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
              Avg Price
            </th>
            {/* NEW: 1Y Chart column */}
            <th className="px-6 py-4 text-center text-sm font-semibold uppercase tracking-wide text-slate-600">
              1Y Chart
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {holdings.map((holding) => (
            <PortfolioTableRow key={holding.id} holding={holding} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

**File**: `frontend/src/components/dashboard/PortfolioTableRow.tsx`

Add new column cells:

```typescript
import type { HoldingResponse } from '../../services/api/portfolioApi';
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters';
import { Sparkline } from './Sparkline';

interface PortfolioTableRowProps {
  holding: HoldingResponse;
}

export function PortfolioTableRow({ holding }: PortfolioTableRowProps) {
  const returnClass = getReturnColorClass(holding.totalReturnDollars);
  const sevenDayReturnClass = getReturnColorClass(holding.sevenDayReturnDollars);

  return (
    <tr className="transition-colors hover:bg-slate-50">
      {/* Symbol Column */}
      <td className="px-6 py-4">
        <div className="font-semibold text-slate-900">{holding.symbol}</div>
        <div className="text-sm text-slate-500">{holding.companyName}</div>
      </td>

      {/* Last Price Column */}
      <td className="px-6 py-4 text-right font-medium text-slate-900">
        {formatCurrency(holding.lastPrice)}
      </td>

      {/* NEW: 7D Return Column */}
      <td className={`px-6 py-4 text-right ${sevenDayReturnClass}`}>
        <div className="font-medium">{formatPercent(holding.sevenDayReturnPercent)}</div>
      </td>

      {/* Total Return Column */}
      <td className={`px-6 py-4 text-right ${returnClass}`}>
        <div className="font-medium">{formatPercent(holding.totalReturnPercent)}</div>
        <div className="text-sm">{formatCurrency(holding.totalReturnDollars)}</div>
      </td>

      {/* Value/Cost Column */}
      <td className="px-6 py-4 text-right">
        <span className="font-semibold text-slate-900">
          {formatCurrency(holding.currentValue)}
        </span>
        <span className="text-slate-400"> / </span>
        <span className="text-slate-500">{formatCurrency(holding.costBasis)}</span>
      </td>

      {/* NEW: Weight/Shares Column */}
      <td className="px-6 py-4 text-right">
        <div className="font-medium text-slate-900">{holding.weight.toFixed(1)}%</div>
        <div className="text-sm text-slate-500">{holding.shares.toFixed(2)} shares</div>
      </td>

      {/* NEW: Avg Price Column */}
      <td className="px-6 py-4 text-right font-medium text-slate-900">
        {formatCurrency(holding.averageCost)}
      </td>

      {/* NEW: 1Y Chart Column */}
      <td className="px-6 py-4">
        <div className="flex justify-center">
          <Sparkline data={holding.sparklineData} />
        </div>
      </td>
    </tr>
  );
}
```

---

### Phase 14: Frontend - Update Dashboard Page

**File**: `frontend/src/pages/Dashboard/Dashboard.tsx` (or equivalent)

Integrate the performance chart above the table:

```typescript
import { usePortfolio } from '../../hooks/usePortfolio';
import { usePortfolioPerformance } from '../../hooks/usePortfolioPerformance';
import { PortfolioTable } from '../../components/dashboard/PortfolioTable';
import { PerformanceChart } from '../../components/dashboard/PerformanceChart';
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters';

export function Dashboard() {
  const { portfolio, loading: portfolioLoading, error: portfolioError, refresh } = usePortfolio();
  const { data: performanceData, range, loading: chartLoading, changeRange } = usePortfolioPerformance('1y');

  if (portfolioLoading) {
    return <div className="flex justify-center p-8">Loading portfolio...</div>;
  }

  if (portfolioError) {
    return <div className="p-8 text-red-500">{portfolioError}</div>;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <header className="mb-8 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Portfolio Overview</h1>
        <button
          onClick={refresh}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700"
        >
          Refresh Prices
        </button>
      </header>

      {/* Portfolio Summary Cards */}
      {portfolio && (
        <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* ... existing summary cards ... */}
        </div>
      )}

      {/* NEW: Performance Chart Widget */}
      <section className="mb-8">
        <PerformanceChart
          data={performanceData}
          range={range}
          onRangeChange={changeRange}
          loading={chartLoading}
        />
      </section>

      {/* Holdings Table */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Your Holdings</h2>
          <span className="text-sm text-slate-500">
            Prices updated: {portfolio?.pricesUpdatedAt
              ? new Date(portfolio.pricesUpdatedAt).toLocaleTimeString()
              : '-'}
          </span>
        </div>
        {portfolio?.holdings.length ? (
          <PortfolioTable holdings={portfolio.holdings} />
        ) : (
          <p className="text-slate-500">No holdings yet.</p>
        )}
      </section>
    </div>
  );
}
```

---

### Phase 15: Frontend - Component Exports

**File**: `frontend/src/components/dashboard/index.ts`

Update barrel export:

```typescript
export { PortfolioTable } from './PortfolioTable';
export { PortfolioTableRow } from './PortfolioTableRow';
export { PerformanceChart } from './PerformanceChart';
export { Sparkline } from './Sparkline';
```

---

## API Specification

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/portfolio` | Get portfolio with enhanced data | Yes |
| GET | `/api/portfolio/refresh` | Force refresh (bypasses cache) | Yes |
| GET | `/api/portfolio/performance?range={range}` | Get performance history | Yes |

### Enhanced Portfolio Response Format

```json
{
  "success": true,
  "data": {
    "holdings": [
      {
        "id": 1,
        "symbol": "AAPL",
        "companyName": "Apple Inc.",
        "shares": 50.0,
        "averageCost": 142.50,
        "lastPrice": 178.25,
        "previousClose": 176.50,
        "totalReturnDollars": 1787.50,
        "totalReturnPercent": 25.08,
        "currentValue": 8912.50,
        "costBasis": 7125.00,
        "sevenDayReturnPercent": 3.45,
        "sevenDayReturnDollars": 298.50,
        "weight": 19.52,
        "sparklineData": [172.5, 173.2, 171.8, ..., 178.25]
      }
    ],
    "totalValue": 45678.90,
    "totalCost": 38500.00,
    "totalReturnDollars": 7178.90,
    "totalReturnPercent": 18.65,
    "pricesUpdatedAt": "2024-01-15T10:30:00"
  }
}
```

### Performance History Response Format

```json
{
  "success": true,
  "data": [
    {
      "date": "2023-01-15",
      "totalValue": 35500.00,
      "dailyChange": 0,
      "dailyChangePercent": 0
    },
    {
      "date": "2023-01-16",
      "totalValue": 35780.50,
      "dailyChange": 280.50,
      "dailyChangePercent": 0.79
    }
  ]
}
```

---

## File Structure Summary

```
backend/
├── src/main/java/com/stocktracker/
│   ├── client/
│   │   ├── YahooFinanceClient.java           # UPDATE: Add getHistoricalData methods
│   │   └── dto/
│   │       ├── StockQuote.java               # EXISTING
│   │       ├── HistoricalPrice.java          # NEW
│   │       └── HistoricalData.java           # NEW
│   ├── dto/response/
│   │   ├── HoldingResponse.java              # UPDATE: Add new fields
│   │   ├── PortfolioResponse.java            # EXISTING
│   │   └── PortfolioPerformancePoint.java    # NEW
│   ├── service/
│   │   └── PortfolioService.java             # UPDATE: Add calculations
│   ├── controller/
│   │   └── PortfolioController.java          # UPDATE: Add performance endpoint
│   └── config/
│       └── CacheConfig.java                  # UPDATE: Add cache configs

frontend/
├── package.json                              # UPDATE: Add recharts dependency
├── src/
│   ├── services/api/
│   │   └── portfolioApi.ts                   # UPDATE: Add types and endpoint
│   ├── hooks/
│   │   ├── usePortfolio.ts                   # EXISTING
│   │   └── usePortfolioPerformance.ts        # NEW
│   ├── components/dashboard/
│   │   ├── index.ts                          # UPDATE: Add exports
│   │   ├── PortfolioTable.tsx                # UPDATE: Add new columns
│   │   ├── PortfolioTableRow.tsx             # UPDATE: Add new cells
│   │   ├── PerformanceChart.tsx              # NEW
│   │   └── Sparkline.tsx                     # NEW
│   └── pages/Dashboard/
│       └── Dashboard.tsx                     # UPDATE: Add chart widget
```

---

## Implementation Order

### Backend (Phase 1-7)
1. Create `HistoricalPrice.java` and `HistoricalData.java` DTOs
2. Add `getHistoricalData` and `getHistoricalDataBatch` methods to `YahooFinanceClient`
3. Create `PortfolioPerformancePoint.java` DTO
4. Update `HoldingResponse.java` with new fields
5. Add calculation methods to `PortfolioService` (7D return, weight, sparkline extraction)
6. Add `getPerformanceHistory` method to `PortfolioService`
7. Add `/performance` endpoint to `PortfolioController`
8. Update `CacheConfig` with new caches
9. Test API endpoints with Postman/curl

### Frontend (Phase 8-15)
10. Install recharts: `npm install recharts`
11. Update `portfolioApi.ts` with new types and endpoint
12. Create `usePortfolioPerformance.ts` hook
13. Create `Sparkline.tsx` component
14. Create `PerformanceChart.tsx` component
15. Update `PortfolioTable.tsx` with new column headers
16. Update `PortfolioTableRow.tsx` with new column cells
17. Update Dashboard page to include PerformanceChart
18. Update barrel exports in `index.ts`
19. Test end-to-end

---

## Error Handling

### Backend
- Yahoo Finance historical data timeout → Return empty array
- Missing historical data for symbol → Set 7D return to 0, empty sparkline
- Date parsing errors → Log warning, skip data point
- Aggregation with missing dates → Use available dates only

### Frontend
- Empty performance data → Show "No data available" message
- Sparkline with insufficient data → Show placeholder
- Chart loading error → Show error message with retry option
- 7D return null → Display "N/A" or "--"

---

## Performance Considerations

### Backend
- Historical data cache (30 min TTL) reduces Yahoo Finance calls
- Parallel fetching for multiple symbols using CompletableFuture
- Downsample sparkline data to ~52 points (weekly) to reduce payload size
- Consider lazy loading sparklines only when table is scrolled into view

### Frontend
- Recharts uses SVG (lightweight, crisp rendering)
- Sparklines are minimal (no axes, tooltips) for performance
- Time range change only re-fetches performance data, not holdings
- Consider virtualized table for large portfolios (future enhancement)

---

## Design Considerations

### Chart Styling
- Use brand colors for positive (emerald-500) and negative (red-500) trends
- Subtle gradient fill for area chart
- Clean grid lines (horizontal only)
- Responsive container adapts to screen size

### Table Column Widths
| Column | Suggested Width | Alignment |
|--------|-----------------|-----------|
| Symbol | auto (min 120px) | Left |
| Last Price | 100px | Right |
| 7D Return | 90px | Right |
| Total Return | 120px | Right |
| Value/Cost | 140px | Right |
| Weight/Shares | 110px | Right |
| Avg Price | 100px | Right |
| 1Y Chart | 100px | Center |

### Responsive Behavior
- Desktop: All columns visible
- Tablet: Consider hiding 7D Return or Avg Price
- Mobile: Horizontal scroll with sticky Symbol column

---

## Acceptance Criteria

- [ ] Performance chart displays above holdings table
- [ ] Time period toggles (7D, 1M, 3M, YTD, 1Y) work correctly
- [ ] Default time period is 1Y
- [ ] Chart shows portfolio total value over time
- [ ] Chart color reflects positive (green) or negative (red) trend
- [ ] Hover tooltip shows date and value
- [ ] 7D % Return column shows 7-day performance
- [ ] Weight/Shares column shows portfolio allocation % and share count
- [ ] Avg Price column shows average cost per share
- [ ] 1Y Chart column shows sparkline for each holding
- [ ] Sparkline color reflects positive (green) or negative (red) trend
- [ ] API caches historical data appropriately
- [ ] Loading states shown during data fetch
- [ ] Error states handled gracefully
- [ ] Responsive on tablet and mobile devices

---

## Future Enhancements (Out of Scope)

- [ ] Comparison with benchmark indices (S&P 500, NASDAQ)
- [ ] Multiple chart types (line, candlestick)
- [ ] Custom date range picker
- [ ] Download chart as image
- [ ] Animated sparklines
- [ ] Click on sparkline to expand to full chart modal
- [ ] Sector allocation pie chart
- [ ] Performance attribution analysis
