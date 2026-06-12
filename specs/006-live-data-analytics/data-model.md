# Phase 1 Data Model: Live Market Data & Portfolio Analytics

Maps spec Key Entities to concrete tables. New tables and column changes are
delivered by Flyway migration `V4__live_data_transactions_alerts_currency.sql`.
All user-owned rows carry `user_id` (FR-024), consistent with feature 005.
Existing tables not listed below are unchanged.

## Modified: `instrument`

Today: `symbol (PK), name, sector, exchange, active, created_at, updated_at`.

| Change | Detail |
|--------|--------|
| `currency` | **NEW** `CHAR(3) NOT NULL DEFAULT 'USD'` — native trading currency (FR-029) |
| on-demand rows | Rows may now be created at runtime when a user adds a searched symbol (incl. non-US, e.g. SGX `.SI`); no longer limited to the seed (FR-026/028) |

Backs spec entity **Instrument**. `exchange` + `currency` are populated from the
provider's symbol-search result when created on demand.

## Modified: `app_user`

| Change | Detail |
|--------|--------|
| `base_currency` | **NEW** `CHAR(3) NOT NULL DEFAULT 'USD'` — user's chosen reporting currency (FR-031) |

## Modified: `portfolio_transaction`

Today: `id, user_id, trade_date, instrument_symbol, transaction_type, quantity,
price, fees, source, created_at, updated_at`, `transaction_type ∈ {buy, sell}`.

| Change | Detail |
|--------|--------|
| `instrument_symbol` | becomes **NULLABLE** (cash events have no symbol) |
| `amount` | **NEW** `DECIMAL(19,4) NULL` — cash value for dividend/deposit/withdrawal/fee |
| `currency` | **NEW** `CHAR(3) NULL` — defaults to the instrument's currency for security txns; required for cash txns |
| `transaction_type` | widened: `buy, sell, dividend, split, deposit, withdrawal, fee` |

Field usage by type:

| Type | symbol | quantity | price | fees | amount | currency |
|------|--------|----------|-------|------|--------|----------|
| `buy` / `sell` | required | shares | per-share | optional | — | = instrument ccy |
| `dividend` | required | — | — | optional | cash (req) | = instrument ccy |
| `split` | required | ratio (new per old) | — | — | — | n/a |
| `deposit`/`withdrawal`/`fee` | null | — | — | — | cash (req) | required |

**Validation rules** (`TransactionValidationService`):
- `quantity > 0` for buy/sell/split; `amount > 0` for dividend/deposit/withdrawal/fee.
- `instrument_symbol` required for buy/sell/dividend/split; null/blank for cash types.
- `currency` required for cash types; for security types it defaults to the
  instrument's currency and must match it if provided.
- `transaction_type` one of the seven allowed values (lowercased).
- A `sell` may not drive a symbol's split-adjusted balance below zero.

## New: `instrument_quote` (latest live quote cache)

Backs spec entity **Quote**. One row per symbol; upserted by `QuoteRefreshJob`.

| Column | Type | Notes |
|--------|------|-------|
| `instrument_symbol` | VARCHAR PK | FK → `instrument.symbol` |
| `price` | DECIMAL(19,4) | latest quote price (native currency) |
| `change_amount` | DECIMAL(19,4) | price − previous_close |
| `change_pct` | DECIMAL(9,4) | day % vs previous_close (alert `pct_change` baseline) |
| `previous_close` | DECIMAL(19,4) | prior trading-day close |
| `as_of` | TIMESTAMP | provider's market timestamp for the value |
| `fetched_at` | TIMESTAMP | when our job last successfully obtained a value |
| `source` | VARCHAR | provider id (`stub` / real) |
| `stale` | BOOLEAN | derived: `fetched_at` older than N refresh intervals (provider failing) — **not** market-closed |
| `updated_at` | TIMESTAMP | last upsert |

Not user-scoped (market data is shared). Staleness is judged via `fetched_at`, so
a closed-exchange quote with an old `as_of` is **not** stale (global-market edge case).

## New: `fx_rate` (daily exchange rates)

Backs spec entity **FX Rate**. Cached by `FxRefreshJob`; read by `CurrencyService`.

| Column | Type | Notes |
|--------|------|-------|
| `base_currency` | CHAR(3) | e.g. `USD` |
| `quote_currency` | CHAR(3) | e.g. `SGD` |
| `rate_date` | DATE | daily granularity |
| `rate` | DECIMAL(19,8) | units of quote per 1 base |
| `source` | VARCHAR | provider id |
| `stale` | BOOLEAN | true when served as last-known on a fetch miss |
| PK | (`base_currency`,`quote_currency`,`rate_date`) | |

`CurrencyService.convert(amount, from, to, onDate)` uses the rate for `on_date`
(or the most recent prior rate); on a missing pair it uses the last known rate
marked stale rather than failing the view (FX-unavailable edge case).

## Reused: `instrument_price_bar` (historical daily closes)

Backs spec entity **Historical Price**. Existing OHLCV daily table.
`HistoricalBackfillService` upserts daily-close rows fetched from the provider on
demand (incl. for just-added symbols). No schema change.

## Derived (no table): Lot, Cash Balance, Performance Summary

Computed on read (research §7–8, §12):

- **Lot** — `LotMatchingService` replay in trade-date order (FIFO/LIFO);
  realized P&L per closed lot + remaining open lots, in native currency.
- **Cash Balance** — running per-currency sum: `+deposit +dividend +sell_proceeds
  −withdrawal −buy_cost −fee`, then converted to base for display.
- **Performance Summary** — realized + unrealized P&L, daily cumulative-return
  series, TWR, per-holding contribution, **all converted to the user's base
  currency** via `fx_rate`, for a selected window and lot-matching method.

## New: `alert`

Backs spec entity **Alert**. User-owned, evaluated each refresh.

| Column | Type | Notes |
|--------|------|-------|
| `id` | PK | |
| `user_id` | FK → `app_user.id` | per-user isolation |
| `instrument_symbol` | VARCHAR | FK → `instrument.symbol` |
| `condition_type` | VARCHAR | `price_above` \| `price_below` \| `pct_change` |
| `threshold` | DECIMAL(19,4) | price (native ccy) or percent for `pct_change` |
| `armed` | BOOLEAN | true = eligible to fire; false after firing until condition clears |
| `last_triggered_at` | TIMESTAMP NULL | last fire time |
| `created_at` / `updated_at` | TIMESTAMP | |

State transitions (FR-021/SC-008): `armed=true` + condition met → write
`notification`, set `armed=false`, `last_triggered_at=now` → condition clears on a
later refresh → `armed=true`. Editing an alert re-arms it.

## New: `notification`

Backs spec entity **Notification**.

| Column | Type | Notes |
|--------|------|-------|
| `id` | PK | |
| `user_id` | FK → `app_user.id` | per-user isolation |
| `alert_id` | FK → `alert.id` NULL | source alert (nullable if alert later deleted) |
| `message` | VARCHAR | e.g. "D05.SI crossed above 45.00 SGD" |
| `read` | BOOLEAN | unread by default |
| `created_at` | TIMESTAMP | |

## Migration ordering

`V4` is additive and safe on existing data: new columns default sensibly
(`currency`/`base_currency` → `USD`), widening `transaction_type` and adding
nullable columns preserves all current rows, and existing buy/sell transactions
and v1 CSV exports stay valid (SC-004). Latest version moves V3 → **V4**.
