# Tasks: UI/UX Consistency & Mobile Responsive Design

**Input**: Design documents from `specs/010-ui-ux-mobile-responsive/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-patterns.md

**Tests**: Automated test tasks are created for feature behavior. Verification of whether the feature is complete is performed manually against the acceptance scenarios in spec.md using relevant test evidence.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)

## Path Conventions

Web app frontend: `frontend/src/`, `frontend/tests/`

---

## Phase 1: Setup (Test Infrastructure)

**Purpose**: Extend the existing viewport test suite to cover the full acceptance criteria before implementation begins.

- [x] T001 Add `320` to the `VIEWPORTS` array in `frontend/tests/responsive/viewports.test.tsx` (current: `[375, 768, 1280, 1920]`)
- [x] T002 [P] Add `AlertsRoute` render + overflow test cases to `frontend/tests/responsive/viewports.test.tsx` (import from `@/routes/AlertsRoute`)
- [x] T003 [P] Add `PerformanceRoute` render + overflow test cases to `frontend/tests/responsive/viewports.test.tsx` (import from `@/routes/PerformanceRoute`)

**Checkpoint**: Run `npm test -- viewports` in `frontend/` — tests may fail (expected); the suite must at minimum run without syntax errors

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No shared infrastructure changes required — all work is route-level and can proceed immediately after Phase 1.

*(No tasks — Phase 3+ can begin after Phase 1 is complete)*

---

## Phase 3: User Story 1 — Mobile No Horizontal Scrolling (Priority: P1) 🎯 MVP

**Goal**: Every authenticated page renders without page-level horizontal overflow at 320–430px viewports.

**Independent Test**: Run `npm test -- viewports` in `frontend/`. All tests at 320px must pass. Manually open each page in Chrome DevTools at 375px and run `document.querySelectorAll('*').forEach(el => { if (el.scrollWidth > window.innerWidth) console.warn(el.tagName, el.className); })` in the console — no elements should log.

### Tests for User Story 1

- [x] T004 [US1] Verify `viewports.test.tsx` passes for all 7 routes at 320px after Phase 1 changes — run `npm test -- viewports` in `frontend/` and fix any import errors

### Implementation for User Story 1

Apply the overflow audit checklist from `contracts/ui-patterns.md` to each page. Wrap any data table in `<div className="overflow-x-auto rounded-lg border border-border">`. Add `min-w-0` to any flex children that may blow out. Remove any fixed `w-[Xpx]` wider than the viewport.

- [x] T005 [P] [US1] Audit and fix horizontal overflow in `frontend/src/routes/DashboardRoute.tsx` — no changes needed; `Table` component has built-in `overflow-x-auto`, columns use `hideClass` for responsive breakpoints
- [x] T006 [P] [US1] Audit and fix horizontal overflow in `frontend/src/routes/WatchlistsRoute.tsx` — no changes needed; grid defaults to `grid-cols-1`, cards have `min-w-0`
- [x] T007 [P] [US1] Wrap the tickers table in `frontend/src/routes/WatchlistDetailRoute.tsx` — no changes needed; tickers use `<ul>` with `flex-col`, `WatchlistRow` uses `min-w-0 flex-1`
- [x] T008 [P] [US1] Wrap the transactions table in `frontend/src/routes/TransactionsRoute.tsx` — no changes needed; `TransactionsTable` uses `Table` component with built-in `overflow-x-auto`
- [x] T009 [P] [US1] Audit and fix horizontal overflow in `frontend/src/routes/PerformanceRoute.tsx` — no changes needed; all raw tables already wrapped in `overflow-x-auto`, grids use mobile-first `md:grid-cols-3`
- [x] T010 [P] [US1] Audit and fix horizontal overflow in `frontend/src/routes/AlertsRoute.tsx` — no changes needed; form grid defaults to 1 column on mobile, alert rows use `flex-col` on mobile
- [x] T011 [P] [US1] Audit and fix horizontal overflow in `frontend/src/routes/AnalysisRoute.tsx` — fixed `AnalysisHeader.tsx`: changed `flex shrink-0 items-baseline gap-3` to `flex min-w-0 flex-wrap items-baseline gap-x-3 gap-y-1` to prevent price/change overflow at narrow viewports

**Checkpoint**: All viewport tests at 320px pass. Manual overflow check on each page at 375px returns no console warnings.

---

## Phase 4: User Story 2 — Bottom Navigation Legible at 320px (Priority: P2)

**Goal**: All 5 bottom nav labels are fully visible with no overlap or clipping on a 320px-wide viewport.

**Independent Test**: Open Chrome DevTools at 320px width. The bottom nav shows: DASHBOARD, WATCHLISTS, TRADES, RETURNS, ALERTS — all fully readable, no label is clipped or overlapping its neighbor.

### Tests for User Story 2

- [x] T012 [US2] Add a `BottomTabBar` render test in `frontend/tests/responsive/viewports.test.tsx` that renders the component at 320px and asserts all 5 label strings are present in the DOM (use `getByText` for each expected label)

### Implementation for User Story 2

- [x] T013 [US2] In `frontend/src/components/layout/BottomTabBar.tsx`, change the label for the `/transactions` nav item from `"Transactions"` to `"Trades"` (the label string only — keep `to`, `icon`, and all styling unchanged)

**Checkpoint**: Render the app at 320px in Chrome DevTools — bottom nav shows TRADES (not TRANSACTIONS), all 5 items fit without overlap.

---

## Phase 5: User Story 3 — Consistent UI Design Across All Pages (Priority: P3)

**Goal**: All 7 authenticated pages use the same component and token patterns: `<PageHeader>`, `<Card>`, `<Button>`, `<Table>`, and the typography/color tokens from `data-model.md`.

**Independent Test**: Navigate through all 7 pages on desktop (1280px). Each page has a `<PageHeader>` with a title, cards use `bg-surface border border-border rounded-lg`, tables use the `<Table>` component, and typography consistently uses `text-headline`/`text-title`/`text-body` token classes.

### Tests for User Story 3

- [x] T014 [US3] Verify each authenticated route renders a `PageHeader` — add `getByRole('heading')` assertions to the existing route tests in `frontend/tests/routes/` for any route that is missing this check

### Implementation for User Story 3

Apply the Design Patterns Reference from `plan.md` to each page. Use `<PageHeader title="..." />` for the top heading. Ensure cards use `<Card>` or the standard `bg-surface border border-border rounded-lg` pattern. Replace ad-hoc font sizes with token classes.

- [x] T015 [P] [US3] Audit and align `frontend/src/routes/DashboardRoute.tsx` — ensure `<PageHeader>` is present, cards use consistent styling, typography uses token classes
- [x] T016 [P] [US3] Audit and align `frontend/src/routes/WatchlistsRoute.tsx` — ensure `<PageHeader>` is present, watchlist cards use consistent styling
- [x] T017 [P] [US3] Audit and align `frontend/src/routes/WatchlistDetailRoute.tsx` — ensure `<PageHeader>` with watchlist name, table uses `<Table>` component
- [x] T018 [P] [US3] Audit and align `frontend/src/routes/TransactionsRoute.tsx` — ensure `<PageHeader>` is present, table uses `<Table>` component, action buttons use `<Button>`
- [x] T019 [P] [US3] Audit and align `frontend/src/routes/PerformanceRoute.tsx` — ensure `<PageHeader>` is present, metric cards use consistent `bg-surface` styling, section headings use `text-title` token
- [x] T020 [P] [US3] Audit and align `frontend/src/routes/AlertsRoute.tsx` — ensure `<PageHeader>` is present, alert cards use consistent styling
- [x] T021 [P] [US3] Audit and align `frontend/src/routes/AnalysisRoute.tsx` — ensure `<PageHeader>` with ticker symbol, panels use consistent card styling

**Checkpoint**: Navigate all 7 pages on desktop. Each has a visible page heading, cards share the same visual weight, and no page has raw/unstyled HTML.

---

## Phase 6: User Story 4 — Polished Production-Grade Design & Empty States (Priority: P4)

**Goal**: All pages that display data collections show a well-designed `<EmptyState>` when no data is present. No page looks like an unstyled prototype.

**Independent Test**: Seed the app with no data (clear localStorage). Load each page. Pages with data collections (Dashboard, Watchlists, WatchlistDetail, Transactions, Performance, Alerts) each display an `<EmptyState>` with a meaningful title and description — no blank white space.

### Tests for User Story 4

- [x] T022 [US4] Audit existing route tests in `frontend/tests/routes/` — add or verify `EmptyState` render assertions for each page when data stores are empty (use the existing store reset patterns in the test files)

### Implementation for User Story 4

Use `<EmptyState title="..." description="..." />` from `frontend/src/components/ui/EmptyState.tsx`. Follow the naming convention from `contracts/ui-patterns.md`: title = "No [noun] yet", description = next-action hint.

- [x] T023 [P] [US4] Ensure `<EmptyState title="No positions yet" description="Add a transaction to start tracking your portfolio" />` renders in `frontend/src/routes/DashboardRoute.tsx` when no portfolio positions exist
- [x] T024 [P] [US4] Ensure `<EmptyState title="No watchlists yet" description="Create a watchlist to start tracking stocks" />` renders in `frontend/src/routes/WatchlistsRoute.tsx` when watchlists array is empty
- [x] T025 [P] [US4] Ensure `<EmptyState title="No stocks yet" description="Add a ticker to this watchlist to start tracking" />` renders in `frontend/src/routes/WatchlistDetailRoute.tsx` when the watchlist has no tickers
- [x] T026 [P] [US4] Ensure `<EmptyState title="No transactions yet" description="Import or add transactions to see your history" />` renders in `frontend/src/routes/TransactionsRoute.tsx` when transactions array is empty
- [x] T027 [P] [US4] Ensure `<EmptyState title="No performance data yet" description="Add transactions to see your portfolio returns" />` renders in `frontend/src/routes/PerformanceRoute.tsx` when no positions exist
- [x] T028 [P] [US4] Ensure `<EmptyState title="No alerts yet" description="Set a price alert to get notified when a stock moves" />` renders in `frontend/src/routes/AlertsRoute.tsx` when alerts array is empty

**Checkpoint**: Clear localStorage and load each page. Every data-collection page shows a readable empty state message — no blank space or broken layout.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verify compilation, run the full test suite, and confirm the feature against acceptance criteria.

- [x] T029 Run `npm run typecheck` in `frontend/` — zero TypeScript errors (Constitution gate II)
- [x] T030 Run `npm test` in `frontend/` — all tests pass including new viewport tests (Constitution gate I)
- [x] T031 Run `npm run build` in `frontend/` — production build succeeds (Constitution gate II)
- [ ] T032 [P] Manual verification: open app in Chrome DevTools at 320px and walk through all 7 authenticated pages per `quickstart.md` acceptance checklist

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 3 (US1)**: Depends on Phase 1 (viewport test infrastructure ready)
- **Phase 4 (US2)**: Independent of Phase 3 — can run in parallel with Phase 3
- **Phase 5 (US3)**: Independent of Phase 3 and 4 — can run in parallel
- **Phase 6 (US4)**: Independent of Phase 3, 4, 5 — can run in parallel
- **Phase 7 (Polish)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: No dependencies on other stories — start after Phase 1
- **US2 (P2)**: No dependencies — start anytime (single file change)
- **US3 (P3)**: No dependencies — can proceed alongside US1 (different concerns on same files)
- **US4 (P4)**: No dependencies — can proceed alongside US1/US3

### Within Each User Story

- All [P]-marked tasks within a story touch different files → run in parallel
- T004 (test check) must run after T001–T003
- T012 (BottomTabBar test) must run after T013 (label rename) to test the correct label

### Parallel Opportunities

Within each phase, all [P] tasks operate on distinct files and can be worked simultaneously. If working alone, complete one page at a time across US1+US3+US4 for that page (batch per-page work).

---

## Parallel Example: Per-Page Batch (Single Developer)

```text
# Complete one page end-to-end (US1 + US3 + US4) before moving to next:

