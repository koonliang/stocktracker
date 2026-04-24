# Phase 0 Research: StockTracker Frontend Prototype

Purpose: resolve all `NEEDS CLARIFICATION` items from the Technical Context and
record best-practice decisions for each major technology choice, so that Phase 1
design and later implementation have no open questions to re-litigate.

Result: all items below are **Decided**. Zero open `NEEDS CLARIFICATION`.

---

## 1. Frontend framework & build

- **Decision**: React 18 + Vite 5 + TypeScript 5 (strict mode).
- **Rationale**: User mandated React + Vite. TypeScript is required to make
  Constitution III (Compilation Integrity) meaningful for a JS stack — `tsc
  --noEmit` is the compile gate.
- **Alternatives considered**: CRA (deprecated), Next.js (SSR unnecessary for a
  static prototype, adds complexity), Remix (same).

## 2. Styling & design system approach

- **Decision**: Tailwind CSS 3 with design tokens defined in `styles/tokens.css`
  (CSS variables) and consumed via Tailwind theme extension. Components are
  hand-built under `components/ui/` using those tokens; no prebuilt component
  kit is adopted.
- **Rationale**: The spec's "must not look vibe-coded" requirement (FR-022 +
  SC-005) is best met by a *deliberate* design language. Pulling in a generic
  kit (e.g., shadcn defaults) risks the stock AI-assistant look. Tailwind +
  tokens lets the `frontend-design` skill define a distinctive palette,
  typography, and component language while keeping the implementation simple.
- **Alternatives considered**:
  - shadcn/ui → good primitives but distinctive look requires heavy
    customization; risks the very aesthetic we want to avoid.
  - Chakra UI / Mantine → opinionated defaults fight the custom design
    direction.
  - Plain CSS modules → higher friction for responsive + state variants.

## 3. State management & persistence

- **Decision**: Zustand 4 with `persist` middleware writing to `localStorage`.
  One store per domain: `portfolioStore` (transactions), `watchlistStore`
  (named lists), `uiStore` (theme/nav).
- **Rationale**: Simpler than Redux Toolkit, typed, minimal boilerplate, and
  `persist` satisfies FR-020 (reload persistence) without writing custom
  serialization. One-store-per-domain keeps tests isolated (Constitution I).
- **Alternatives considered**:
  - Redux Toolkit → heavier, unnecessary for prototype scale.
  - React Context + useReducer → persistence has to be hand-rolled; ergonomics
    suffer once there are 3 domains.
  - IndexedDB (via Dexie) → richer than needed; our totals comfortably fit in
    localStorage's 5–10 MB budget.

## 4. Routing

- **Decision**: React Router v6 (data-router form not required for prototype).
  Routes: `/` → Dashboard, `/watchlists`, `/watchlists/:id`,
  `/transactions` (import/export), `/analysis/:ticker`.
- **Rationale**: De facto standard; integrates cleanly with code-splitting if
  needed later.
- **Alternatives considered**: TanStack Router (strongly typed but larger API
  surface; not needed at this scale).

## 5. Charting library (Stock Analysis)

- **Decision**: Recharts for both the price chart and the allocation
  visualization.
- **Rationale**: Simple React-idiomatic API; responsive out of the box; keeps
  bundle reasonable; good enough fidelity for a prototype chart with time-range
  selector (FR-011). Covers line, area, and pie/donut used in the dashboard
  allocation widget.
- **Alternatives considered**:
  - TradingView `lightweight-charts` → best fidelity for price charts, but
    imperative API and larger footprint; overkill for prototype.
  - visx/d3 → maximum control but high implementation cost; violates
    Constitution IV (simplicity).

## 6. CSV import/export

- **Decision**: PapaParse for parsing; native `Blob` + anchor download for
  export. Row-level validation with Zod schema in `lib/csv.ts`.
- **Rationale**: PapaParse handles streaming, quoting, and encoding edge cases
  robustly. Zod gives one source of truth for row shape and yields typed
  errors for the preview UI (FR-015).
- **Alternatives considered**: hand-rolled parser (fragile with quoted commas),
  `csv-parse` (Node-oriented), SheetJS (overkill; binary formats out of scope).

## 7. Canonical CSV schema

- **Decision**: Fixed header set: `date, ticker, type, quantity, price, fees`.
  `date` is ISO-8601 `YYYY-MM-DD`; `type` is `buy` or `sell`; `quantity` and
  `price` are positive decimals; `fees` is a non-negative decimal (defaults to
  0 if column present but blank). Documented in
  `contracts/csv-transaction-schema.md`. Round-trip equivalence required by
  SC-008.
- **Rationale**: Minimal, unambiguous, easy to validate; matches buy/sell
  scope in spec Assumptions.
