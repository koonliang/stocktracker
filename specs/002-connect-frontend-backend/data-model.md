# Phase 1 Data Model: StockTracker Full-Stack Integration

Scope: persistent server-backed data for the StockTracker dashboard, watchlists,
analysis, and CSV import/export workflows. All canonical domain data is stored
in MySQL. Holdings and portfolio summary remain derived projections computed by
backend services.

This model intentionally assumes a single default portfolio context for this
iteration, so no user/auth tables are introduced.

---

## Persistent Entities

### Instrument

Reference record for a supported ticker.

| Field | Type | Notes |
|-------|------|-------|
| symbol | string | Primary key, uppercase ticker symbol |
| name | string | Display name |
| sector | string | Sector/category label |
| exchange | string | Exchange label such as NASDAQ or NYSE |
| active | boolean | Allows seed data to be retired without deleting history |
| created_at | timestamp | Audit field |
| updated_at | timestamp | Audit field |

**Validation**:
- `symbol` matches `^[A-Z]{1,5}$`
- `name` is required
- `symbol` is unique and immutable after insertion

---

### InstrumentPriceBar

Historical OHLCV data used by analysis charts and dashboard pricing.

| Field | Type | Notes |
|-------|------|-------|
| id | bigint | Surrogate primary key |
| instrument_symbol | string | FK -> Instrument.symbol |
| trade_date | date | One row per symbol/date |
| open_price | decimal | `> 0` |
| high_price | decimal | `>= open_price` and `>= close_price` |
| low_price | decimal | `<= open_price` and `<= close_price`, `> 0` |
| close_price | decimal | `> 0` |
| volume | bigint | `>= 0` |
| created_at | timestamp | Audit field |

**Validation**:
- Unique constraint on `(instrument_symbol, trade_date)`
- All price columns stored at a fixed decimal precision suitable for equity
  prices

---

### InstrumentStat

Latest per-ticker statistics rendered in the analysis view.

| Field | Type | Notes |
|-------|------|-------|
| instrument_symbol | string | PK and FK -> Instrument.symbol |
| open_price | decimal | Latest open |
| high_price | decimal | Latest high |
| low_price | decimal | Latest low |
| previous_close | decimal | Previous close |
| volume | bigint | Latest volume |
| week_52_high | decimal | Rolling 52-week high |
| week_52_low | decimal | Rolling 52-week low |
| market_cap | bigint | Latest market cap |
| pe_ratio | decimal nullable | Nullable for tickers without P/E |
| as_of_date | date | Snapshot date |
| updated_at | timestamp | Audit field |

---

### PortfolioTransaction

Canonical transaction record saved by the user and used to derive holdings.

| Field | Type | Notes |
|-------|------|-------|
| id | bigint | Primary key |
| trade_date | date | Transaction date |
| instrument_symbol | string | FK -> Instrument.symbol |
| transaction_type | enum | `BUY` or `SELL` |
| quantity | decimal | `> 0`, fractional shares allowed |
| price | decimal | `> 0` |
| fees | decimal | `>= 0`, defaults to `0` |
| source | enum | `MANUAL` or `CSV_IMPORT` |
| created_at | timestamp | Audit field |
| updated_at | timestamp | Audit field |

**Validation**:
- `quantity > 0`
- `price > 0`
- `fees >= 0`
- `trade_date` cannot be in the future
- `instrument_symbol` must refer to a known instrument

**State transitions**:
- `previewed` (request-scoped only, not persisted)
- `committed` (stored as a row)
- `deleted` (hard delete is acceptable for this iteration)

---

### Watchlist

Named collection of tracked tickers.

| Field | Type | Notes |
|-------|------|-------|
| id | bigint | Primary key |
| name | string | Unique case-insensitively within the single portfolio context |
| created_at | timestamp | Audit field |
| updated_at | timestamp | Audit field |

**Validation**:
- Trimmed non-empty name
- Maximum length 40
- Unique case-insensitive name