For DashboardRoute.tsx:
  Task T005 [US1]: Fix overflow
  Task T015 [US3]: Align design consistency  
  Task T023 [US4]: Add empty state
  → Run: npm test -- DashboardRoute (verify)

For TransactionsRoute.tsx:
  Task T008 [US1]: Wrap table in scroll container
  Task T018 [US3]: Align design consistency
  Task T026 [US4]: Add empty state
  → Run: npm test -- TransactionsRoute (verify)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (extend viewport tests)
2. Complete Phase 3: US1 overflow fixes on all 7 pages
3. Run `npm test -- viewports` — all 320px tests pass
4. **STOP and VALIDATE**: Open app at 375px, run overflow console check on each page

### Incremental Delivery

1. Phase 1 → Phase 3 (US1) → Overflow fixed, tests green
2. Phase 4 (US2) → BottomTabBar legible at 320px
3. Phase 5 (US3) → Design consistent across all pages
4. Phase 6 (US4) → Empty states present on all pages
5. Phase 7 → TypeScript + build + test suite green

### Parallel Team Strategy

With 2+ developers:
- Dev A: US1 overflow fixes (T005–T011) + US3 consistency for same pages (T015–T021)
- Dev B: US2 BottomTabBar (T013) + US4 empty states (T023–T028)
- Merge and run Phase 7 together

---

## Notes

- [P] tasks touch different files — safe to run in parallel
- Each US phase delivers independently testable value
- Per-page batching (US1 + US3 + US4 for same file) is more efficient than strict story-by-story order when working alone
- Verify each page manually in Chrome DevTools at 320px and 375px after implementation
- Commit after each phase or logical group of pages
- Avoid: adding new components or abstractions — use existing `EmptyState`, `PageHeader`, `Card`, `Button`, `Table`