- **Alternatives considered**: supporting broker-specific exports (Fidelity,
  Schwab, IBKR) — deferred; explicitly out of scope per spec.

## 8. Forms & validation

- **Decision**: React Hook Form + Zod resolver for any form inputs (watchlist
  naming, add-ticker dialog, optional manual transaction entry if time
  permits). Same Zod schemas are reused for CSV row validation.
- **Rationale**: Single validation source per entity; typed inference; small
  bundle.
- **Alternatives considered**: Formik (older ergonomics), plain controlled
  inputs (duplicates validation logic with CSV import).

## 9. Testing strategy

- **Decision**:
  - Unit: Vitest for `lib/*` pure functions (portfolio math, CSV parse/serialize,
    formatters). Target: 100% of pure-function branches.
  - Component: @testing-library/react for each feature's key interactions
    (dashboard sort, watchlist add/remove, CSV preview flag, analysis range
    switch).
  - Accessibility: `vitest-axe` assertion on each route's default render
    (satisfies SC-006 in CI).
  - E2E: deferred this iteration. Rationale: Playwright adds CI time and
    infrastructure; primary interactions are covered by component tests; a
    prototype's UX is validated manually per spec Success Criteria.
- **Rationale**: Meets Constitution I (tests must pass) without over-building.
- **Alternatives considered**: Jest (slower, duplicate ecosystem in Vite),
  Cypress (heavier than Playwright).

## 10. Linting & formatting

- **Decision**: ESLint with `@typescript-eslint`, `eslint-plugin-react`,
  `eslint-plugin-react-hooks`, and `eslint-plugin-jsx-a11y`. Prettier for
  formatting; `eslint-config-prettier` to disable stylistic conflicts. `npm
  run lint` runs both `eslint .` and `prettier --check .`.
- **Rationale**: `jsx-a11y` supports FR-024 (accessibility) at the lint level.
  Prettier removes style debate (constitution II).
- **Alternatives considered**: Biome — fast and modern but less mature plugin
  ecosystem for a11y rules.

## 11. Type-check & build as compile gate

- **Decision**: `npm run typecheck` → `tsc --noEmit`. `npm run build` → `vite
  build` (which also type-checks via `vue-tsc`-equivalent wrapper if
  configured; we rely on explicit `tsc --noEmit` for a deterministic gate).
  CI/local verification runs both.
- **Rationale**: Explicit separate type-check gives a clean Constitution III
  gate output independent of bundler warnings.
- **Alternatives considered**: relying solely on `vite build` — masks type
  errors in some test-only paths.

## 12. Seed dataset sourcing & size

- **Decision**: Ship 20–30 tickers across 5–6 sectors. For each: company name,
  sector, exchange, current key stats (open/high/low/prev close/volume/52w
  range/market cap/P/E), and 5 years of synthetic daily OHLC aligned to a
  plausible price walk. Generated by a one-off script committed under
  `frontend/scripts/generate-seed.ts`; outputs JSON under `frontend/src/data/`.
  Seed portfolio: 6–8 holdings across 2–3 transactions each.
- **Rationale**: No live feed per spec. Synthetic data keeps us clear of
  market-data licensing concerns (no real vendor scraping) while producing
  charts that look credible. 5Y history satisfies the "5Y" and "ALL" chart
  ranges (FR-011).
- **Alternatives considered**: checking in real historical CSVs from a public
  source — licensing ambiguity and stale-by-design data; not worth it for
  prototype.

## 13. Accessibility & theming

- **Decision**: WCAG 2.1 AA contrast enforced via design tokens (documented
  contrast ratios in `styles/tokens.css`). Keyboard focus rings visible on all
  interactive elements. Light theme by default; dark theme optional this
  iteration — if delivered, wired through `uiStore` with a data-attribute on
  `<html>`.
- **Rationale**: FR-024 and SC-006 are hard gates. Dark mode is explicitly
  optional per spec edge cases ("if a theme toggle is present").
- **Alternatives considered**: defer accessibility entirely → violates
  constitution and spec.

## 14. Responsiveness strategy

- **Decision**: Mobile-first Tailwind breakpoints (`sm/md/lg/xl`). Primary
  views tested at 375 / 768 / 1280 / 1920 (SC-007). Sidebar nav collapses to a
  bottom tab bar below `md`.
- **Rationale**: Simplest pattern that satisfies FR-021/SC-007.

## 15. Deployment

- **Decision**: Out of scope for this iteration beyond producing a buildable
  static output (`vite build` → `frontend/dist/`). No hosting setup committed.
- **Rationale**: Prototype is demoable locally via `npm run dev`; hosting will
  be decided when the backend lands.

---

## Open questions

None. All NEEDS CLARIFICATION items are resolved.
