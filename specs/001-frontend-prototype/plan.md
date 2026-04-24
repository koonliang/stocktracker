# Implementation Plan: StockTracker Frontend Prototype

**Branch**: `001-frontend-prototype` | **Date**: 2026-04-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-frontend-prototype/spec.md`

## Summary

Ship a frontend-only prototype of StockTracker covering four feature areas — Portfolio
Dashboard (P1), Watchlist (P2), Stock Analysis (P2), and Import/Export Transactions
(P3) — using React + Vite + TypeScript. No backend service is built in this iteration;
all ticker metadata, historical price series, and key statistics are loaded from a
bundled seed dataset, and user-created state (watchlists, imported transactions) is
persisted in the browser via `localStorage`. Visual design follows a deliberate,
consistent design system produced with the `frontend-design` skill during
implementation — explicitly not "vibe-coded" AI defaults. Quality gates (tests, lint,
type-check/build) are wired from day one per the project constitution.

## Technical Context

**Language/Version**: TypeScript 5.5+ (strict mode), targeting ES2022
**Primary Dependencies**:
- React 18, React Router v6 (routing)
- Vite 5 (build + dev server)
- Tailwind CSS 3 (utility styling, design tokens driven by frontend-design skill)
- Zustand 4 with `persist` middleware (global state + localStorage persistence)
- Recharts (price & allocation charts — prototype-grade, swappable later)
- PapaParse (CSV import/export)
- React Hook Form + Zod (form handling and CSV row validation)
- Lucide React (icons)

**Storage**: Browser `localStorage` via Zustand `persist` middleware. Seed data
(ticker catalog, historical prices, key statistics) shipped as static JSON under
`frontend/src/data/` and imported at module load; no server, no database.

**Testing**:
- Unit / component tests: Vitest + @testing-library/react + jsdom
- Accessibility assertions: vitest-axe (SC-006 gate)
- E2E: deferred — not required for prototype (flagged in research)

**Target Platform**: Modern evergreen browsers (last 2 versions of Chrome, Edge,
Safari, Firefox) on desktop and mobile; deploys as a static site.

**Project Type**: Web application — frontend-only this iteration; a `backend/`
tree is intentionally not created yet, but the top-level layout reserves room for
it to land in a later iteration without restructuring.

**Performance Goals**:
- First meaningful paint on primary views within 2s on a mid-range laptop (SC-004)
- Holdings/watchlist tables remain smooth at 100+ rows (FR edge case)
- Bundled seed JSON kept under ~500 KB gzipped to avoid hurting TTI

**Constraints**:
- No network calls for market data in this iteration — seed data only
- Must render cleanly at viewport widths 375px, 768px, 1280px, 1920px (SC-007)
- WCAG 2.1 AA contrast + keyboard reachability (FR-024, SC-006)
- Quality gates (test, lint, type-check/build) must all pass — constitution I/II/III

**Scale/Scope**:
- ~4 top-level routes (Dashboard, Watchlists, Import/Export, Analysis detail)
- ~20 distinct components
- Seed catalog: ~30 tickers with 5Y daily history is sufficient to populate demos
- Single user per browser profile

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Gates derived from `.specify/memory/constitution.md` v1.0.0:

| Principle | How this plan satisfies it | Status |
|-----------|----------------------------|--------|
| I. Test Verification (NON-NEGOTIABLE) | Vitest + RTL configured at project setup; component tests for dashboard summary math, CSV import validation, watchlist store reducers, and one smoke render per route. `npm test` is the test gate. | PASS |
| II. Lint & Style Compliance (NON-NEGOTIABLE) | ESLint (typescript-eslint + react-hooks + jsx-a11y) and Prettier configured; `npm run lint` is the lint gate. No rule is globally disabled without justification. | PASS |
| III. Compilation Integrity (NON-NEGOTIABLE) | `tsc --noEmit` and `vite build` both run in CI/local verification; `npm run typecheck` and `npm run build` together are the compile gate. | PASS |
| IV. Simplicity & YAGNI | State via Zustand (not Redux); localStorage persist (not IndexedDB); Recharts (not custom D3); no backend scaffolding, no auth, no SSR. Deferred: e2e runner, Storybook, i18n. | PASS |
| V. Specification-Driven Development | Work flows `/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement`. Scope locked to spec FRs; no out-of-spec features added during implementation. | PASS |

**Post-design re-check (after Phase 1)**: PASS — data model, UI contracts, and
quickstart introduce no new dependencies or abstractions beyond those listed in
Technical Context. No entries in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-frontend-prototype/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (UI route + CSV schema contracts)
│   ├── routes.md
│   └── csv-transaction-schema.md
├── checklists/
│   └── requirements.md  # From /speckit-specify
└── tasks.md             # /speckit-tasks output (not created here)
```

### Source Code (repository root)

```text
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.ts
├── postcss.config.js
├── .eslintrc.cjs
├── .prettierrc
├── src/
│   ├── main.tsx                       # app entry, router bootstrap
│   ├── App.tsx                        # route shell + nav chrome
│   ├── routes/
│   │   ├── DashboardRoute.tsx
│   │   ├── WatchlistsRoute.tsx
│   │   ├── ImportExportRoute.tsx
│   │   └── AnalysisRoute.tsx          # /analysis/:ticker
│   ├── features/
│   │   ├── dashboard/                 # holdings table, summary tiles, allocation viz
│   │   ├── watchlist/                 # list CRUD, ticker rows
│   │   ├── analysis/                  # chart, stats, position summary
│   │   └── transactions/              # import preview, exporter
│   ├── components/                    # shared design-system primitives
│   │   ├── ui/                        # Button, Card, Table, Tabs, Dialog, Input, ...
│   │   └── layout/                    # AppShell, Sidebar, TopBar, Breadcrumbs
│   ├── stores/
│   │   ├── portfolioStore.ts          # transactions + derived holdings
│   │   ├── watchlistStore.ts          # named watchlists
│   │   └── uiStore.ts                 # theme, nav state
│   ├── lib/
│   │   ├── portfolio.ts               # holdings/P&L derivations (pure)
│   │   ├── csv.ts                     # parse/serialize + Zod schema
│   │   ├── format.ts                  # currency/percent/date formatters
│   │   └── seed.ts                    # loads bundled ticker/price JSON
│   ├── data/
│   │   ├── tickers.json               # catalog (symbol, name, sector)
│   │   ├── prices.json                # daily OHLCV per ticker
│   │   ├── stats.json                 # key statistics per ticker
│   │   └── seed-portfolio.json        # demo portfolio transactions
│   ├── styles/
│   │   ├── globals.css
│   │   └── tokens.css                 # design tokens (colors, spacing, type)
│   └── test/
│       ├── setup.ts                   # Vitest setup, jsdom, vitest-axe
│       └── utils.tsx                  # render helper with providers
└── tests/
    ├── lib/                           # pure-function unit tests
    ├── features/                      # component/integration tests
    └── routes/                        # route-level smoke tests

backend/                               # reserved for future iteration (NOT created now)
```

**Structure Decision**: Web-application layout with a `frontend/` package at the
repo root. `backend/` is intentionally omitted this iteration (spec Assumptions:
"Scope is frontend-only"). When the Java Quarkus service lands in a later
iteration it will sit alongside as a sibling `backend/` directory without
restructuring the frontend. Feature code is grouped under `frontend/src/features/`
by product area (dashboard/watchlist/analysis/transactions) rather than by
technical layer, to keep each feature independently testable per constitution I.

## Complexity Tracking

No constitutional violations. This section intentionally left empty.
