# Phase 0 Research: Live Market Data & Portfolio Analytics

All Technical Context unknowns are resolved below. Each decision lists what was
chosen, why, and the alternatives rejected. Decisions 11–13 reflect the global-
market, multi-currency, and symbol-search clarifications. The concrete providers
(Yahoo Finance, Frankfurter) and the stub-vs-real wiring were pinned in the
`/speckit.clarify` session (spec Clarifications, 2026-06-12).

## 1. Quote provider abstraction & concrete impl

- **Decision**: `MarketDataProvider` SPI selected by
  `stocktracker.marketdata.provider` (env `STOCKTRACKER_MARKETDATA_PROVIDER`):
  `stub` (default for dev + all tests; deterministic, seeded — no network) and
  `yahoo` (the real prod impl) calling Yahoo Finance's public JSON endpoints via a
  Quarkus REST client / backend proxy: `/v7/finance/quote` (batch quotes),
  `/v8/finance/chart/{symbol}?interval=1d` (daily history), `/v1/finance/search`
  (symbol search). No API key.
- **Rationale**: Mirrors feature 005's mode-agnostic auth; isolates the external
  quote dependency behind one seam so tests stay hermetic (Constitution I). Yahoo
  is free, key-less, and covers global exchanges incl. SGX `.SI` (FR-028) with
  batch quotes that satisfy the cache-not-per-request rule (FR-002).
- **Java note**: the clarify answer referenced `yahooquery`/`yfinance` — those are
  Python wrappers; from Java we call the same underlying Yahoo HTTP endpoints
  directly, so no Python dependency is introduced.
- **Risk**: Yahoo's endpoints are unofficial and may rate-limit or change. A sane
  `User-Agent`, batch calls, retry-next-cycle, and serving last cached value
  (FR-006) contain it; the stub isolates every test from this volatility. Accepted
  for a personal tracker.
- **Alternatives rejected**: per-request live fetch (rate-limit/latency); Finnhub
  (free tier US-only — fails FR-028 without a paid plan); Alpha Vantage / Twelve
  Data (free-tier rate limits too tight for a ~60s refresh).

## 2. Refresh mechanism & rate-limit bounding

- **Decision**: One `@Scheduled(every="60s")` `QuoteRefreshJob` resolves the union
  of *tracked* symbols (in any `portfolio_transaction` or `watchlist_item`),
  batch-fetches their quotes, upserts `instrument_quote`, then runs alert
  evaluation against the fresh quotes.
- **Rationale**: Bounds provider calls to real usage; one job serves both refresh
  and alert eval (FR-020) so alerts always see the same fresh quote.
- **Alternatives rejected**: refresh all instruments (wasteful); a second alert
  scheduler (redundant, risks stale evaluation).

## 3. Frontend transport: polling vs SSE

- **Decision**: Client polling. `quotesStore` (Zustand) polls
  `GET /api/quotes?symbols=…` on a visibility-aware interval (~30s focused, paused
  when hidden). Notifications poll the same way.
- **Rationale**: Simplest mechanism meeting FR-004 at a 60s cadence; no push infra,
  Lambda-friendly; no new dependency.
- **Alternatives rejected**: SSE/WebSocket (infra + Lambda-unfriendly for marginal
  gain); TanStack Query (new dep for what a small store already does).

## 4. Quote cache shape, staleness & dashboard integration

- **Decision**: `instrument_quote` holds the latest quote per symbol with **two
  timestamps**: `as_of` (the market timestamp from the provider) and `fetched_at`
  (when our job last successfully obtained a value). **Staleness = `fetched_at`
  older than a few refresh intervals** (i.e. our fetch is failing) — *not* whether
  the market is open. `PortfolioService` reads current price from `instrument_quote`,
  falling back to the latest `instrument_price_bar` close (marked stale) when no
  live quote exists.
- **Rationale**: Separating the two timestamps satisfies the global-market edge
  case (a closed-exchange quote with an old `as_of` is **not** stale because
  `fetched_at` is recent), while still flagging genuine provider outages (FR-006).
- **Alternatives rejected**: single timestamp + market-hours gate (false-stale for
  closed global exchanges, the exact edge case we must avoid); in-memory cache
  (lost on restart, harder to test).

## 5. Extended transaction model & split semantics

- **Decision**: Keep one `portfolio_transaction` table. `V4` makes
  `instrument_symbol` nullable, adds `amount DECIMAL(19,4)` (nullable) and
  `currency CHAR(3)`, and widens `transaction_type` to `buy, sell, dividend,
  split, deposit, withdrawal, fee`. `split` stores its ratio in `quantity`
  (new-per-old; `2`=2:1, `0.1`=1:10). Cash types use `amount` + `currency`, null
  symbol.
- **Rationale**: Minimal additive schema; splits are first-class date-ordered
  events replayed by cost-basis logic.
- **Alternatives rejected**: a table per type (over-modeling); `$CASH`
  pseudo-symbols (hacky); numerator/denominator pair (a single ratio suffices).

## 6. CSV v2 schema & v1 backwards compatibility

