# Implementation Plan: Mobile UI Declutter and Navigation Improvement

**Branch**: `012-mobile-ui-cleanup` | **Date**: 2026-06-24 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/012-mobile-ui-cleanup/spec.md`

## Summary

Reduce visual clutter on all five mobile screens (Dashboard, Trade/Transactions, Returns/Performance, Alerts, Bottom Navigation) by hiding secondary creation forms behind FABs, removing secondary metric selectors, and making the bottom navigation icon-only with a clear active-state indicator. All changes are confined to the React/TypeScript frontend (`frontend/src/`) and affect only the mobile layout (`md:hidden` / below the `md` breakpoint). The key constraint is that FIFO/LIFO must remain functional — it moves to a settings control rather than being deleted.

## Technical Context

**Language/Version**: TypeScript 5.6, React 18.3  
**Primary Dependencies**: Vite 5.4, Tailwind CSS 3.4, React Router 6.27, Recharts, Lucide React, Vitest 2.1  
**Storage**: N/A (frontend only; state in Zustand stores)  
**Testing**: Vitest + React Testing Library (tests in `frontend/tests/routes/` and `frontend/tests/`)  
**Target Platform**: Mobile web (phones), scoped to breakpoints below `md` (< 768px)  
**Project Type**: Web application — React SPA  
**Performance Goals**: No measurable regression; chart render must remain smooth  
**Constraints**: Desktop layout (`md:` and above) must be completely unaffected; existing `data-testid` attributes used by existing tests must not be removed without updating tests  
**Scale/Scope**: 5 route-level components + 1 layout component (`BottomTabBar`) + 1 new shared FAB component

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Automated Tests & Manual Verification | PASS — plan requires new/updated Vitest tests for every changed component | Tests must be committed with the feature |
| II. Compilation Integrity | PASS — TypeScript strict; `tsc --noEmit` must be green before merge | No new build steps introduced |
| III. Simplicity & YAGNI | PASS — changes are CSS/JSX restructuring; no new abstractions except a shared `FAB` component | FAB is shared across 3 routes — justified |
| IV. Specification-Driven Development | PASS — spec exists and is approved | |

No gate violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/012-mobile-ui-cleanup/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
frontend/
├── src/
│   ├── components/
│   │   ├── layout/
│   │   │   ├── BottomTabBar.tsx          # MODIFY: icon-only + active indicator
│   │   │   └── AppShell.tsx              # no change
│   │   └── ui/
│   │       └── FAB.tsx                   # NEW: floating action button component
│   ├── routes/
│   │   ├── DashboardRoute.tsx            # MODIFY: remove Add Symbol card + Allocation card from mobile
│   │   ├── TransactionsRoute.tsx         # MODIFY: hide Manual Entry + Import behind FAB on mobile
│   │   ├── PerformanceRoute.tsx          # MODIFY: remove FIFO/LIFO toggle; keep only window selector
│   │   └── AlertsRoute.tsx              # MODIFY: hide Create form behind FAB on mobile
│   └── stores/
│       └── uiStore.ts                    # MODIFY: add fifoLifoMethod state (moved from PerformanceRoute local state) — optional, see research
└── tests/
    └── routes/
        ├── DashboardRoute.test.tsx       # UPDATE
        ├── TransactionsRoute.test.tsx    # UPDATE
        ├── PerformanceRoute.test.tsx     # UPDATE
        └── AlertsRoute.test.tsx         # UPDATE
```

**Structure Decision**: Single frontend project. All changes are within `frontend/src/`. No new pages or routes. One new shared component `FAB.tsx` is warranted because three routes need the same floating action button pattern.

## Complexity Tracking

No constitution violations.
