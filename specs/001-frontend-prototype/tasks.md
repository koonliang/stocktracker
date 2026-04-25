---

description: "Task list for StockTracker Frontend Prototype (001-frontend-prototype)"
---

# Tasks: StockTracker Frontend Prototype

**Input**: Design documents from `/specs/001-frontend-prototype/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The project constitution (Principle I, NON-NEGOTIABLE) requires
every feature to ship with passing automated tests, so test tasks are present for
each user story.

**Organization**: Tasks are grouped by user story (US1 Dashboard, US2 Watchlist,
US3 Stock Analysis, US4 Import/Export) to enable independent implementation and
testing. Each user story phase is a complete, independently demoable increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Maps a task to its user story for traceability (US1–US4)
- Every task includes an exact file path

## Path Conventions

Web application layout with a single `frontend/` package at the repo root, per
plan.md. `backend/` is intentionally not created this iteration.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the frontend package, install dependencies, and wire the
three quality gates (test, lint, compile) from the first commit.

- [X] T001 Create the `frontend/` directory and initialize an npm package at `frontend/package.json` with name `stocktracker-frontend`, version `0.1.0`, and scripts `dev`, `build`, `preview`, `test`, `test:watch`, `lint`, `lint:fix`, `typecheck`, `generate-seed`, `verify` (runs lint + typecheck + test + build) per `quickstart.md`.
- [X] T002 Install runtime dependencies in `frontend/`: `react`, `react-dom`, `react-router-dom`, `zustand`, `recharts`, `papaparse`, `react-hook-form`, `zod`, `@hookform/resolvers`, `lucide-react`.
- [X] T003 Install dev dependencies in `frontend/`: `typescript`, `vite`, `@vitejs/plugin-react`, `tailwindcss`, `postcss`, `autoprefixer`, `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `jsdom`, `vitest-axe`, `axe-core`, `eslint`, `@typescript-eslint/parser`, `@typescript-eslint/eslint-plugin`, `eslint-plugin-react`, `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`, `prettier`, `eslint-config-prettier`, `@types/react`, `@types/react-dom`, `@types/papaparse`, `@types/node`.
- [X] T004 [P] Create `frontend/tsconfig.json` with `strict: true`, `noUncheckedIndexedAccess: true`, `target: ES2022`, `module: ESNext`, `moduleResolution: bundler`, `jsx: react-jsx`, `types: [vite/client, vitest/globals, @testing-library/jest-dom]`, and a `paths` entry mapping `@/*` → `src/*`.
- [X] T005 [P] Create `frontend/vite.config.ts` configuring the React plugin, path alias `@` → `src`, Vitest environment `jsdom`, globals true, setup file `src/test/setup.ts`, and a `css.postcss` hook.
- [X] T006 [P] Create `frontend/tailwind.config.ts` with `content: ["./index.html","./src/**/*.{ts,tsx}"]`, theme extensions that consume CSS variables from `styles/tokens.css` (colors, spacing, font sizes, radii, shadows), and the `darkMode: ["class"]` strategy.
- [X] T007 [P] Create `frontend/postcss.config.js` with `tailwindcss` and `autoprefixer` plugins.
- [X] T008 [P] Create `frontend/.eslintrc.cjs` extending `eslint:recommended`, `plugin:@typescript-eslint/recommended`, `plugin:react/recommended`, `plugin:react-hooks/recommended`, `plugin:jsx-a11y/recommended`, and `prettier`; set `settings.react.version = "detect"`.
- [X] T009 [P] Create `frontend/.prettierrc` with `semi: true`, `singleQuote: true`, `trailingComma: "all"`, `printWidth: 100`, and create `frontend/.prettierignore` excluding `dist/`, `node_modules/`, `src/data/*.json`.
- [X] T010 [P] Create `frontend/index.html` with a `<div id="root">` and a `<script type="module" src="/src/main.tsx">` entry.
- [X] T011 [P] Create `frontend/.gitignore` excluding `node_modules/`, `dist/`, `coverage/`, `.vite/`, `.DS_Store`.
- [X] T012 Create the source tree skeleton (empty directories with `.gitkeep` where needed): `frontend/src/{routes,features,components/ui,components/layout,stores,lib,data,styles,test}`, `frontend/tests/{lib,features,routes}`, `frontend/scripts/`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Design system, seed data, shared utilities, stores, layout shell,
and router — everything every user story depends on.