- **Decision**: v2 export header `date,ticker,type,quantity,price,fees,amount,currency`.
  Import detects v1 (no `amount`/`currency` columns and only buy/sell types →
  parsed exactly as today, `currency` defaults to the instrument's currency) vs v2
  (extra columns and/or new types present). v2 is a strict superset.
- **Rationale**: Every valid v1 file remains valid (SC-004) with no separate
  parser; version identifiable from the column set (FR-012).
- **Alternatives rejected**: mandatory version column/comment (breaks v1 files); a
  separate v2 upload path (duplicate logic).

## 7. Lot matching & realized P&L

- **Decision**: `LotMatchingService` replays a symbol's transactions in trade-date
  order: `buy` opens a lot; `split` rescales open lots (total cost preserved);
  `sell` consumes lots by **FIFO (default)** or **LIFO**, emitting realized P&L per
  closed lot. Computed in the instrument's **native currency**; conversion to base
  happens in reporting (decision 12). `specific-lot` is engine-supported via an
  explicit lot→qty map but its UI is deferred (per clarification); v1 ships
  FIFO/LIFO.
- **Rationale**: One date-ordered replay handles retroactive splits (SC-005) and
  yields realized + remaining open lots in a single pass.
- **Alternatives rejected**: average-cost only (can't do LIFO/specific-lot);
  persisting lots (premature; replay is fast and always consistent).

## 8. Cumulative return, time-weighted return & contributions

- **Decision**: `PerformanceService` builds a daily portfolio-value series from
  split-adjusted holdings × daily closes (`instrument_price_bar`), treating
  deposits/withdrawals and buy/sell cash legs as flows, **converting each day's
  values to the base currency** via that day's FX rate. **TWR** chains daily
  sub-period returns; per-holding **contribution** sums to the total (SC-007).
  Missing history → on-demand backfill (FR-025).
- **Rationale**: TWR is the standard timing-neutral measure; reusing
  `instrument_price_bar` avoids a new time-series store; daily FX keeps currency
  conversion tractable.
- **Alternatives rejected**: money-weighted/IRR only (timing-sensitive);
  precomputed valuation snapshots (clarified decision is on-demand backfill).

## 9. Alert evaluation & fire-once semantics

- **Decision**: `alert` carries `condition_type` (`price_above`, `price_below`,
  `pct_change`), `threshold`, `armed`, `last_triggered_at`. On each refresh, fire
  only when `armed` and the condition holds; then `armed=false` + write
  `notification`; re-arm when the condition clears. `pct_change` is vs the symbol's
  **previous close** (resets daily). Thresholds compare against the quote's native
  price.
- **Rationale**: Armed flag gives exactly-once-per-crossing (FR-021, SC-008) with
  trivial state; in-job evaluation guarantees fresh quotes.
- **Alternatives rejected**: cooldown timers (double-fire on hovering price);
  per-alert price history (unnecessary).

## 10. Market hours handling

- **Decision**: The refresh job runs on a continuous 60s cadence regardless of any
  single market's hours; per-symbol freshness is governed by `fetched_at`
  (decision 4). An optional `ExchangeCalendar` may skip fetching symbols on a
  known-closed exchange purely as a rate-limit optimization — it never drives
  staleness.
- **Rationale**: Directly satisfies FR-028 (no single US calendar). Continuous
  cadence is simplest and correct across mixed exchanges; the calendar is an
  optimization, not a correctness dependency.
- **Alternatives rejected**: US-hours gating (breaks global symbols — the edge case
  we must avoid); a full holiday-calendar library (not worth the dependency).

## 11. Global / non-US exchange support

- **Decision**: Instruments are no longer limited to the seeded US set. Any symbol
  the provider recognizes (including SGX `.SI`) can be added. The `stub` provider's
  seed includes at least one non-US example (e.g. an `.SI` symbol) so global
  behavior is covered in tests without network. Production simply requires a
  provider plan that covers the needed exchanges.
- **Rationale**: Satisfies FR-028/SC-012 while keeping global coverage a
  deployment/config concern behind the SPI.
- **Alternatives rejected**: US-only scope (contradicts the clarified requirement);
  baking exchange-specific logic into services (the SPI already abstracts it).

## 12. Multi-currency & FX

- **Decision**: Add a separate **`FxRateProvider` SPI**
  (`stocktracker.fx.provider`: `stub` for tests/dev; `frankfurter` in prod —
  `api.frankfurter.app`, ECB daily reference rates, no API key), a daily
  `FxRefreshJob`, and an `fx_rate` cache table. Each `instrument` has a `currency`;
  each user has a `base_currency` (default `USD`). `CurrencyService` converts
  native→base using the applicable daily rate, using the **last known rate (marked
  stale)** if a pair is temporarily unavailable. All combined totals/P&L/performance
  report in base currency; per-holding native values remain visible (FR-029–032).
- **Rationale**: Decoupling FX from the quote provider lets either change
  independently and keeps each provider's stub simple. Daily granularity is
  sufficient (no intraday FX) — minimal per Constitution IV. Last-known-rate
  fallback mirrors the quote degradation rule (FR-006) and the FX-unavailable edge
  case.
- **Alternatives rejected**: folding FX into the quote provider (couples unrelated
  concerns); intraday FX (unneeded precision/cost); converting in the frontend
  (scatters rate logic, risks inconsistent totals).

## 13. Symbol search & add-on-demand

- **Decision**: `MarketDataProvider` gains `searchSymbols(query)`. `GET
  /api/instruments/search?q=` returns matches (name, ticker, exchange, currency).
  Adding a symbol (`POST /api/instruments`) creates the `instrument` row on demand
  (capturing currency/exchange), then **immediately** calls the quote +
  historical-backfill path so price/value appear at once (FR-027/SC-010); the
  symbol thereafter joins the refresh cycle. If the immediate fetch fails, the
  instrument is still added and shows stale (edge case). Unrecognized symbols are
  rejected with no row created.
- **Rationale**: Search + on-demand creation is the minimal way to support an open
  ticker universe (FR-026); reusing the existing quote/backfill services avoids new
  machinery.
- **Alternatives rejected**: pre-importing a full global symbol list (huge,
  stale-prone); blocking the add until backfill completes (slower UX, fragile on
  provider hiccups).
