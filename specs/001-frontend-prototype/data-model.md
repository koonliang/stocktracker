# Phase 1 Data Model: StockTracker Frontend Prototype

Scope: client-side data only. All entities live in memory at runtime; user-authored
entities (Transaction, Watchlist) are persisted to `localStorage` via Zustand's
`persist` middleware. Seed entities (Ticker, PriceBar, KeyStats) are loaded from
bundled JSON and are read-only at runtime.

All shapes below are expressed as TypeScript-leaning pseudocode; the concrete
Zod schemas live in `frontend/src/lib/csv.ts` (for Transaction) and
`frontend/src/lib/seed.ts` (for seed entities).

---

## Entities

### Ticker (seed, read-only)

Represents a tradable instrument in the bundled catalog.

| Field    | Type   | Notes                                   |
|----------|--------|-----------------------------------------|
| symbol   | string | Uppercase, 1–5 chars. Primary key.      |
| name     | string | Company or fund name.                    |
| sector   | string | One of a fixed enum (Tech, Health, ...). |
| exchange | string | e.g., NASDAQ, NYSE.                      |

**Validation**:
- `symbol` matches `^[A-Z]{1,5}$`.
- `symbol` is unique across the catalog.

---

### PriceBar (seed, read-only)

Daily OHLCV for a ticker. Stored as an array keyed by symbol in `prices.json`.

| Field  | Type            | Notes                                 |
|--------|-----------------|---------------------------------------|
| date   | string (ISO)    | `YYYY-MM-DD`. Unique per symbol.      |
| open   | number          | > 0.                                  |
| high   | number          | >= max(open, close).                  |
| low    | number          | <= min(open, close); > 0.             |
| close  | number          | > 0. Drives current price (last bar). |
| volume | number          | Integer, >= 0.                        |

**Derived**:
- `currentPrice(symbol) = last(priceBars[symbol]).close`
- `dayChange(symbol) = last.close - prev.close`
- `dayChangePct(symbol) = dayChange / prev.close`

---

### KeyStats (seed, read-only)

Per-ticker statistics rendered on the Analysis view (FR-012).

| Field        | Type   | Notes                                  |
|--------------|--------|----------------------------------------|
| symbol       | string | FK → Ticker.symbol.                    |
| open         | number | Latest session open.                   |
| high         | number | Latest session high.                   |
| low          | number | Latest session low.                    |
| previousClose| number | Prior session close.                   |
| volume       | number | Latest session volume.                 |
| week52High   | number | Rolling 52-week high.                  |
| week52Low    | number | Rolling 52-week low.                   |
| marketCap    | number | In USD.                                |
| peRatio      | number \| null | null if not applicable.        |

---

### Transaction (user, persisted)

A single buy or sell event recorded by the user, either via the (optional)
manual-entry dialog or via CSV import. Multiple Transactions aggregate into a
Holding.

| Field    | Type    | Notes                                              |
|----------|---------|----------------------------------------------------|
| id       | string  | UUID (v4). Generated client-side on create.        |
| date     | string  | ISO `YYYY-MM-DD`.                                  |
| ticker   | string  | Uppercase; must exist in the Ticker catalog.       |
| type     | enum    | `"buy"` \| `"sell"`.                               |
| quantity | number  | > 0. Fractional shares allowed (up to 6 decimals). |
| price    | number  | > 0. Per-share execution price in USD.             |
| fees     | number  | >= 0. Defaults to 0 if omitted.                    |

**Validation rules (enforced by Zod, reused by CSV importer)**:
- `date` parseable as ISO date and not in the future.
- `ticker` matches `^[A-Z]{1,5}$` AND exists in the seeded catalog (unknown
  tickers are rejected → invalid row in preview, FR-015).
- `type` ∈ {`buy`, `sell`}, case-insensitive on input but normalized to
  lowercase on store.
- `quantity` > 0.
- `price` > 0.
- `fees` >= 0.

**State transitions**:
- `draft` (in import preview, may be invalid) → `committed` (written to store)
  or `rejected` (excluded from import).
- Once committed, Transactions are immutable for the prototype (delete is
  allowed; edit is out of scope).

---

### Holding (derived, not persisted)

Computed from the user's Transactions on demand. Not stored; recomputed in
selectors / memoized hooks (`lib/portfolio.ts`).

