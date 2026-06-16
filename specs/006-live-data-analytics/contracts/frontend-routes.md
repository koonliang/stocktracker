# Contract: Frontend Routes & UI Behavior

Builds on the existing React Router 6 + Zustand + recharts setup. New routes are
wrapped in `ProtectedRoute` (feature 005). `data-testid` attributes are added for
the e2e Page-Object suite.

## Quote polling (cross-cutting — User Story 1)

- New `quotesStore` (Zustand) polls `GET /api/quotes?symbols=…` on a
  visibility-aware interval (~30s while the tab is focused; paused when hidden,
  resumes on focus). It tracks per-symbol `price`, `asOf`, `stale`.
- `portfolioStore` and the watchlist store merge live quotes into holdings /
  rows so `currentPrice`, market value, and day-change tick without a reload
  (FR-004).
- Dashboard tiles (`SummaryTiles`) and rows (`HoldingsTable`, watchlist rows)
  render a **"last updated <relative time>"** indicator and a **stale badge**
  when `stale` (FR-005). `data-testid`: `quote-last-updated`, `quote-stale`.
- Each holding/row shows the **base-currency** value (default) with the
  **native-currency** value and code alongside (FR-032). `data-testid`:
  `holding-native-value`, `holding-base-value`.

## Symbol search & add ticker (cross-cutting — User Story 1, FR-026/027)

- New `SymbolSearch.tsx`: a search box queries `GET /api/instruments/search?q=`
  (debounced) and shows matches (name, ticker, exchange, currency), including
  global symbols like `D05.SI`. Selecting a result calls `POST /api/instruments`,
  which creates the instrument and returns an immediate quote; the symbol's price
  appears at once (no wait for the next cycle) and is added to the polling set.
- Empty-query results and unrecognized-symbol (422) responses render clear
  empty/error states, not crashes. `data-testid`: `symbol-search`,
  `symbol-search-result`, `symbol-add`.

## Base currency (multi-currency — FR-031)

- `BaseCurrencySelect.tsx` (in settings / nav) reads `GET /api/me/base-currency`
  and persists via `PUT`. Changing it re-fetches dashboard/performance so all
  combined totals re-express in the new base currency. `data-testid`:
  `base-currency-select`.

## Transactions (User Story 2)

- The transaction entry form gains a `type` selector with the new options;
  fields show/hide by type (symbol + amount for dividend; symbol + ratio for
  split; amount + **currency** for deposit/withdrawal/fee).
- The import preview labels the detected CSV schema version (v1/v2) and renders
  the optional `amount`/`currency` columns. Existing import/preview/commit UX is
  otherwise unchanged.

## Performance route (User Story 3)

- New route `/performance` (`PerformanceRoute.tsx`), linked in nav.
- Controls: window selector (`1M/3M/6M/1Y/YTD/ALL`) and lot-method toggle
  (`FIFO`/`LIFO` — `specific` deferred). Calls `GET /api/performance`.
- Components: `ReturnChart` (recharts cumulative-return line), realized-P&L table
  of closed lots, unrealized summary, per-holding `ContributionTable`.
- `data-testid`: `performance-page`, `return-chart`, `realized-table`,
  `contribution-table`, `lot-method-toggle`, `perf-window-select`.

## Alerts route + notifications (User Story 4)

- New route `/alerts` (`AlertsRoute.tsx`): `AlertList` + `AlertForm`
  (react-hook-form + zod). Create/edit/delete via `/api/alerts`.
- `data-testid`: `alerts-page`, `alert-form`, `alert-symbol`, `alert-condition`,
  `alert-threshold`, `alert-submit`, `alert-row`, `alert-delete`.
- `NotificationToaster` (mounted in `App.tsx`) polls
  `GET /api/notifications?unread=true` and shows in-app toasts for new fires,
  marking them read via `POST /api/notifications/{id}/read`. `data-testid`:
  `notification-toast`.

## App wiring

`App.tsx` adds the two protected routes and mounts `NotificationToaster` once at
the app shell so toasts appear on any page.
