# Contract: Performance API

Requires a valid bearer JWT; scoped to the current user (FR-024). All monetary
figures are reported in the user's **base currency** (FR-031), converted from each
instrument's native currency via `CurrencyService`.

## GET /api/performance

Return realized + unrealized P&L, a cumulative-return series, time-weighted
return, and per-holding contributions for a window.

**Query params**
- `window` (optional, default `1Y`): `1M` | `3M` | `6M` | `1Y` | `YTD` | `ALL`.
- `method` (optional, default `fifo`): `fifo` | `lifo` | `specific` (lot-matching).
  `specific` is engine-supported; the UI offers `fifo` | `lifo` only in v1
  (lot-selection UI deferred per clarification).

**200 Response**
```json
{
  "window": "1Y",
  "method": "fifo",
  "baseCurrency": "USD",
  "realizedPnL": 1234.56,
  "unrealizedPnL": 5678.90,
  "timeWeightedReturnPct": 12.34,
  "closedLots": [
    {
      "symbol": "D05.SI",
      "currency": "SGD",
      "openDate": "2023-02-01",
      "closeDate": "2025-11-15",
      "quantity": 100,
      "costBasisNative": 4000.00,
      "proceedsNative": 4500.00,
      "realizedPnLNative": 500.00,
      "realizedPnLBase": 370.40
    }
  ],
  "incomeEvents": [
    {
      "symbol": "GOOGL",
      "currency": "USD",
      "date": "2026-06-03",
      "type": "dividend",
      "amountNative": 200.00,
      "amountBase": 200.00
    }
  ],
  "returnSeries": [
    { "date": "2025-06-12", "cumulativeReturnPct": 0.0 },
    { "date": "2026-06-12", "cumulativeReturnPct": 12.34 }
  ],
  "contributions": [
    { "symbol": "AAPL", "contributionPct": 7.10 },
    { "symbol": "D05.SI", "contributionPct": 5.24 }
  ]
}
```

**Behavior**
- Realized P&L per closed lot via `LotMatchingService` under `method`, splits
  applied retroactively (FR-014/015, SC-005/006). Lots compute in native currency
  (`*Native`) and are converted to base (`realizedPnLBase`).
- Dividend transactions are reported as `incomeEvents` and included in
  `realizedPnL` net of fees. They are not `closedLots`, because no lot is
  consumed.
- `returnSeries` + `timeWeightedReturnPct` derive from the daily portfolio
  valuation (split-adjusted holdings × daily closes, each day converted to base
  via that day's FX rate) over `window`, chaining daily sub-period returns
  (FR-016/017).
- `contributions` sum to the overall return within rounding tolerance (SC-007).
- Missing daily history for the window triggers `HistoricalBackfillService`
  (FR-025). A missing FX pair uses the last known rate, marked stale.
- No closed positions → `realizedPnL: 0`, empty `closedLots`, valid (flat) series
  — never an error (edge case).