| Field              | Type   | Derivation                                                  |
|--------------------|--------|-------------------------------------------------------------|
| ticker             | string | Group key over committed transactions.                      |
| shares             | number | Σ buy.quantity − Σ sell.quantity (must be >= 0).            |
| averageCost        | number | Cost-basis of remaining shares using weighted-average method (running avg reset is not applied; cost basis is reduced proportionally on sell). |
| costBasis          | number | `shares * averageCost`.                                     |
| currentPrice       | number | `currentPrice(ticker)` from PriceBar.                       |
| marketValue        | number | `shares * currentPrice`.                                    |
| unrealizedPnL      | number | `marketValue - costBasis`.                                  |
| unrealizedPnLPct   | number | `unrealizedPnL / costBasis` (0 if costBasis = 0).           |
| dayChange          | number | `shares * (currentPrice − prevClose)`.                      |
| dayChangePct       | number | `(currentPrice − prevClose) / prevClose`.                   |
| weight             | number | `marketValue / Σ marketValue` across all holdings.          |

**Derivation notes**:
- Weighted-average cost basis is the simplest method that round-trips cleanly
  through CSV re-import (SC-008). FIFO/LIFO/specific-lot are out of scope.
- A position with `shares == 0` is considered closed and excluded from the
  dashboard (but its transactions remain in the export).
- A sell that exceeds current shares is rejected at commit time (invalid in
  preview if detected by re-running the reducer over the whole imported batch
  in order; for this prototype, per-row validation is sufficient and a
  post-import guard logs a warning).

---

### Portfolio (derived, not persisted)

Aggregate view across all Holdings.

| Field              | Type   | Derivation                                          |
|--------------------|--------|-----------------------------------------------------|
| totalMarketValue   | number | Σ Holding.marketValue                               |
| totalCostBasis     | number | Σ Holding.costBasis                                 |
| totalUnrealizedPnL | number | totalMarketValue − totalCostBasis                   |
| totalUnrealizedPnLPct | number | totalUnrealizedPnL / totalCostBasis (0 if 0)     |
| totalDayChange     | number | Σ Holding.dayChange                                 |
| totalDayChangePct  | number | totalDayChange / (totalMarketValue − totalDayChange)|

---

### Watchlist (user, persisted)

A named, ordered list of tickers the user wants to monitor without owning.

| Field     | Type     | Notes                                          |
|-----------|----------|------------------------------------------------|
| id        | string   | UUID (v4).                                     |
| name      | string   | 1–40 chars, trimmed, unique per user.          |
| tickers   | string[] | Ordered, unique within the list; each must exist in the Ticker catalog. |
| createdAt | string   | ISO timestamp.                                 |
| updatedAt | string   | ISO timestamp; bumped on any mutation.         |

**Validation**:
- `name` is required, trimmed, non-empty, <= 40 chars.
- `name` uniqueness enforced case-insensitively.
- `tickers` deduplicated on insert; preserves insertion order; reorder action
  allowed (FR-007).
- Unknown tickers rejected with inline error (FR-009).

**State transitions**:
- Created → Updated (rename / add / remove / reorder) → Deleted.

---

## Relationships

- `Watchlist 1..N → Ticker` (many-to-many via `tickers[]`).
- `Transaction N..1 → Ticker`.
- `Holding` is a pure projection of `Transaction[]` grouped by ticker and
  enriched with live `PriceBar`/`KeyStats` for that ticker.
- `Portfolio` is a pure projection of `Holding[]`.

```text
Ticker ──(1..N)── PriceBar
Ticker ──(1..1)── KeyStats

User Transactions ──group by ticker──► Holding ──aggregate──► Portfolio
User Watchlists ──contain──► Ticker refs
```

---

## Persistence boundary

| Entity       | Seed (bundled JSON) | Persisted (localStorage) | Derived (in-memory) |
|--------------|---------------------|--------------------------|---------------------|
| Ticker       | ✅                  | —                        | —                   |
| PriceBar     | ✅                  | —                        | —                   |
| KeyStats     | ✅                  | —                        | —                   |
| Transaction  | seed-portfolio.json (first-run only, cleared on import) | ✅ (portfolioStore) | — |
| Watchlist    | —                   | ✅ (watchlistStore)      | —                   |
| Holding      | —                   | —                        | ✅                  |
| Portfolio    | —                   | —                        | ✅                  |

First-run behavior: if `portfolioStore` is empty on first load, it is seeded
with `seed-portfolio.json` transactions so the dashboard has demo content.
Subsequent loads respect whatever the user has done since.
