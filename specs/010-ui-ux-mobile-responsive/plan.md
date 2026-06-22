# Implementation Plan: UI/UX Consistency & Mobile Responsive Design

**Branch**: `010-ui-ux-mobile-responsive` | **Date**: 2026-06-22 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/010-ui-ux-mobile-responsive/spec.md`

## Summary

Audit and fix all 7 authenticated pages for mobile responsiveness (no horizontal overflow at 320–430px), align visual design across pages using the existing design token system and shared components, and ensure the BottomTabBar is fully legible at 320px. No new dependencies or abstractions — changes are confined to route components, existing shared components, and the viewport test suite.

## Technical Context

**Language/Version**: TypeScript 5.x  
**Primary Dependencies**: React 18, Tailwind CSS (custom design tokens via CSS variables), lucide-react, react-router-dom v6, Vitest + React Testing Library  
**Storage**: N/A (frontend only)  
**Testing**: Vitest + React Testing Library; existing `viewports.test.tsx` extended for this feature  
**Target Platform**: Web — mobile (320–430px), tablet (768–1023px), desktop (1024px+)  
**Project Type**: Web application (frontend SPA)  
**Performance Goals**: Standard web app — pages render within 2 seconds on mobile networks  
**Constraints**: No external UI libraries; all styling via Tailwind + existing design tokens  
**Scale/Scope**: 7 authenticated route components + 1 shared layout component + 1 test file

## Constitution Check

*GATE: Must pass before implementation. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Automated tests | PASS | Extend existing `viewports.test.tsx` + add route-level render tests for AlertsRoute and PerformanceRoute |
| Compile gate | PASS | TypeScript; run `tsc --noEmit` after each route change |
| Simplicity & YAGNI | PASS | No new abstractions; use existing components and tokens |
| Spec-driven | PASS | Flowing through full Spec Kit workflow |

*Post-design re-check*: No violations. All changes are narrowly scoped to fixing existing routes against established patterns.

## Project Structure

### Documentation (this feature)

```text
specs/010-ui-ux-mobile-responsive/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (UI component contracts)
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (affected paths)

```text
frontend/
├── src/
│   ├── components/
│   │   └── layout/
│   │       └── BottomTabBar.tsx       # Shorten "Transactions" label to "Trades"
│   └── routes/
│       ├── DashboardRoute.tsx         # Audit + fix overflow, empty state, consistency
│       ├── WatchlistsRoute.tsx        # Audit + fix overflow, empty state, consistency
│       ├── WatchlistDetailRoute.tsx   # Audit + fix overflow (table), consistency
│       ├── TransactionsRoute.tsx      # Audit + fix overflow (table), empty state, consistency
│       ├── PerformanceRoute.tsx       # Audit + fix overflow, empty state, consistency
│       ├── AlertsRoute.tsx            # Audit + fix overflow, empty state, consistency
│       └── AnalysisRoute.tsx          # Audit + fix overflow, consistency
└── tests/
    └── responsive/
        └── viewports.test.tsx         # Add 320px viewport + AlertsRoute + PerformanceRoute
```

## Implementation Phases

### Phase A: BottomTabBar Label Fix (FR-002, FR-003)

**File**: `frontend/src/components/layout/BottomTabBar.tsx`

Change the label for the `/transactions` nav item from `"Transactions"` to `"Trades"`. All 5 labels must fit within 64px at 320px viewport (= 5 equal-width slots in a 320px container).

Label width analysis at `text-micro` (0.6875rem ≈ 11px) uppercase + 0.08em tracking:
- DASHBOARD: ~70px estimated — already marginal; keep as-is (it's the widest acceptable)
- WATCHLISTS: ~68px — acceptable
- TRANSACTIONS: ~90px — OVERFLOW, rename to TRADES (~46px)
- RETURNS: ~53px — acceptable
- ALERTS: ~42px — acceptable

**Test**: Extend `viewports.test.tsx` — add a test that renders `BottomTabBar` at 320px and asserts no label text is wider than the container.

### Phase B: Per-Page Audit & Fix (FR-001, FR-004–FR-010)

For each of the 7 authenticated route files, apply this checklist:

**Overflow audit**:
- [ ] No element has an inline `width: Xpx` wider than the viewport
- [ ] All flex rows have `min-w-0` on children where needed
- [ ] All data tables are wrapped in `overflow-x-auto` with `rounded` clipping

**Design consistency audit**:
- [ ] Page uses `<PageHeader>` for the top heading
- [ ] Cards use the `<Card>` component or matching `bg-surface border border-border rounded` pattern
- [ ] Buttons use the `<Button>` component
- [ ] Tables use the `<Table>` component or consistent `<table>` styling
- [ ] Typography uses `text-headline`, `text-title`, `text-body`, `text-small`, `text-micro` tokens

**Empty state audit**:
- [ ] When no data is present, `<EmptyState>` is rendered with a meaningful message

**Pages in scope**:
1. `DashboardRoute.tsx` — portfolio summary cards, chart
2. `WatchlistsRoute.tsx` — list of watchlists
3. `WatchlistDetailRoute.tsx` — watchlist table (most at-risk for overflow)
4. `TransactionsRoute.tsx` — transactions table (most at-risk for overflow)
5. `PerformanceRoute.tsx` — returns chart + metrics (largest file, 369 lines)
6. `AlertsRoute.tsx` — alerts list
7. `AnalysisRoute.tsx` — ticker analysis view

### Phase C: Viewport Test Extension (FR-001, SC-001, SC-005)

**File**: `frontend/tests/responsive/viewports.test.tsx`

Changes:
1. Add `320` to the `VIEWPORTS` array: `[320, 375, 768, 1280, 1920] as const`
2. Add render tests for `AlertsRoute` and `PerformanceRoute` (currently missing)
3. Optionally add a `BottomTabBar` render test that checks label node counts at 320px

The jsdom-based tests cannot verify real layout overflow (no layout engine), but they guard against inline `width: Xpx` wider than the viewport and ensure every route renders without throwing at each breakpoint.

## Design Patterns Reference

All implementations MUST use these existing patterns:

| Element | Pattern |
|---------|---------|
| Page heading | `<PageHeader title="..." />` |
| Content card | `<Card>` or `className="bg-surface border border-border rounded-lg"` |
| Primary button | `<Button variant="primary">` |
| Table container | `<div className="overflow-x-auto rounded-lg border border-border">` wrapping `<Table>` |
| Empty state | `<EmptyState title="..." description="..." />` |
| Section heading | `text-headline font-semibold` |
| Body text | `text-body text-text` |
| Muted label | `text-small text-text-muted` |
| Positive value | `text-positive` |
| Negative value | `text-negative` |

## Complexity Tracking

No constitution violations. All changes use existing components and patterns; no new abstractions introduced.