**CRITICAL**: No user story phase may start until this phase is complete.

- [X] T013 [P] Invoke the `frontend-design` skill to produce the design language (palette, typography scale, spacing scale, radii, shadows, interaction/focus states, component visual rules) for the prototype; capture the output as design tokens in `frontend/src/styles/tokens.css` (CSS custom properties in `:root` and `[data-theme="dark"]`).
- [X] T014 [P] Create `frontend/src/styles/globals.css` importing Tailwind layers (`@tailwind base; @tailwind components; @tailwind utilities;`), setting base typography from tokens, and defining a visible `:focus-visible` ring consistent with the design system.
- [X] T015 [P] Create `frontend/scripts/generate-seed.ts` that deterministically synthesizes 20–30 tickers across 5–6 sectors, 5 years of daily OHLCV per ticker, per-ticker KeyStats, and a 6–8-holding seed portfolio with 2–3 transactions each; outputs `frontend/src/data/tickers.json`, `prices.json`, `stats.json`, `seed-portfolio.json`; wire to `npm run generate-seed`.
- [X] T016 Run `npm run generate-seed` to produce the initial `frontend/src/data/*.json` files and commit them.
- [X] T017 [P] Create `frontend/src/lib/format.ts` exporting pure formatters: `formatCurrency(n, currency="USD")`, `formatPercent(n, digits=2)`, `formatSignedPercent(n)`, `formatNumber(n, digits)`, `formatDateISO(d)`, `formatCompactNumber(n)` — all locale-aware via `Intl`.
- [X] T018 [P] Create `frontend/src/lib/seed.ts` exporting typed loaders `loadTickers()`, `loadPrices()`, `loadStats()`, `loadSeedPortfolio()` that import the JSON and return strongly-typed objects keyed by symbol; include a `findTicker(symbol)` helper and an `isKnownTicker(symbol)` predicate.
- [X] T019 [P] Create `frontend/src/lib/portfolio.ts` exporting pure functions: `computeHoldings(transactions, prices)`, `computePortfolio(holdings)`, `currentPrice(symbol, prices)`, `dayChange(symbol, prices)`; implements the weighted-average cost-basis method from `data-model.md`.
- [X] T020 [P] Create `frontend/tests/lib/format.test.ts` covering each formatter (positive, negative, zero, very large, null-ish inputs).
- [X] T021 [P] Create `frontend/tests/lib/portfolio.test.ts` covering holdings aggregation, weighted-average cost basis across multiple buys, sell reduces shares proportionally, closed positions (shares=0) excluded, portfolio totals reconcile against holdings, day-change math.
- [X] T022 Create `frontend/src/stores/uiStore.ts` (Zustand) holding `theme: "light" | "dark"` with a `toggleTheme()` action and `persist` middleware keyed `stocktracker.ui`.
- [X] T023 Create `frontend/src/test/setup.ts` registering `@testing-library/jest-dom` matchers, a `vitest-axe` matcher (`toHaveNoViolations`), and a `matchMedia` polyfill; create `frontend/src/test/utils.tsx` exporting a `renderWithProviders()` helper that wraps in `MemoryRouter`.
- [X] T024 [P] Create `frontend/src/components/ui/Button.tsx`, `Card.tsx`, `Input.tsx`, `Label.tsx`, `Tabs.tsx`, `Table.tsx`, `Dialog.tsx`, `Toast.tsx`, `EmptyState.tsx` using tokens from T013/T014 and satisfying the interaction states requirement (hover, focus, active, disabled, loading, error) from FR-023; every interactive component forwards refs and accepts `aria-*` props.
- [X] T025 [P] Create `frontend/src/components/layout/AppShell.tsx`, `Sidebar.tsx` (desktop), `BottomTabBar.tsx` (mobile, <768px), `TopBar.tsx` (with global search affordance), `PageHeader.tsx`; composes into a single shell with a semantic `<main>` region.
- [X] T026 Create `frontend/src/App.tsx` defining the router with routes `/`, `/watchlists`, `/watchlists/:id`, `/transactions`, `/analysis/:ticker`, and a catch-all 404; wraps children in `AppShell`.
- [X] T027 Create `frontend/src/main.tsx` mounting `<App />` inside `React.StrictMode` and `BrowserRouter`, importing `styles/globals.css`.
- [X] T028 [P] Create `frontend/tests/components/ui/Button.test.tsx` and `frontend/tests/components/ui/Dialog.test.tsx` verifying keyboard operation, focus management (Dialog traps focus, restores on close), and disabled state.
- [X] T029 [P] Create `frontend/tests/components/layout/AppShell.test.tsx` verifying primary nav renders, current route is indicated (aria-current), and layout reflows to BottomTabBar below 768px (emulated viewport).

