# Quickstart: Mobile UI Declutter and Navigation Improvement

**Feature**: 012-mobile-ui-cleanup  
**Date**: 2026-06-24

## Running the Frontend Dev Server

```bash
cd frontend
npm install       # if not already done
npm run dev       # starts Vite on http://localhost:5173
```

To test mobile layout: open Chrome/Firefox DevTools → toggle device toolbar → select a phone preset (e.g., iPhone SE 375px wide).

## Running Tests

```bash
cd frontend
npm test          # vitest run (one-shot)
npm run test:watch # vitest watch mode
```

Tests for affected routes live in:

```
frontend/tests/routes/DashboardRoute.test.tsx
frontend/tests/routes/TransactionsRoute.test.tsx
frontend/tests/routes/PerformanceRoute.test.tsx
frontend/tests/routes/AlertsRoute.test.tsx
```

## TypeScript Check

```bash
cd frontend
npx tsc --noEmit
```

Must pass with zero errors before marking the feature complete.

## Key Files for This Feature

| File | Purpose |
|------|---------|
| `src/components/ui/FAB.tsx` | New FAB button (create this file) |
| `src/components/layout/BottomTabBar.tsx` | Icon-only nav + active indicator + alert badge |
| `src/routes/DashboardRoute.tsx` | Hide Add Symbol + Allocation on mobile |
| `src/routes/TransactionsRoute.tsx` | Hide Manual Entry + Import on mobile; add FAB |
| `src/routes/PerformanceRoute.tsx` | Hide FIFO/LIFO toggle on mobile |
| `src/routes/AlertsRoute.tsx` | Hide Create form on mobile; add FAB |

## Verifying Mobile Behaviour Manually

1. Open app in browser with DevTools set to mobile (e.g., 375px width)
2. **Dashboard**: Confirm no "Add a symbol" card or "Allocation" card visible; portfolio total and holdings appear without scrolling; FAB "+" visible bottom-right
3. **Trade page**: Confirm no form cards on load; FAB present; tapping FAB reveals entry/import options
4. **Returns page**: Confirm chart is fully visible without scroll; no FIFO/LIFO buttons visible
5. **Alerts page**: Confirm no Create form on load; FAB present; tapping FAB reveals form
6. **Bottom nav**: Confirm no text labels; active tab has pill highlight; Alerts shows badge count when alerts exist
