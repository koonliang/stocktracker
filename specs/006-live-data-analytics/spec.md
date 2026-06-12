# Feature Specification: Live Market Data & Portfolio Analytics

**Feature Branch**: `006-live-data-analytics`  
**Created**: 2026-06-12  
**Status**: Draft  
**Input**: User description: "1. Live Market Data Integration — real quote provider, scheduled refresh + cache, /quotes endpoint, frontend polling/SSE, rate-limit handling, last-updated indicator. 2. Dividends, Splits & Cash Transactions — new types dividend/split/deposit/withdrawal/fee, cost-basis & P&L handling splits retroactively, CSV schema with v1 backwards-compat. 3. Realized vs. Unrealized P&L Reporting — realized P&L per closed lot (FIFO default, optional LIFO/specific-lot), Performance page with cumulative return chart, time-weighted return, per-holding contribution. 4. Price Alerts & Notifications — thresholds (price >, price <, % change), evaluated on each quote refresh, in-app toast first, email/push later."

## Clarifications

### Session 2026-06-12

- Q: What quote refresh cadence should the system target? → A: **~1 minute** — refresh the latest quote per symbol roughly every 60 seconds during market hours; a quote older than that is "stale."
- Q: Where do the historical portfolio valuations for the cumulative-return chart and time-weighted return come from? → A: **Backfill from provider** — fetch historical daily closing prices from the quote source on demand so returns can be computed over any past window immediately.
- Q: For the "% change" alert type, the percentage is measured relative to what baseline? → A: **Previous close** — % change vs. the prior trading day's closing price (the conventional daily % change), which resets each trading day.
- Q: How does the user choose which lots a "specific-lot" sale consumes? → A: **Defer to planning** — FIFO (default) and LIFO are fully specified now; the specific-lot selection UX is left as a planning-phase detail.
- Q: With a live provider, what universe of tickers can a user add/track? → A: **Any provider symbol** — users may add any ticker the quote source recognizes; the instrument record is created on demand (no longer limited to the seeded set).
- Q: Should there be a ticker search / autocomplete? → A: **Search by name or ticker** — a search queries the provider's symbol lookup; the user types a company name or partial/exact ticker and picks from matching results (name, ticker, exchange).
- Q: When a user adds a brand-new ticker, when does its price first appear? → A: **Fetch immediately** — adding a symbol triggers an on-demand quote and recent-history backfill right away, and the symbol then joins the recurring refresh cycle.
- Q: Should the feature support non-US / global exchanges (e.g. SGX `.SI` tickers)? → A: **Global, any provider market** — users may add tickers from any exchange the chosen provider covers; market-hours and refresh logic must not assume US-only hours. (Provider note: Finnhub's free tier is US-only; global coverage requires a paid plan or a provider that covers the required exchanges.)
- Q: How should currency be handled given holdings across exchanges trade in different currencies (e.g. SGD vs USD)? → A: **Add multi-currency now** (reverses the earlier out-of-scope decision) — each instrument/transaction is tagged with a currency, an FX rate source supplies daily rates, and totals are reported in a user-chosen base currency.
- Q: Which market data provider should the pluggable quote source bind to? → A: **Yahoo Finance** (via yahooquery/yfinance) — free, no API key, global coverage incl. SGX `.SI`, and supplies symbol search, on-demand current quotes, and daily historical closes; accept the reliability risk of its unofficial/undocumented endpoints for this personal tracker.
- Q: How is the concrete provider selected across test/dev/prod? → A: **Deterministic stub by default; real provider opt-in via env** — a seeded stub (fixed quotes/history/FX, same interface, no network) is the default for all automated tests AND local dev so tests are reproducible and offline; dev may opt into the real provider via an env flag (e.g. `MARKET_DATA_PROVIDER=yahoo`); prod always uses the real provider.
- Q: Which FX rate source supplies the daily rates for multi-currency conversion? → A: **Frankfurter (ECB)** — `frankfurter.app`, free, no API key, daily granularity, ECB reference rates; a dedicated FX source separate from the quote provider.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See Live, Auto-Updating Prices (Priority: P1)

A signed-in user opens their portfolio dashboard and watchlist and sees current
market prices that update on their own without a manual refresh. Each price
shows when it was last updated, and the user trusts that figures reflect the
latest available market data rather than a static seed value.

**Why this priority**: Every downstream capability (alerts, realized/unrealized
reporting, contribution analysis) depends on real, current prices. Replacing
static seed snapshots with live quotes is the foundational slice and delivers
immediate standalone value: a tracker that actually tracks.

**Independent Test**: Open the dashboard and watchlist for an account holding
known symbols; confirm displayed prices match the current values from the quote
source within the refresh interval, that each tile/row shows a "last updated"
time, and that values change over time without the user reloading the page.

**Acceptance Scenarios**:

1. **Given** a user holds positions in tracked symbols, **When** they open the dashboard, **Then** each holding shows a current market price and a "last updated" timestamp.
2. **Given** the dashboard is open, **When** a new quote becomes available for a displayed symbol, **Then** the corresponding tile/row updates its price and timestamp without a full page reload.
3. **Given** the quote source is temporarily unavailable or rate-limited, **When** a refresh fails, **Then** the user continues to see the last known price marked as stale rather than an error or blank value.
4. **Given** a symbol the user is tracking, **When** the market is closed, **Then** the user sees the most recent available price (e.g., last close) with a timestamp indicating its age.

---

### User Story 2 - Record Dividends, Splits & Cash Movements (Priority: P2)

A user records portfolio events beyond simple buys and sells: dividends
received, stock splits, cash deposits and withdrawals, and fees. Their
cost basis, share counts, and profit/loss figures stay correct after these
events — including splits applied retroactively to positions acquired before
the split.

**Why this priority**: Buy/sell-only tracking produces wrong cost basis and P&L
for any real long-term portfolio. Accurate event handling is a prerequisite for
trustworthy realized/unrealized reporting (Story 3), but the app already
delivers value with live prices (Story 1) before this lands.

**Independent Test**: Add a position, then record a dividend, a 2-for-1 split,
and a deposit/withdrawal/fee; confirm share count and cost basis adjust per the
split, cash balance reflects the cash movements, and previously imported v1 CSV
files still import without modification.

**Acceptance Scenarios**:

1. **Given** an existing holding, **When** the user records a stock split (e.g., 2-for-1), **Then** the share quantity and per-share cost basis adjust so total cost basis is unchanged, and the adjustment applies to shares acquired before the split.
2. **Given** an existing holding, **When** the user records a dividend, **Then** it is captured as income against that holding without altering the cost basis of the shares held.
3. **Given** any portfolio, **When** the user records a deposit, withdrawal, or fee, **Then** the cash balance changes accordingly and the event appears in the transaction history.
4. **Given** a CSV exported under the previous (v1) buy/sell schema, **When** the user imports it, **Then** it parses successfully and is interpreted as buy/sell transactions with no data loss.
5. **Given** a CSV containing the new transaction types, **When** the user imports or exports it, **Then** the new types round-trip correctly and the schema version is identifiable.

---

### User Story 3 - Understand Realized vs. Unrealized Performance (Priority: P2)

A user opens a Performance page to see not just unrealized gains on open
positions, but realized profit/loss from positions they have already closed,
how their portfolio has grown over time, and which holdings contributed most to
returns.

**Why this priority**: Realized P&L and performance reporting are the primary
analytical payoff of the product, but they depend on correct prices (Story 1)
and correct event handling (Story 2) to be accurate. High value, later in the
dependency chain.

**Independent Test**: For an account with at least one fully closed position and
several open ones, confirm the Performance page reports realized P&L per closed
lot using FIFO, lets the user switch the lot-matching method, shows a cumulative
return chart over a selectable window, a time-weighted return figure, and a
per-holding contribution breakdown.

**Acceptance Scenarios**:

1. **Given** a position closed across one or more sell transactions, **When** the user views the Performance page, **Then** realized P&L is shown per closed lot, matched FIFO by default.
2. **Given** realized P&L is displayed, **When** the user selects a different lot-matching method (LIFO or specific-lot), **Then** realized figures recompute according to the chosen method.
3. **Given** a portfolio with transaction history, **When** the user selects a time window, **Then** a cumulative return chart and a time-weighted return value are shown for that window.
4. **Given** multiple holdings, **When** the user views the contribution breakdown, **Then** each holding's contribution to overall return is shown and the contributions reconcile with the total.

---

### User Story 4 - Get Alerted When a Price Crosses a Threshold (Priority: P3)

A user sets an alert on a ticker — price above a value, price below a value, or
a percentage change — and is notified in-app when the condition is met, without
having to watch the dashboard.

**Why this priority**: Alerts are a valuable engagement and retention feature
but are strictly additive: they depend on live quotes (Story 1) and add no
correctness risk to the core tracking. Lowest priority of the set, deliverable
last.

**Independent Test**: Create a "price above X" alert on a symbol; drive the quote
past the threshold; confirm an in-app notification appears once; confirm the
alert does not re-fire repeatedly for the same crossing and can be edited or
deleted.

**Acceptance Scenarios**:

1. **Given** a symbol, **When** the user sets a threshold (price >, price <, or % change), **Then** the alert is saved and visible in a list of the user's alerts.
2. **Given** an active alert, **When** a quote refresh shows the condition is met, **Then** the user receives an in-app notification.
3. **Given** an alert has fired, **When** subsequent refreshes still satisfy the condition, **Then** the alert does not spam repeated notifications for the same crossing.
4. **Given** an existing alert, **When** the user edits or deletes it, **Then** the change takes effect on the next evaluation and a deleted alert no longer fires.

---

### Edge Cases

- A symbol is unknown to the quote source or has been delisted — the user sees a clear "no current quote" state with the last known value rather than a crash.
- A ticker search returns no provider matches — the user sees an empty-results state, not an error.
- A user tries to add a symbol the provider does not recognize — the add is rejected with a clear message and no instrument record is created.
- The on-demand fetch for a just-added ticker fails (provider error/rate limit) — the symbol is still added and shows as stale until the next successful refresh, rather than blocking the add.
- A portfolio holds instruments in multiple currencies (e.g. SGD and USD) — combined totals are shown in the user's base currency, with each holding's native value also visible.
- An FX rate for a currency pair is temporarily unavailable — the system uses the last known rate (marked stale) rather than failing the whole portfolio view.
- A symbol is on a global exchange whose hours differ from US markets — its quote reflects that exchange's latest available value and is not treated as stale merely because US markets are closed.
- The quote source rate-limits or returns errors for a batch — the system serves cached values and retries on a later cycle without losing the "last updated" history.
- A split is recorded with a non-integer ratio (e.g., 3-for-2) or a reverse split (1-for-10) — quantities and cost basis adjust correctly, including fractional-share handling.
- A sell quantity exceeds the shares available under the selected lot-matching method — the system rejects or flags the inconsistency rather than producing negative holdings.
- A v1 CSV and a v2 CSV are imported in the same session — each is parsed under its own schema version without cross-contamination.
- An alert is created on a symbol the user later removes from their portfolio/watchlist — behavior is well-defined (alert persists or is cleaned up) and communicated.
- Two thresholds on the same symbol are both crossed in a single refresh — each fires at most once for that crossing.
- The Performance page is opened for an account with no closed positions — realized P&L shows a zero/empty state rather than an error.

## Requirements *(mandatory)*

### Functional Requirements

#### Live Market Data

- **FR-001**: System MUST retrieve current market quotes for tracked symbols from a real external quote source rather than static seed data.
- **FR-002**: System MUST refresh quotes on a recurring schedule of approximately every 60 seconds during market hours and cache the latest value per symbol so reads are served from cache, not a live call per request.
- **FR-003**: System MUST expose a way for the frontend to request current quotes for one or more symbols in a single call.
- **FR-004**: System MUST update dashboard tiles and watchlist rows with new prices without requiring a full page reload.
- **FR-005**: System MUST display a per-quote "last updated" indicator and visibly mark a quote as stale when its age exceeds the ~60-second refresh interval.
- **FR-006**: System MUST handle quote-source rate limits and transient failures gracefully by serving the last cached value and retrying on a later cycle, without surfacing errors that block the user.
- **FR-026**: Users MUST be able to search for instruments by company name or partial/exact ticker; results MUST show enough to disambiguate (name, ticker, exchange). Users MAY add any symbol the quote source recognizes, and the system MUST create the instrument record on demand rather than restricting to a fixed seeded set.
- **FR-027**: When a user adds a previously-untracked symbol, the system MUST fetch its current quote and backfill recent daily history immediately so price and market value appear without waiting for the next scheduled refresh, and the symbol MUST then be included in the recurring refresh cycle.
- **FR-028**: System MUST support instruments from non-US / global exchanges (e.g. SGX `.SI` tickers) in addition to US markets. Refresh scheduling and market-hours handling MUST NOT assume a single (US) trading calendar; each instrument's quote reflects the most recent available value for its own exchange.

#### Multi-Currency

- **FR-029**: System MUST associate a currency with each instrument (and therefore each transaction in that instrument); cash transactions (deposit/withdrawal/fee) MUST also carry a currency.
- **FR-030**: System MUST obtain foreign-exchange rates from an FX rate source (daily granularity is sufficient) to convert between transaction currencies and a user-chosen base reporting currency.
- **FR-031**: System MUST let the user choose a base reporting currency and MUST report combined portfolio totals, P&L, and performance figures in that base currency, converting native-currency values via the FX rates.
- **FR-032**: System MUST display each holding's native-currency price/value alongside the converted base-currency value so currency conversion is transparent.

#### Dividends, Splits & Cash Transactions

- **FR-007**: System MUST support transaction types beyond buy/sell: dividend, split, deposit, withdrawal, and fee.
- **FR-008**: System MUST adjust share quantity and per-share cost basis when a split is recorded, preserving total cost basis, and MUST apply the adjustment retroactively to shares acquired before the split.
- **FR-009**: System MUST record dividends as income associated with a holding without altering the cost basis of held shares.
- **FR-010**: System MUST track a cash balance affected by deposits, withdrawals, fees, dividends, buys, and sells.
- **FR-011**: System MUST import CSV files in the previous (v1) buy/sell schema without modification (backwards-compatible parsing).
- **FR-012**: System MUST support a CSV schema that represents the new transaction types and MUST make the schema version identifiable on import/export.

#### Realized vs. Unrealized P&L Reporting

- **FR-013**: System MUST compute and display unrealized P&L for open positions using current quotes.
- **FR-014**: System MUST compute and display realized P&L per closed lot, matching sells to buys using FIFO by default.
- **FR-015**: Users MUST be able to choose an alternative lot-matching method (LIFO or specific-lot) and see realized figures recompute accordingly. The mechanism by which a user selects individual lots for specific-lot matching is deferred to the planning phase.
- **FR-016**: System MUST provide a Performance view showing a cumulative return chart over a user-selectable time window.
- **FR-017**: System MUST display a time-weighted return for the selected window.
- **FR-018**: System MUST display a per-holding contribution breakdown that reconciles with the overall portfolio return.
- **FR-025**: System MUST obtain historical daily closing prices for held symbols from the quote source (backfilled on demand) so the cumulative-return chart and time-weighted return can be computed over any past window the user selects.

#### Price Alerts & Notifications

- **FR-019**: Users MUST be able to create alerts on a symbol with a threshold of: price above a value, price below a value, or a percentage change. For "% change" alerts, the percentage is measured relative to the symbol's previous trading-day close and the baseline resets each trading day.
- **FR-020**: System MUST evaluate active alerts on each quote refresh and notify the user in-app when a condition is met.
- **FR-021**: System MUST avoid repeated notifications for the same threshold crossing (fire once per crossing).
- **FR-022**: Users MUST be able to view, edit, and delete their alerts, with changes taking effect on the next evaluation.
- **FR-023**: System MUST persist alerts durably so they survive restarts and are evaluated across sessions.

#### Cross-cutting

- **FR-024**: All live-data, transaction, reporting, and alert data MUST remain scoped per authenticated user, consistent with the existing per-user isolation model.

### Key Entities *(include if feature involves data)*

- **Quote**: The latest known market data for a symbol — symbol, price, timestamp of last update, source, and staleness state. Cached and refreshed roughly every 60 seconds during market hours.
- **Historical Price**: A daily closing price for a symbol on a given date, backfilled from the quote source, used to build cumulative-return series and time-weighted return.
- **Instrument**: A tradable symbol with descriptive metadata (name, ticker, exchange, **currency**). No longer limited to the seeded set or to US markets — created on demand when a user adds any symbol the quote source recognizes (via ticker search), including global exchanges (e.g. SGX `.SI`).
- **FX Rate**: A daily exchange rate between two currencies from an FX rate source, used to convert native-currency values into the user's base reporting currency.
- **Transaction**: A portfolio event for a user — type (buy, sell, dividend, split, deposit, withdrawal, fee), symbol (where applicable), quantity/ratio/amount, date, and fees. Extends the existing buy/sell model.
- **Lot**: A parcel of shares from a buy, consumed by sells under the chosen matching method (FIFO/LIFO/specific-lot); the basis for realized P&L per closed lot.
- **Cash Balance**: Per-user running cash position derived from cash-affecting transactions.
- **Performance Summary**: Derived per-user reporting data — realized P&L, unrealized P&L, cumulative return series, time-weighted return, and per-holding contributions over a window.
- **Alert**: A user-owned rule on a symbol — condition type (price above/below, % change), threshold value, status (active/triggered), and last-fired marker to prevent duplicate notifications.
- **Notification**: An in-app message generated when an alert fires — references the alert and the triggering quote, with a read/unread state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: During market hours, displayed prices reflect the latest available market data within 60 seconds, and every quote shows a "last updated" time.
- **SC-002**: Dashboard and watchlist values update on their own within 60 seconds while a page is open, with no manual reload required.
- **SC-003**: When the quote source is unavailable, users still see last-known prices marked stale, with zero blank or error states on the dashboard.
- **SC-004**: 100% of valid v1 CSV files import successfully after the schema change (no regression in existing import).
- **SC-005**: After recording a split, total cost basis is unchanged and realized/unrealized P&L for the affected holding is correct for shares acquired before the split.
- **SC-006**: Realized P&L per closed lot matches an independently calculated FIFO result for a defined test portfolio, and switching to LIFO/specific-lot produces the correspondingly correct figures.
- **SC-007**: The per-holding contribution breakdown sums to the total portfolio return within a negligible rounding tolerance.
- **SC-008**: An alert whose condition is met produces exactly one in-app notification per crossing (no duplicates, no misses) in a controlled test.
- **SC-009**: A user can create a price alert and interpret the Performance page without external guidance, completing each task on first attempt.
- **SC-010**: A user can find a symbol by name or ticker via search and add it; for a valid symbol, a live price and market value appear within a few seconds of adding (not after a full refresh cycle).
- **SC-011**: Ticker search returns relevant matches for a known company name or partial ticker, and adding an unrecognized symbol is rejected with a clear message.
- **SC-012**: A user can add and track a global (non-US) instrument such as `D05.SI`, and its live price appears in that instrument's native currency.
- **SC-013**: For a portfolio mixing currencies (e.g. SGD + USD), the combined total and P&L in the chosen base currency equal the sum of each holding's native value converted at the applicable FX rate, within a negligible rounding tolerance.

## Assumptions

- This feature builds on the shipped Portfolio Dashboard, Watchlist, Stock Analysis, and CSV Import/Export (specs 001–002) and the per-user authentication model (spec 005); existing per-user data isolation is reused.
- The four capabilities ship as one feature delivered in priority order (P1 live data → P2 transactions & reporting → P3 alerts); each user story is independently demonstrable so partial delivery still adds value.
- The system treats the quote source as a pluggable interface (offering symbol search/lookup, on-demand current quotes, and daily historical data, with global coverage including US plus markets such as SGX). The concrete bound implementation is **Yahoo Finance** (via yahooquery/yfinance): free, no API key, global coverage incl. SGX `.SI`. Its endpoints are unofficial/undocumented (can change without notice); this reliability risk is accepted for a personal tracker. (Finnhub was rejected because its free tier is US-only.)
- Provider selection is environment-driven: a **deterministic stub** (a fake provider implementing the same interface, returning fixed seeded quotes, historical prices, and FX rates with no network calls) is the default for all automated tests AND local dev, keeping tests reproducible and offline. Local dev MAY opt into the real provider via an environment flag (e.g. `MARKET_DATA_PROVIDER=yahoo`); production always uses the real provider.
- The FX rate source is **Frankfurter** (`frankfurter.app`, ECB daily reference rates): free, no API key, daily granularity. It is a dedicated FX source separate from the quote provider and is also fronted by the deterministic stub in tests/dev.
- The quote refresh cadence is approximately 60 seconds during market hours (not sub-second tick data); the chosen provider must support this rate within its limits.
- Historical daily closing prices are backfilled from the quote source on demand to power the cumulative-return chart and time-weighted return; the chosen provider must offer historical daily data.
- During closed-market periods, "current price" means the most recent available value (e.g., last close) for that instrument's own exchange, clearly timestamped, rather than a live tick; staleness is judged per exchange, not against a single US calendar.
- Notification delivery in this feature is in-app only; email and push delivery are explicitly deferred to a later iteration.
- Quote, alert, and transaction data persist in the existing relational store (MySQL) consistent with the current architecture.
- Cost-basis and P&L calculations follow standard conventions (FIFO default for lot matching; splits preserve total cost basis); no jurisdiction-specific tax treatment (e.g., wash sales) is in scope here.
- Multi-currency handling IS in scope for this feature (see FR-029–FR-032): instruments and transactions carry a currency, daily FX rates convert to a user-chosen base reporting currency, and combined totals are reported in that base currency. Intraday/real-time FX precision is not required — daily rates suffice.