**Checkpoint**: `npm run verify` passes. User story phases may now proceed in parallel.

---

## Phase 3: User Story 1 — Portfolio Dashboard (Priority: P1) — MVP

**Goal**: On load, show a dashboard of total portfolio value, cost basis, P&L,
day change, allocation, and a sortable holdings table — with a professional,
designed empty state when no holdings exist.

**Independent Test**: Load the app with seeded demo transactions; the Dashboard
route renders summary tiles, an allocation visualization, and a holdings table
whose numbers reconcile with the tiles; sorting works; empty state renders when
all transactions are deleted.

### Tests for User Story 1

- [X] T030 [P] [US1] Create `frontend/tests/stores/portfolioStore.test.ts` covering first-run seeding from `seed-portfolio.json`, `addTransaction`, `removeTransaction`, `replaceAll` (used by CSV import), persistence key, derived holdings re-computation.
- [X] T031 [P] [US1] Create `frontend/tests/features/dashboard/SummaryTiles.test.tsx` asserting tiles render correctly for a fixture portfolio (value, cost, P&L absolute & %, day change) and handle the zero-cost-basis edge case.
- [X] T032 [P] [US1] Create `frontend/tests/features/dashboard/HoldingsTable.test.tsx` asserting header renders all specified columns, sorting by each numeric column toggles direction, sort indicator updates, clicking a row navigates to `/analysis/:ticker` (via router mock).
- [X] T033 [P] [US1] Create `frontend/tests/features/dashboard/AllocationChart.test.tsx` asserting the chart renders a segment per holding with weights summing to 1.0 and hover surfaces ticker + weight.
- [X] T034 [P] [US1] Create `frontend/tests/routes/DashboardRoute.test.tsx` smoke test: route renders with seeded data, and `vitest-axe` reports zero critical violations.
- [X] T035 [P] [US1] Create `frontend/tests/features/dashboard/EmptyState.test.tsx` asserting empty state renders with CTAs when no holdings exist.

### Implementation for User Story 1

