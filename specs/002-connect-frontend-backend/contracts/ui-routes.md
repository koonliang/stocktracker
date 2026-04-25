# UI Route Contract

The frontend route surface remains intentionally close to the prototype so the
integration work replaces persistence behavior without redefining navigation.

All routes render inside the shared app shell and load their product data from
the backend API.

| Route | Purpose | Primary backend dependency | Primary regions |
|-------|---------|----------------------------|-----------------|
| `/` | Portfolio dashboard | `GET /api/dashboard` | Summary tiles, allocation chart, holdings table |
| `/watchlists` | Watchlists index | `GET /api/watchlists` | Watchlist list, create CTA |
| `/watchlists/:id` | Single watchlist detail | `GET /api/watchlists` plus watchlist mutation endpoints | Header, ticker rows, add/remove/reorder controls |
| `/transactions` | Import/export and transaction history | Transaction preview/commit/export/list endpoints | Import dropzone, preview table, export button, transactions table |
| `/analysis/:ticker` | Stock analysis view | `GET /api/instruments/{ticker}` | Header, time-range chart, key stats grid, position summary |

## Navigation contract

- Primary navigation remains Dashboard, Watchlists, and Transactions.
- Analysis pages are reached from holdings rows, watchlist rows, or ticker
  search rather than from a primary nav tab.
- Browser back/forward navigation must keep working across all routes.

## Route behavior contract

### `/`

- Shows loading placeholders while dashboard data is in flight.
- Shows a recoverable error state if dashboard loading fails.
- Empty state is shown when no holdings exist.
- Clicking a holding row opens `/analysis/:ticker`.

### `/watchlists`

- Allows creation of a new named watchlist.
- Watchlist names are validated before submission.
- A successful create transitions the user to `/watchlists/:id`.

### `/watchlists/:id`

- Supports rename, delete, add ticker, remove ticker, and reorder.
- Invalid tickers are rejected with an inline error.
- Mutation failures must keep the current watchlist visible and explain the
  failure without losing current page context.
- Clicking a ticker row opens `/analysis/:ticker`.

### `/transactions`

- Import flow remains two-step: preview first, commit second.
- Invalid CSV rows must be visible in preview and excluded from commit.
- Export returns the current committed transaction history in canonical CSV
  format.
- The current transactions table reflects the server's committed data, not
  local browser state.

### `/analysis/:ticker`

- Supports the same visible time-range options as the prototype.
- Missing or failed analysis data shows a non-destructive error state with a
  way back to a stable route.
- Position summary appears only when the current portfolio contains that ticker.

## Cross-cutting UI states

- Every route must define loading, empty, success, and error states where
  applicable.
- Route transitions must remain usable at 375px, 768px, 1280px, and 1920px.
- Keyboard accessibility and focus visibility remain part of the route
  contract.
