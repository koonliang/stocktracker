# UI Route Contract

The app exposes a small set of client-side routes. Each route is a "contract" in
the sense that its URL shape, required params, visible regions, and primary
interactions are fixed — downstream feature work and tests are written against
this contract.

All routes render inside a shared `<AppShell>` that provides primary navigation,
a page title region, and a content region.

| Route | Purpose | URL Params | Primary Regions | Empty State |
|-------|---------|------------|-----------------|-------------|
| `/` | Portfolio Dashboard (US1) | — | Summary tiles, Allocation chart, Holdings table | "No holdings yet — import transactions or view a watchlist" with CTAs |
| `/watchlists` | Watchlists index (US2) | — | List of watchlists with counts; "New watchlist" CTA | "No watchlists — create your first" |
| `/watchlists/:id` | Single watchlist detail (US2) | `id` = Watchlist.id | Header (name, rename, delete), ticker rows (price, day change, remove, reorder), "Add ticker" control | "This watchlist is empty — add a ticker" |
| `/transactions` | Import / Export (US4) | — | Import dropzone + preview table; Export button; current transactions table | "No transactions — import a CSV or add one manually" |
| `/analysis/:ticker` | Stock Analysis view (US3) | `ticker` = Ticker.symbol | Header (symbol, name, price, day change), time-range price chart, key stats grid, position summary (if held) | If ticker not in catalog: "Not found" with link back to Dashboard |

## Navigation contract

- Primary nav is always visible and highlights the current section.
- Primary nav items: Dashboard, Watchlists, Transactions.
- `/analysis/:ticker` is reachable from dashboard holdings rows, watchlist
  rows, and a global search affordance in the top bar; no primary nav entry.
- Browser back/forward MUST work for all navigations (React Router default).

## Interaction contract per route

### `/` Dashboard
- Sort holdings table by any numeric column (FR-003); selected sort column and
  direction are visually indicated and persist in component state only (not
  across reloads).
- Click a holding row → `/analysis/:ticker`.
- Allocation chart hover surfaces ticker + weight.

### `/watchlists` and `/watchlists/:id`
- Create: opens a dialog; name is validated (non-empty, <= 40 chars, unique
  case-insensitively). On submit, new list becomes the selected route.
- Rename / delete: inline controls on the detail page; delete requires a
  confirm dialog.
- Add ticker: input with inline validation (FR-009) — unknown tickers blocked
  with an error message; duplicates silently ignored.
- Remove / reorder: per-row remove button and drag-handle reorder.
- Click a ticker row → `/analysis/:ticker`.

### `/transactions`
- Import flow:
  1. Drop or select a CSV file.
  2. File is parsed client-side; a preview table renders all rows with
     per-row status (`valid` / `invalid: <reason>`).
  3. "Confirm import" button writes only the valid rows to the portfolio
     store; invalid rows are excluded.
  4. On success, user is navigated to `/` with a toast summarizing counts.
- Export flow: single "Export CSV" button triggers a browser download of all
  current transactions using the canonical schema (see
  `csv-transaction-schema.md`).
- Current transactions table: lists all committed transactions, sortable by
  date; per-row delete with confirm.

### `/analysis/:ticker`
- Time-range selector: `1D / 1W / 1M / 3M / 1Y / 5Y / ALL`; default `3M`; the
  selected range is visually highlighted (FR-011).
- Key-stats grid renders values from `KeyStats`; missing values render an
  em dash, never "NaN" or empty cells.
- Position summary: rendered only when the user owns the ticker (shares > 0).

## Accessibility contract (applies to every route)

- All interactive controls reachable via Tab and operable via Enter/Space.
- Focus ring visible on every focusable element.
- Every form field has a programmatically associated label.
- Every route passes `vitest-axe` assertions with zero critical violations in
  CI (SC-006).
- Color is never the sole indicator of meaning (e.g., P&L rows use both color
  and an arrow glyph).

## Responsiveness contract

- Renders without horizontal scroll at viewport widths 375, 768, 1280, 1920
  (SC-007).
- Below `md` (768px), primary nav collapses to a bottom tab bar; tables
  switch to card layout where appropriate.