- [X] T036 [US1] Create `frontend/src/stores/portfolioStore.ts` (Zustand + `persist`, key `stocktracker.portfolio`) with state `{ transactions: Transaction[] }` and actions `addTransaction`, `removeTransaction`, `replaceAll(transactions)`, plus a selector `selectHoldings()` that composes `lib/portfolio.computeHoldings` with loaded prices; first-run seed from `loadSeedPortfolio()` if store is empty.
- [X] T037 [P] [US1] Create `frontend/src/features/dashboard/SummaryTiles.tsx` rendering total value, total cost, unrealized P&L (absolute + %), day change tiles using tokens from T013 and formatters from T017.
- [X] T038 [P] [US1] Create `frontend/src/features/dashboard/HoldingsTable.tsx` rendering columns: ticker, name, shares, avg cost, current price, market value, weight %, unrealized P&L, day change; implements client-side sorting on any numeric column with a visible indicator; rows are keyboard-activatable and navigate to `/analysis/:ticker`.
- [X] T039 [P] [US1] Create `frontend/src/features/dashboard/AllocationChart.tsx` rendering a donut/pie via Recharts with hover tooltip showing ticker + weight; accessible fallback list included for screen readers.
- [X] T040 [P] [US1] Create `frontend/src/features/dashboard/EmptyState.tsx` with designed copy and CTAs linking to `/transactions` and `/watchlists`.
- [X] T041 [US1] Create `frontend/src/routes/DashboardRoute.tsx` composing `SummaryTiles`, `AllocationChart`, `HoldingsTable`, and `EmptyState`; reads from `portfolioStore` and price data.
- [X] T042 [US1] Verify quality gates on current scope: `npm run verify` passes end-to-end.

**Checkpoint**: US1 is demoable as an MVP. Stop and validate.

---

## Phase 4: User Story 2 — Watchlist (Priority: P2)

**Goal**: User can create, rename, and delete named watchlists; add/remove/reorder
tickers with inline validation; navigate to analysis from a ticker row; state
persists across reloads.

**Independent Test**: Create a watchlist named "Tech Majors", add 3 tickers (one
unknown ticker rejected inline), remove one, reorder one, reload page — state
persists. Delete the watchlist — it disappears.

### Tests for User Story 2

- [X] T043 [P] [US2] Create `frontend/tests/stores/watchlistStore.test.ts` covering `create`, `rename` (unique case-insensitive), `remove`, `addTicker` (rejects unknown + duplicate), `removeTicker`, `reorderTickers`, persistence key.
- [X] T044 [P] [US2] Create `frontend/tests/features/watchlist/WatchlistRow.test.tsx` asserting price + day change render, click → navigate to analysis.
- [X] T045 [P] [US2] Create `frontend/tests/features/watchlist/AddTickerInput.test.tsx` asserting unknown ticker shows inline error and is not added; known ticker is added and cleared.
- [X] T046 [P] [US2] Create `frontend/tests/routes/WatchlistsRoute.test.tsx` and `frontend/tests/routes/WatchlistDetailRoute.test.tsx` as smoke + axe tests for both pages.

### Implementation for User Story 2

- [X] T047 [US2] Create `frontend/src/stores/watchlistStore.ts` (Zustand + `persist`, key `stocktracker.watchlists`) holding `Watchlist[]` and actions `create(name)`, `rename(id, name)`, `remove(id)`, `addTicker(id, symbol)`, `removeTicker(id, symbol)`, `reorderTickers(id, from, to)`; enforces name uniqueness (case-insensitive) and ticker catalog membership.
- [X] T048 [P] [US2] Create `frontend/src/features/watchlist/WatchlistRow.tsx` rendering symbol, name, current price, day change (absolute + %), and a remove button; row click navigates to analysis.
- [X] T049 [P] [US2] Create `frontend/src/features/watchlist/AddTickerInput.tsx` using React Hook Form + Zod; inline error for unknown ticker; submits via Enter.
- [X] T050 [P] [US2] Create `frontend/src/features/watchlist/WatchlistHeader.tsx` with rename (inline edit, validated) and delete (confirm dialog) controls.
- [X] T051 [P] [US2] Create `frontend/src/features/watchlist/NewWatchlistDialog.tsx` using the shared `Dialog` component, RHF + Zod validation on name.
- [X] T052 [US2] Create `frontend/src/routes/WatchlistsRoute.tsx` rendering the index (list of watchlists with counts, New Watchlist CTA, empty state).
- [X] T053 [US2] Create `frontend/src/routes/WatchlistDetailRoute.tsx` at `/watchlists/:id` composing `WatchlistHeader`, `AddTickerInput`, and a reorderable list of `WatchlistRow` using native HTML5 drag-and-drop or keyboard reordering (up/down buttons).
- [X] T054 [US2] Verify quality gates on current scope: `npm run verify` passes end-to-end.

