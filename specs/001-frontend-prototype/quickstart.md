# Quickstart: StockTracker Frontend Prototype

Get the prototype running locally and verify all three constitutional quality
gates (test, lint, type-check/build) pass before considering a feature complete.

## Prerequisites

- Node.js >= 20.10 (LTS)
- npm >= 10 (bundled with Node 20)

## First-time setup

```bash
cd frontend
npm ci
```

## Daily development

```bash
# start the Vite dev server (default http://localhost:5173)
npm run dev
```

On first load, the app seeds a demo portfolio so the Dashboard is immediately
populated. To reset to seed state, clear site data in your browser or run
`localStorage.clear()` in the devtools console.

## Quality gates (must all pass before marking a feature complete)

Per `.specify/memory/constitution.md` principles I–III:

```bash
# I. Test gate
npm test              # vitest run (unit + component + a11y assertions)

# II. Lint gate
npm run lint          # eslint . && prettier --check .

# III. Compile gate
npm run typecheck     # tsc --noEmit
npm run build         # vite build
```

Convenience:

```bash
npm run verify        # runs lint, typecheck, test, and build in sequence
```

A feature is NOT considered complete until `npm run verify` exits 0.

## Useful scripts

| Script                  | Purpose                                         |
|-------------------------|-------------------------------------------------|
| `npm run dev`           | Vite dev server with HMR                        |
| `npm run build`         | Production build → `frontend/dist/`             |
| `npm run preview`       | Serve the production build locally              |
| `npm test`              | Run Vitest once (CI mode)                       |
| `npm run test:watch`    | Run Vitest in watch mode                        |
| `npm run lint`          | ESLint + Prettier check                         |
| `npm run lint:fix`      | ESLint --fix and Prettier --write               |
| `npm run typecheck`     | `tsc --noEmit`                                  |
| `npm run generate-seed` | Regenerate `src/data/*.json` from the generator |
| `npm run verify`        | lint + typecheck + test + build                 |

## Verifying each User Story end-to-end

### US1 — Portfolio Dashboard
1. Load the app; the Dashboard renders with seeded holdings.
2. Verify totals reconcile against the holdings table (spot-check one row).
3. Click a numeric column header; rows reorder and the sort indicator updates.
4. Resize to 375px wide; no horizontal scroll.

### US2 — Watchlist
1. Navigate to `Watchlists` → `New watchlist` → name it "Tech Majors".
2. Add three tickers (e.g., AAPL, MSFT, NVDA). Each appears with price and
   day change.
3. Try adding `ZZZZ` — inline error; not added.
4. Reload the page; the watchlist persists.

### US3 — Stock Analysis
1. From any entry point, open an `/analysis/:ticker` view.
2. Switch across all seven time ranges; the chart updates and the selection
   is highlighted.
3. For a ticker you hold, the position summary renders and matches the
   dashboard figure.

### US4 — Import / Export Transactions
1. On `/transactions`, import the sample CSV at
   `frontend/src/test/fixtures/sample-transactions.csv` (includes a mix of
   valid and invalid rows).
2. Verify the preview flags invalid rows and disables them from commit.
3. Click `Confirm import`; Dashboard reflects the new holdings.
4. Click `Export CSV`; re-import the downloaded file into a fresh browser
   profile and verify the Dashboard totals match (round-trip, SC-008).

## Troubleshooting

- Dashboard is empty on first run: the seed may have failed — check the
  browser console for a seed-loader warning.
- Types out of sync after editing `src/data/*.json`: re-run `npm run
  generate-seed` (the generator emits a matching `*.d.ts`).
- Focus ring missing: you have probably overridden `outline` in a component
  without providing a replacement focus style — fix at the component level,
  do NOT disable the global rule.

## What this prototype deliberately does NOT do (see spec Assumptions)

- No backend, no live market data, no authentication, no multi-user.
- No dividends/splits/transfers, no multi-currency, no short positions.
- No broker-specific CSV formats.
- No e2e test suite — component tests + manual walkthrough are the
  verification surface for this iteration.
