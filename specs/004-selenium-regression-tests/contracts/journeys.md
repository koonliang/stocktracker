# Contract: Core User Journeys

Each journey is one JUnit 5 test. Routes are from `frontend/src/App.tsx`.
"Selectors" lists the stable hooks each Page Object needs; prefer accessible
locators (role/label/text), adding `data-testid` only where noted.

## J1 — Dashboard (route `/`)

**Goal**: Portfolio dashboard renders holdings and summary for seeded data.

| Step | Action | Assertion |
|------|--------|-----------|
| 1 | Navigate to `/` | Dashboard landing element visible |
| 2 | Wait for holdings to load | Holdings table present with ≥1 row |
| 3 | Read portfolio summary | Summary tiles show non-empty totals |

**Selectors needed**: `data-testid="holdings-table"`, `data-testid="summary-tiles"`
(map to existing HoldingsTable / SummaryTiles components).

**Maps to**: FR-005, Story 2 AS-1.

---

## J2 — Watchlist (routes `/watchlists`, `/watchlists/:id`)

**Goal**: An instrument can be added to and removed from a watchlist.

| Step | Action | Assertion |
|------|--------|-----------|
| 1 | Navigate to `/watchlists` | Watchlists list visible |
| 2 | Open/create a watchlist, add an instrument | Instrument row appears in the watchlist |
| 3 | Remove the instrument | Instrument row no longer present |

**Selectors needed**: add controls (`data-testid="watchlist-add"`),
instrument row (`data-testid="watchlist-item-<ticker>"`), remove control
(`data-testid="watchlist-remove"`). Exact ids confirmed during implementation
against the watchlist feature components.

**Maps to**: FR-005, Story 2 AS-2.

---

## J3 — Analysis (route `/analysis/:ticker`)

**Goal**: Price chart and key statistics display for a selected instrument.

| Step | Action | Assertion |
|------|--------|-----------|
| 1 | Navigate to `/analysis/<seeded-ticker>` | Analysis view visible |
| 2 | Wait for chart + stats | `data-testid="price-chart"` (already exists) visible |
| 3 | Read key stats | Key-stats grid shows non-empty values |

**Selectors needed**: existing `data-testid="price-chart"`; add
`data-testid="key-stats-grid"` (KeyStatsGrid component). Ticker chosen from
seeded reference data.

**Maps to**: FR-005, Story 2 AS-3.

---

## J4 — CSV import/export (route `/transactions`)

**Goal**: Transactions can be imported from CSV and exported to CSV.

| Step | Action | Assertion |
|------|--------|-----------|
| 1 | Navigate to `/transactions` | Transactions view visible |
| 2 | Import a known CSV file (via file input) | Imported transactions appear in the table |
| 3 | Trigger export | Export action succeeds (download/confirmation observable) |

**Selectors needed**: import control / file `<input type=file>`
(`data-testid="csv-import-input"`), export control (`data-testid="csv-export"`),
transactions table (`data-testid="transactions-table"`). A small fixture CSV
ships under `e2e/src/test/resources/`. Headless Chrome download handling
configured via ChromeOptions download prefs, or import-result assertion used as
the primary export-success proxy if download capture proves brittle.

**Maps to**: FR-005, Story 2 AS-4.

---

## Cross-cutting contract

- Every journey waits on explicit conditions (no `Thread.sleep`) — FR-007.
- Every journey runs headless against the compose stack — FR-001/FR-002/FR-006.
- A failure in any journey captures a screenshot + fails the suite — FR-004/FR-009.