**Checkpoint**: US1 and US2 both work independently.

---

## Phase 5: User Story 3 — Stock Analysis (Priority: P2)

**Goal**: From any entry point (dashboard, watchlist, search), open an
analysis view that shows a price chart with selectable time ranges, a key-stats
grid, and — when the user owns the ticker — a position summary.

**Independent Test**: Navigate to `/analysis/AAPL`; chart renders within 2s;
switching across 1D/1W/1M/3M/1Y/5Y/ALL updates the chart and highlights the
selected range; stats grid populates; if AAPL is held, position summary matches
dashboard figures; for an unknown ticker, "Not found" page renders with a
dashboard link.

### Tests for User Story 3

- [X] T055 [P] [US3] Create `frontend/tests/features/analysis/PriceChart.test.tsx` asserting each range filters the dataset to the expected number of bars and the selected range has `aria-pressed="true"`.
- [X] T056 [P] [US3] Create `frontend/tests/features/analysis/KeyStatsGrid.test.tsx` asserting every stat renders; missing/null values render as em dash (never "NaN" or empty).
- [X] T057 [P] [US3] Create `frontend/tests/features/analysis/PositionSummary.test.tsx` asserting summary renders only when shares > 0 and values match `computeHoldings` for the ticker.
- [X] T058 [P] [US3] Create `frontend/tests/routes/AnalysisRoute.test.tsx` smoke + axe, plus an "unknown ticker" case asserting the 404-style not-found UI.

### Implementation for User Story 3

- [X] T059 [P] [US3] Create `frontend/src/features/analysis/PriceChart.tsx` using Recharts; accepts `symbol` and `range`; renders line/area chart; keyboard-accessible range buttons expose `aria-pressed`.
- [X] T060 [P] [US3] Create `frontend/src/features/analysis/KeyStatsGrid.tsx` rendering the nine stats specified in `data-model.md` using formatters from T017; null values render `—`.
- [X] T061 [P] [US3] Create `frontend/src/features/analysis/PositionSummary.tsx` consuming `portfolioStore` selector for the symbol and rendering shares, average cost, market value, unrealized P&L.
- [X] T062 [P] [US3] Create `frontend/src/features/analysis/AnalysisHeader.tsx` with symbol, company name, current price, and day change badge.
- [X] T063 [US3] Create `frontend/src/routes/AnalysisRoute.tsx` at `/analysis/:ticker` composing the header, chart, stats, and conditional position summary; renders a not-found state when the symbol is not in the catalog.
- [X] T064 [P] [US3] Create `frontend/src/components/layout/TickerSearch.tsx` mounted in `TopBar` — combobox backed by the ticker catalog; selecting a result navigates to `/analysis/:ticker`.
- [X] T065 [US3] Verify quality gates on current scope: `npm run verify` passes end-to-end.

**Checkpoint**: US1, US2, and US3 all work independently.

---

## Phase 6: User Story 4 — Import / Export Transactions (Priority: P3)

**Goal**: User can import a CSV of transactions (with validation preview that
flags invalid rows), confirm the import into their portfolio, and export the
current transactions to a CSV that round-trips cleanly.

**Independent Test**: Upload `sample-transactions.csv` containing a mix of valid
and invalid rows; preview flags the invalid rows with reasons; Confirm import
updates the dashboard. Click Export; download the file; clear the store; re-import
the downloaded file; dashboard totals match the original state (SC-008).

### Tests for User Story 4