**State transitions**:
- `created`
- `updated`
- `deleted`

---

### WatchlistItem

Ordered member row within a watchlist.

| Field | Type | Notes |
|-------|------|-------|
| id | bigint | Primary key |
| watchlist_id | bigint | FK -> Watchlist.id |
| instrument_symbol | string | FK -> Instrument.symbol |
| display_order | integer | Zero-based order within watchlist |
| created_at | timestamp | Audit field |

**Validation**:
- Unique constraint on `(watchlist_id, instrument_symbol)`
- Unique constraint on `(watchlist_id, display_order)`
- `display_order >= 0`

---

## Derived Projections

### HoldingView

Computed from `PortfolioTransaction` grouped by ticker and enriched with latest
instrument pricing data.

| Field | Type | Derivation |
|-------|------|------------|
| symbol | string | Group key |
| name | string | From Instrument |
| shares | decimal | Sum of buys minus sells |
| average_cost | decimal | Weighted-average cost of remaining shares |
| cost_basis | decimal | `shares * average_cost` |
| current_price | decimal | Latest close price |
| market_value | decimal | `shares * current_price` |
| unrealized_pnl | decimal | `market_value - cost_basis` |
| unrealized_pnl_pct | decimal | Relative gain/loss |
| day_change | decimal | Position-level day change |
| day_change_pct | decimal | Instrument-level day change percent |
| weight | decimal | Share of total portfolio value |

**Rules**:
- Holdings with zero shares are excluded from dashboard holdings output
- Oversell protection is enforced during import/transaction validation before
  commit

### PortfolioSummaryView

Aggregate projection over all holdings.

| Field | Type | Derivation |
|-------|------|------------|
| total_market_value | decimal | Sum of holding market values |
| total_cost_basis | decimal | Sum of holding cost basis |
| total_unrealized_pnl | decimal | Sum of holding unrealized P&L |
| total_unrealized_pnl_pct | decimal | Relative portfolio gain/loss |
| total_day_change | decimal | Sum of holding day changes |
| total_day_change_pct | decimal | Relative portfolio day change |

### CsvImportPreview

Transient request/response model, not stored in MySQL.

| Field | Type | Notes |
|-------|------|-------|
| row_number | integer | Original row number in CSV |
| normalized_record | object nullable | Present only for valid rows |
| status | enum | `VALID` or `INVALID` |
| message | string | Validation result shown to the user |

---

## Relationships

```text
Instrument 1 ──< InstrumentPriceBar
Instrument 1 ──1 InstrumentStat
Instrument 1 ──< PortfolioTransaction
Instrument 1 ──< WatchlistItem >── 1 Watchlist

PortfolioTransaction ──derived──► HoldingView ──derived──► PortfolioSummaryView
```

- One `Instrument` has many price bars
- One `Instrument` has one current stat snapshot
- One `Watchlist` has many ordered `WatchlistItem` rows
- `HoldingView` and `PortfolioSummaryView` are service-layer projections, not
  persisted tables

---

## Persistence and Seed Rules

| Entity | Persistence | Seeded on bootstrap | Mutable at runtime |
|--------|-------------|---------------------|--------------------|
| Instrument | MySQL table | Yes | Limited |
| InstrumentPriceBar | MySQL table | Yes | No in this iteration |
| InstrumentStat | MySQL table | Yes | No in this iteration |
| PortfolioTransaction | MySQL table | Optional demo rows for first-run local env | Yes |
| Watchlist | MySQL table | No | Yes |
| WatchlistItem | MySQL table | No | Yes |
| HoldingView | Derived only | N/A | Derived |
| PortfolioSummaryView | Derived only | N/A | Derived |
| CsvImportPreview | Request-scoped only | N/A | Transient |

Seed behavior for local development:
- Schema is created via Flyway
- Instrument reference data is inserted if reference tables are empty
- Optional demo transactions may be inserted only for a clearly marked dev
  bootstrap profile
