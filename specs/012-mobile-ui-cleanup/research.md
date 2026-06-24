# Research: Mobile UI Declutter and Navigation Improvement

**Feature**: 012-mobile-ui-cleanup  
**Date**: 2026-06-24  
**Status**: Complete — all decisions resolved

## Decision 1: FAB (Floating Action Button) Pattern

**Decision**: Implement `FAB.tsx` as a single shared component in `frontend/src/components/ui/`. It renders a fixed-position circular button at the bottom-right corner, above the `BottomTabBar`. On click it invokes an `onClick` callback provided by the parent. Each route manages its own `isFormOpen` boolean state. The form/sheet slides in as a full-width panel at the bottom of the screen (bottom sheet pattern using a `div` with `fixed inset-x-0 bottom-[env(safe-area-inset-bottom)+64px]` or similar) or simply renders inline below the FAB anchor — whichever approach avoids new dependencies. Simplest first: toggle inline visibility via `hidden` / show transition, no portal or animation library needed.

**Rationale**: Three routes need the same "hidden form revealed by +" interaction (Dashboard Add Symbol, Transactions, Alerts). A shared `FAB` button component is the minimal reuse boundary; the form reveal logic stays local to each route since each form is different.

**Alternatives considered**:
- Bottom sheet with backdrop overlay: richer UX but requires either a portal or extra z-index handling — deferred to a future enhancement.
- Headless UI / Radix dialog: overkill for a simple toggle; not a current dependency.

---

## Decision 2: FIFO/LIFO Disposition

**Decision**: Hide the FIFO/LIFO toggle on mobile only (add `hidden sm:flex` to the toggle container). The `method` state remains local to `PerformanceRoute`, defaulting to `'fifo'`. No new settings page or uiStore changes are needed. Desktop users continue to see and use the toggle unchanged.

**Rationale**: The spec assumption states FIFO/LIFO should "move to app settings rather than be removed entirely". The simplest implementation that satisfies this for mobile is to make it desktop-only via a Tailwind breakpoint class. There is no settings page in the current app; introducing one would violate YAGNI. If a settings page is added in a future feature, the FIFO/LIFO preference can be surfaced there.

**Alternatives considered**:
- Add FIFO/LIFO to uiStore with persistence: adds complexity without a current concrete need from a non-PerformanceRoute consumer.
- Settings icon in TopBar opening a modal: premature abstraction for a single preference.

---

## Decision 3: Dashboard — Allocation Section Disposition

**Decision**: Hide the Allocation `<Card>` on mobile using `hidden sm:block` wrapper. On desktop, keep it in place between `SummaryTiles` and the Holdings table. Do not move it to another page. Add a "See allocation" text link or icon at the bottom of the Holdings card on mobile pointing to the existing desktop view (no new route needed — users can rotate or use a desktop breakpoint).

**Rationale**: The spec says "remove from main dashboard view; accessible elsewhere". The simplest "elsewhere" is desktop view, which is already accessible to mobile users via responsive layout when they rotate. Adding a full page for Allocation is future scope. The goal is decluttering, not deleting the feature.

**Alternatives considered**:
- Move Allocation to AnalysisRoute: doesn't fit the per-instrument purpose of that route.
- Add a `/allocation` route: out of scope for this feature.

---

## Decision 4: Dashboard — "Add a Symbol" Card Disposition

**Decision**: On mobile, hide the "Add a symbol" card (`hidden sm:block`). Add a `FAB` with a `+` icon on the Dashboard that toggles the `SymbolSearch` component inline (renders below the `SummaryTiles` / at the top of the content area in a compact card). On desktop, the card remains visible as today.

**Rationale**: Symbol search is a secondary action on the Dashboard — most sessions are read-only reviews. The FAB preserves discoverability without occupying prime screen real estate.

---

## Decision 5: Bottom Navigation Active State

**Decision**: Remove the `<span>{label}</span>` text label from each nav item. Replace with a CSS indicator: use a 2px top border or a filled-icon approach. Specifically, use `fill-current` on the icon and a different icon variant for the active state, OR simply change the icon fill. Since Lucide icons don't have filled variants by default, use `strokeWidth` (thinner for inactive, thicker for active) combined with the existing `text-accent` color. Additionally, add `bg-accent/10 rounded-xl` as a pill indicator on the active icon container.

**Rationale**: The existing implementation already uses `text-accent` for the active item, but the text label makes it visually busy. Removing labels frees vertical space and makes the tab height smaller. The pill indicator is a widely adopted mobile pattern (iOS, Android) that gives a clear active signal without text.

**Alternatives considered**:
- Filled Lucide icon variant: not available in the current Lucide React version without importing alternative icon sets.
- Dot/bar indicator below icon: also valid, but pill wrapping the icon is more modern.

---

## Decision 6: Alert Badge Count

**Decision**: The badge count on the Alerts nav icon shows the count of alerts where `armed === true` (alerts actively watching for threshold crossings). The count is fetched via `listAlerts()` (already called in `AlertsRoute`). To show the badge in `BottomTabBar`, the alerts list must be accessible outside the route. The simplest approach: add `armedAlertsCount` to the existing Zustand `alertsStore` or derive it from a new lightweight store. However, to minimize scope: add a `useAlertBadge` hook that calls `listAlerts()` once on mount (in `AppShell` or `BottomTabBar`) and exposes the count. The badge only renders when count > 0.

**Rationale**: The badge is a navigation-level concern, not a route-level concern, so the data must be available in `BottomTabBar`. A minimal hook fetch avoids a global store change. If a full alertsStore is added in future, this can be replaced.

**Alternatives considered**:
- Global Zustand alertsStore: cleaner long-term, but adds more scope than this feature requires.
- Server-Sent Events / WebSocket push for live badge: out of scope.

---

## Summary of Affected Files

| File | Change Type | Change |
|------|-------------|--------|
| `components/ui/FAB.tsx` | NEW | Shared FAB button component |
| `components/layout/BottomTabBar.tsx` | MODIFY | Remove labels; add pill active indicator; add alert badge |
| `routes/DashboardRoute.tsx` | MODIFY | Hide Add Symbol card + Allocation card on mobile; add FAB |
| `routes/TransactionsRoute.tsx` | MODIFY | Hide Manual Entry + Import cards on mobile; add FAB revealing them |
| `routes/PerformanceRoute.tsx` | MODIFY | Hide FIFO/LIFO toggle on mobile (sm:flex → hidden sm:flex) |
| `routes/AlertsRoute.tsx` | MODIFY | Hide Create card on mobile; add FAB to toggle form visibility |
| `tests/routes/DashboardRoute.test.tsx` | UPDATE | Test FAB + hidden sections |
| `tests/routes/TransactionsRoute.test.tsx` | UPDATE | Test FAB reveal pattern |
| `tests/routes/PerformanceRoute.test.tsx` | UPDATE | Update lot-method-toggle test (hidden on mobile) |
| `tests/routes/AlertsRoute.test.tsx` | UPDATE | Test FAB reveal pattern |
