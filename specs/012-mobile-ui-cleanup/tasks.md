# Tasks: Mobile UI Declutter and Navigation Improvement

**Input**: Design documents from `specs/012-mobile-ui-cleanup/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-components.md

**Tests**: Automated test tasks are created for feature behavior. Verification of whether the feature is complete is performed manually against the acceptance scenarios in spec.md using relevant test evidence.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US5)
- Exact file paths are included in all descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the one shared component (`FAB`) that three user stories depend on. US3 and US5 may begin immediately after this phase; US1, US2, US4 must wait for T002.

- [x] T001 Create `frontend/src/components/ui/FAB.tsx` — named export `FAB` per data-model.md props: `onClick`, `label`, `icon?`, `className?`; fixed-positioned, `md:hidden`, `data-testid="fab"`
- [x] T002 Create `frontend/tests/components/FAB.test.tsx` — render FAB, assert `data-testid="fab"` present; assert `aria-label` matches `label` prop; assert click handler fires

**Checkpoint**: FAB component created and tested — US1, US2, US4 may proceed

---

## Phase 2: User Story 5 — Icon-Only Bottom Navigation (Priority: P1) 🎯 MVP

**Goal**: Replace the icon + label navigation with an icon-only nav bar. Active tab gets a pill highlight. Alerts icon shows a badge count when armed alerts exist.

**Independent Test**: Open the app in a mobile viewport. Verify: no text labels visible in the bottom bar; active tab has a distinct background highlight; tapping each tab switches the active state; if at least one armed alert exists, a badge number appears on the Alerts icon.

### Tests for User Story 5 (REQUIRED) ⚠️

> Automated tests exercise the behavior. Manual verification against acceptance scenarios in spec.md (User Story 5) confirms completion.

- [x] T003 [P] [US5] Create `frontend/tests/components/BottomTabBar.test.tsx` — render `BottomTabBar` in a router context; assert no text labels rendered (`queryByText('Dashboard')` etc. return null); assert `data-testid="nav-active"` present on active link; assert badge renders when `armedCount > 0` with `data-testid="alerts-badge"`

### Implementation for User Story 5

- [x] T004 [US5] Update `frontend/src/components/layout/BottomTabBar.tsx` — remove `<span>{label}</span>` from each nav item; add `aria-label={label}` to each `<NavLink>`; rename `label` usage to be aria-only
- [x] T005 [US5] Update `frontend/src/components/layout/BottomTabBar.tsx` — add active pill indicator: wrap icon in a `<span>` with conditional `bg-accent/10 rounded-xl px-3 py-1` when `isActive`; add `data-testid="nav-active"` on the wrapping span when active
- [x] T006 [US5] Update `frontend/src/components/layout/BottomTabBar.tsx` — add alert badge: add a `useAlertBadge` hook inline (or extracted to `frontend/src/components/layout/useAlertBadge.ts`) that calls `listAlerts()` on mount and returns `armedCount` (count of alerts where `armed === true`); render badge `<span data-testid="alerts-badge">` on Alerts nav item when `armedCount > 0`

**Checkpoint**: Icon-only navigation with active pill indicator and alert badge is fully functional and independently testable

---

## Phase 3: User Story 1 — Dashboard At-A-Glance (Priority: P1) 🎯 MVP

**Goal**: Portfolio summary metrics visible above fold on mobile. "Add a symbol" card and Allocation card hidden on mobile; both accessible via a FAB or desktop view.

**Independent Test**: Open Dashboard on a mobile viewport. Verify: `SummaryTiles` and `HoldingsTable` are visible without scrolling; no "Add a symbol" card visible; no Allocation card visible; FAB "+" is visible; tapping FAB reveals `SymbolSearch` inline.

**Depends on**: T001 (FAB component)

### Tests for User Story 1 (REQUIRED) ⚠️

- [x] T007 [US1] Update `frontend/tests/routes/DashboardRoute.test.tsx` — add test: assert "Add a symbol" card not rendered by default on mobile (query by eyebrow text or `CardHeader` content); assert Allocation card not rendered; assert FAB (`data-testid="fab"`) present; assert clicking FAB reveals `SymbolSearch`

### Implementation for User Story 1

- [x] T008 [US1] Update `frontend/src/routes/DashboardRoute.tsx` — wrap the "Add a symbol" `<Card>` in `<div className="hidden sm:block">` so it is hidden on mobile and visible on desktop
- [x] T009 [US1] Update `frontend/src/routes/DashboardRoute.tsx` — wrap the Allocation `<Card>` in `<div className="hidden sm:block">` so it is hidden on mobile and visible on desktop
- [x] T010 [US1] Update `frontend/src/routes/DashboardRoute.tsx` — add `isSymbolSearchOpen` boolean state (default `false`); render `<FAB label="Add a symbol" onClick={() => setIsSymbolSearchOpen(true)} />` (only on mobile: `md:hidden` is built into FAB); when `isSymbolSearchOpen` is `true`, render `SymbolSearch` inside a compact `<Card className="sm:hidden">` at the top of the content area

**Checkpoint**: Dashboard mobile view shows portfolio summary above fold; Add Symbol and Allocation are hidden; FAB triggers Add Symbol search

---

## Phase 4: User Story 2 — Trade Page Streamlined Entry (Priority: P2)

**Goal**: Transactions page shows only the trade ledger on mobile load. Manual Entry and Import forms hidden; both revealed by tapping a FAB.

**Independent Test**: Open Transactions page on a mobile viewport. Verify: no Manual Entry card visible; no Import card visible; ledger visible above fold; FAB present; tapping FAB reveals both form cards.

**Depends on**: T001 (FAB component)

### Tests for User Story 2 (REQUIRED) ⚠️

- [x] T011 [US2] Update `frontend/tests/routes/TransactionsRoute.test.tsx` — add test: assert Manual Entry card not rendered by default; assert Import card not rendered by default; assert FAB present; assert clicking FAB reveals Manual Entry and Import cards

### Implementation for User Story 2

- [x] T012 [US2] Update `frontend/src/routes/TransactionsRoute.tsx` — add `isEntryOpen` boolean state (default `false`)
- [x] T013 [US2] Update `frontend/src/routes/TransactionsRoute.tsx` — wrap the "Manual entry" `<Card>` in `<div className={isEntryOpen ? 'block' : 'hidden sm:block'}>` so it is hidden on mobile until `isEntryOpen` is true; on desktop it is always visible
- [x] T014 [US2] Update `frontend/src/routes/TransactionsRoute.tsx` — wrap the Import/Preview `<Card>` in `<div className={isEntryOpen ? 'block' : 'hidden sm:block'}>` using the same pattern as T013
- [x] T015 [US2] Update `frontend/src/routes/TransactionsRoute.tsx` — add `<FAB label="Record a transaction" onClick={() => setIsEntryOpen(true)} />` after the ledger card; on desktop the FAB is already hidden via `md:hidden` in the FAB component

**Checkpoint**: Transactions mobile view shows only the ledger on load; FAB reveals entry forms

---

## Phase 5: User Story 3 — Returns Chart Without Scrolling (Priority: P2)

**Goal**: On the Returns/Performance page, the chart renders fully visible without any vertical scrolling on mobile. The FIFO/LIFO toggle is hidden on mobile (desktop unchanged).

**Independent Test**: Open Performance page on a mobile viewport. Verify: chart is fully visible without scrolling; no FIFO/LIFO buttons visible; window selector (1M, 3M, 6M, 1Y, YTD, ALL) still visible; key metrics (Realized P&L, Unrealized P&L, TWR) visible.

**No dependencies on FAB** — can run in parallel with US1 and US2.

### Tests for User Story 3 (REQUIRED) ⚠️

- [x] T016 [P] [US3] Update `frontend/tests/routes/PerformanceRoute.test.tsx` — update existing `lot-method-toggle` test to assert the toggle has `hidden sm:flex` class (not `flex`); add test asserting window selector remains visible; assert chart container (`data-testid="return-chart"`) is present

### Implementation for User Story 3

- [x] T017 [P] [US3] Update `frontend/src/routes/PerformanceRoute.tsx` — change the FIFO/LIFO toggle container from `<div className="flex gap-2" data-testid="lot-method-toggle">` to `<div className="hidden sm:flex gap-2" data-testid="lot-method-toggle">` so it is hidden on mobile and visible on desktop

**Checkpoint**: Performance page hides FIFO/LIFO on mobile; chart remains fully visible

---

## Phase 6: User Story 4 — Alerts Page with Hidden Create Form (Priority: P3)

**Goal**: Alerts page shows only the active alerts list on mobile load. Create Alert form hidden; revealed by tapping a FAB.

**Independent Test**: Open Alerts page on a mobile viewport. Verify: no Create Alert form visible; alerts list visible; FAB present; tapping FAB reveals the Create Alert form.

**Depends on**: T001 (FAB component)

### Tests for User Story 4 (REQUIRED) ⚠️

- [x] T018 [US4] Update `frontend/tests/routes/AlertsRoute.test.tsx` — add test: assert Create alert card not rendered by default on mobile; assert FAB present; assert clicking FAB reveals the Create alert form (`data-testid="alert-form"`)

### Implementation for User Story 4

- [x] T019 [US4] Update `frontend/src/routes/AlertsRoute.tsx` — add `isCreateOpen` boolean state (default `false`)
- [x] T020 [US4] Update `frontend/src/routes/AlertsRoute.tsx` — wrap the Create `<Card>` (the one containing `data-testid="alert-form"`) in `<div className={isCreateOpen ? 'block' : 'hidden sm:block'}>` so it is hidden on mobile until `isCreateOpen` is true; desktop always shows it
- [x] T021 [US4] Update `frontend/src/routes/AlertsRoute.tsx` — add `<FAB label="Create alert" onClick={() => setIsCreateOpen(true)} />`; when `isCreateOpen` becomes true (after form submit or cancel), reset `isCreateOpen` to `false` by calling `setIsCreateOpen(false)` in `resetForm()`

**Checkpoint**: Alerts mobile view shows only the alerts list on load; FAB reveals the create form; form resets when closed

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verify the full experience end-to-end, fix any visual regressions, and confirm desktop layouts are unchanged.

- [x] T022 [P] Run `npx tsc --noEmit` in `frontend/` and fix any TypeScript errors introduced in T001–T021
- [x] T023 [P] Run `npm test` in `frontend/` and confirm all tests pass (existing + new); fix any broken existing tests caused by the changes
- [x] T024 Manually verify desktop layout is unchanged for all five pages (Dashboard, Transactions, Performance, Alerts, BottomTabBar) at viewport width ≥ 768px
- [x] T025 Manually verify mobile layout for all five pages per quickstart.md verification checklist at viewport width 375px
- [x] T026 [P] Check `frontend/tests/responsive/viewports.test.tsx` — update or add viewport assertions if the test covers BottomTabBar label rendering

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (US5 — BottomTabBar)**: No dependency on FAB — can start in parallel with Phase 1
- **Phase 3 (US1 — Dashboard)**: Depends on T001 (FAB) from Phase 1
- **Phase 4 (US2 — Transactions)**: Depends on T001 (FAB) from Phase 1; independent of US1
- **Phase 5 (US3 — Performance)**: No dependency on FAB — can start immediately after Phase 1 (even in parallel with Phase 1 for T017)
- **Phase 6 (US4 — Alerts)**: Depends on T001 (FAB) from Phase 1; independent of US1 and US2
- **Phase 7 (Polish)**: Depends on all previous phases

### User Story Dependencies

- **US5 (P1 — BottomTabBar)**: No dependencies — can start immediately
- **US1 (P1 — Dashboard)**: Depends on T001 (FAB)
- **US2 (P2 — Transactions)**: Depends on T001 (FAB); independent of US1
- **US3 (P2 — Performance)**: No dependencies — can run fully in parallel
- **US4 (P3 — Alerts)**: Depends on T001 (FAB); independent of US1/US2

### Within Each User Story

- Tests created alongside implementation (not strictly before)
- State changes before JSX conditional rendering
- JSX wrapping before FAB addition

### Parallel Opportunities

- T003 (US5 tests) and T001 (FAB) can be written in parallel
- T016 + T017 (US3) can run in parallel with US1 and US2 work
- T007 (US1 test) and T008–T010 (US1 implementation) can proceed together
- T011 (US2 test) and T012–T015 (US2 implementation) can proceed together
- T022 and T023 (Polish) can run in parallel

---

## Parallel Example: US1 + US3 simultaneously

```
# Both can run in parallel after T001 (FAB) is done:
US1: T007 (test) + T008 (hide Add Symbol) + T009 (hide Allocation) + T010 (FAB wiring)
US3: T016 (test update) + T017 (hide FIFO/LIFO toggle) — independent of FAB
```

---

## Implementation Strategy

### MVP First (US5 + US1 — the two P1 stories)

1. Complete Phase 1: Create FAB (T001–T002)
2. Complete Phase 2: BottomTabBar (T003–T006)
3. Complete Phase 3: Dashboard (T007–T010)
4. **STOP and VALIDATE**: Run `npm test` and manually verify Dashboard + BottomTabBar on mobile
5. Demo if ready — US5 + US1 deliver the highest-impact visual changes

### Incremental Delivery

1. Phase 1 (FAB) + Phase 2 (US5 nav) → Clean navigation on all pages
2. Phase 3 (US1 dashboard) → Dashboard decluttered
3. Phase 4 (US2 transactions) → Trade page decluttered
4. Phase 5 (US3 returns) → Returns chart without scroll
5. Phase 6 (US4 alerts) → Alerts page decluttered
6. Phase 7 (Polish) → Full regression pass

---

## Notes

- `[P]` tasks = different files, no dependencies on incomplete tasks in the same phase
- Desktop layout at `sm:` / `md:` breakpoints MUST remain unchanged throughout — verify after every phase
- `hidden sm:block` pattern: element is `display: none` on mobile, `display: block` on desktop
- `hidden sm:flex` pattern: element is `display: none` on mobile, `display: flex` on desktop
- FAB `md:hidden` is built into the component — no extra wrapper needed in route files
- For T006 (alert badge): `listAlerts()` import is already available in `frontend/src/api/alertsApi.ts`
- Verify each story manually against its acceptance scenarios in spec.md before marking complete