- [X] T066 [P] [US4] Create `frontend/src/test/fixtures/sample-transactions.csv` containing at least 10 valid rows and 2 invalid rows (one unknown ticker, one future date).
- [X] T067 [P] [US4] Create `frontend/tests/lib/csv.test.ts` covering parsing, normalization (uppercased ticker, lowercased type, trimmed whitespace), default `fees` handling, every invalid-reason path, and a round-trip `export → import` equality check over a fixture portfolio.
- [X] T068 [P] [US4] Create `frontend/tests/features/transactions/ImportPreview.test.tsx` asserting invalid rows are visually flagged with reason text and excluded from commit; counts (valid / invalid) shown.
- [X] T069 [P] [US4] Create `frontend/tests/features/transactions/ExportButton.test.tsx` asserting clicking Export triggers a download with canonical header and LF line endings (verify via a captured Blob).
- [X] T070 [P] [US4] Create `frontend/tests/routes/TransactionsRoute.test.tsx` smoke + axe.

### Implementation for User Story 4

- [X] T071 [P] [US4] Create `frontend/src/lib/csv.ts` exporting: `transactionRowSchema` (Zod, matches `csv-transaction-schema.md`), `parseTransactionsCSV(text): { valid: Transaction[]; invalid: { row: number; reason: string; raw: Record<string,string> }[] }` using PapaParse, and `serializeTransactionsCSV(transactions): string` emitting the canonical header with LF endings and RFC 4180 quoting.
- [X] T072 [P] [US4] Create `frontend/src/features/transactions/ImportDropzone.tsx` handling file selection (click or drag-drop), reading as text, and invoking `parseTransactionsCSV`.
- [X] T073 [P] [US4] Create `frontend/src/features/transactions/ImportPreview.tsx` rendering a unified table with a status column (`valid` / `invalid: reason`); a "Confirm import" button writes only the valid rows via `portfolioStore.replaceAll([...existing, ...valid])` (or a configurable merge — default: append).
- [X] T074 [P] [US4] Create `frontend/src/features/transactions/ExportButton.tsx` that calls `serializeTransactionsCSV(store.transactions)`, constructs a `Blob`, and triggers a download named `stocktracker-transactions-YYYYMMDD.csv`.
- [X] T075 [P] [US4] Create `frontend/src/features/transactions/TransactionsTable.tsx` listing committed transactions sortable by date with per-row delete + confirm.
- [X] T076 [US4] Create `frontend/src/routes/TransactionsRoute.tsx` composing `ImportDropzone` → `ImportPreview`, `ExportButton`, and `TransactionsTable`; empty state when no transactions.
- [X] T077 [US4] Verify quality gates on current scope: `npm run verify` passes end-to-end.

**Checkpoint**: All four user stories are independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final checks required by spec Success Criteria and constitution gates
across all stories — not new features.

- [X] T078 [P] Add `frontend/tests/a11y/routes.axe.test.tsx` running `vitest-axe` against every primary route (`/`, `/watchlists`, `/watchlists/:id` with a fixture, `/transactions`, `/analysis/:ticker`) and asserting zero critical violations (SC-006).
- [X] T079 [P] Add `frontend/tests/responsive/viewports.test.tsx` that renders each primary route at emulated widths 375, 768, 1280, 1920 and asserts no element overflows the viewport (SC-007).
- [X] T080 [P] Add keyboard-navigation tests under `frontend/tests/a11y/keyboard.test.tsx` verifying that each primary route is fully operable via Tab/Shift-Tab/Enter/Space/Escape (FR-024).
- [X] T081 [P] Create `frontend/README.md` linking to `specs/001-frontend-prototype/quickstart.md` and listing the `npm run verify` commands; no duplication of quickstart content.
- [X] T082 Run `npm run verify` from `frontend/` and confirm all three constitution gates are green (test, lint, typecheck + build). If anything fails, fix the root cause — do NOT silence lints or skip tests.
- [ ] T083 Walk through the quickstart.md per-user-story verification checklist manually in `npm run dev` across desktop and mobile viewports; record any deviations from spec acceptance scenarios and open fixes as follow-up tasks (not in this feature's scope to add more FRs).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — starts immediately.
- **Foundational (Phase 2)**: depends on Setup; BLOCKS all user story phases.
- **User Stories (Phases 3–6)**: each depends only on Foundational (Phase 2).
  Phases 3–6 can proceed in parallel with different owners after Phase 2.
- **Polish (Phase 7)**: depends on all in-scope user stories being complete.

### User Story Dependencies

- **US1 Dashboard (P1)**: depends only on Phase 2. MVP candidate.
- **US2 Watchlist (P2)**: depends only on Phase 2. Independent of US1.
- **US3 Stock Analysis (P2)**: depends only on Phase 2. Its Position Summary
  subcomponent reads the portfolio store (populated by US1's seeding in Phase 2),
  but the route itself does not require US1's UI; US3 remains independently
  testable via fixture-seeded store state.
- **US4 Import/Export (P3)**: depends only on Phase 2. Writes into the portfolio
  store, which US1 also reads; independence is preserved because each story owns
  different entry points and the store is foundational (built in Phase 2).

### Within Each User Story

- Tests in the story's "Tests for" subsection may be written first (TDD) or
  alongside implementation, but MUST pass before the story's final verify task.
- Stores → feature components → route composition, in that order.
- Each story ends with a verify task confirming the constitution gates.

### Parallel Opportunities

- Phase 1: T004–T011 are all independent config files — run in parallel.
- Phase 2: T013 (tokens), T014 (globals.css), T015 (seed generator), T017
  (format), T018 (seed loader), T019 (portfolio math), T020/T021 (lib tests),
  T024 (UI primitives), T025 (layout) are all [P].
- Within each user story, all `[P]` tests and non-overlapping feature components
  can proceed in parallel.

---

## Parallel Example: User Story 1

```bash
# Launch all tests for US1 in parallel:
Task: "tests/stores/portfolioStore.test.ts"
Task: "tests/features/dashboard/SummaryTiles.test.tsx"
Task: "tests/features/dashboard/HoldingsTable.test.tsx"
Task: "tests/features/dashboard/AllocationChart.test.tsx"
Task: "tests/features/dashboard/EmptyState.test.tsx"
Task: "tests/routes/DashboardRoute.test.tsx"

# Launch all independent dashboard components in parallel:
Task: "src/features/dashboard/SummaryTiles.tsx"
Task: "src/features/dashboard/HoldingsTable.tsx"
Task: "src/features/dashboard/AllocationChart.tsx"
Task: "src/features/dashboard/EmptyState.tsx"
```

---

## Implementation Strategy

### MVP First (US1 only)

1. Complete Phase 1 (Setup) — quality gates wired.
2. Complete Phase 2 (Foundational) — design tokens, seed data, shared lib,
   shared UI/layout, router.
3. Complete Phase 3 (US1 Dashboard).
4. **Stop and validate**: `npm run verify` green; manually walk through US1
   acceptance scenarios. This is the MVP; demoable on its own.

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. Add US1 → validate → demo (MVP).
3. Add US2 Watchlist → validate → demo.
4. Add US3 Stock Analysis → validate → demo.
5. Add US4 Import/Export → validate → demo.
6. Polish (Phase 7) → final a11y/responsive/keyboard sweep → `npm run verify`.

### Parallel Team Strategy

After Phase 2 completes:

- Dev A: US1 Dashboard.
- Dev B: US2 Watchlist.
- Dev C: US3 Stock Analysis.
- Dev D: US4 Import/Export.

Stories integrate through the shared stores built in Phase 2; no cross-story
code should be added without a corresponding spec update.

---

## Notes

- `[P]` = different files, no dependencies on incomplete tasks.
- Every story phase ends with a `verify` task; per the constitution (Principles
  I–III), a feature is not complete until `npm run verify` exits 0.
- The `frontend-design` skill (T013) is the visual-design source of truth —
  components built under `components/ui/` in T024 MUST consume its tokens; do
  not re-introduce generic AI-defaults styling.
- The seed generator (T015/T016) produces static synthetic data committed to the
  repo; no network calls at runtime.
- Avoid cross-story coupling: feature code lives under
  `src/features/<story>/`; any symbol imported across stories must live in
  `src/lib/`, `src/components/`, or `src/stores/` — never in another feature's
  folder.
