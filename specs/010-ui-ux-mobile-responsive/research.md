# Research: UI/UX Consistency & Mobile Responsive Design

## 1. Current Bottom Navigation Label Width Risk

**Decision**: Shorten "Transactions" label to "Trades" in the BottomTabBar  
**Rationale**: At 320px with 5 equal nav items (64px each), "TRANSACTIONS" in `text-micro uppercase tracking-[0.08em]` (≈11px font + 8% letter spacing) renders wider than 64px and will clip or wrap. "TRADES" fits comfortably. All other labels (Dashboard=9, Watchlists=10, Returns=7, Alerts=6 chars) fit at 64px width.  
**Alternatives considered**: Icon-only at ≤360px via a CSS breakpoint — rejected because it adds complexity for a minor viewport edge case, and a shorter label is simpler (Principle III). "Trans" was considered but "Trades" is a clearer financial term.

## 2. Page-Level Horizontal Overflow Root Causes

**Decision**: Use `overflow-x-hidden` on the AppShell `<main>` element (already present) plus audit each route for fixed-pixel widths and wide tables  
**Rationale**: The AppShell already applies `overflow-x-hidden` to `<main>`. Residual overflow typically comes from: (a) fixed-pixel `width` on elements wider than viewport, (b) tables without `min-width: 0` or horizontal scroll containers, (c) wide flex rows without `flex-wrap` or `min-w-0`. Each route must be individually audited.  
**Alternatives considered**: CSS `overflow-x: hidden` at a higher level — rejected because it can hide bugs rather than fix them.

## 3. Data Table Mobile Strategy

**Decision**: Wrap data tables in `overflow-x-auto` scroll containers with `min-w-0` guards on parent flex/grid elements  
**Rationale**: Financial data tables (Transactions, Watchlist Detail) have inherent column density that cannot be eliminated. Horizontal scroll within a contained region is acceptable per spec (FR-006) and is a well-established pattern. Card-per-row stacking was considered but would require significant data restructuring.  
**Alternatives considered**: Card-per-row for mobile — rejected as over-engineering for this scope (Principle III).

## 4. Visual Design Consistency Approach

**Decision**: Audit all 7 authenticated pages against a shared pattern checklist (PageHeader, Card, Table, EmptyState, Button) and align deviants to the established pattern  
**Rationale**: The project already has a design token system (`tokens.css`, `tailwind.config.ts`) and shared components (`Card`, `Button`, `Table`, `EmptyState`, `PageHeader`). Consistency work means ensuring all pages *use* these components rather than inventing ad-hoc alternatives.  
**Alternatives considered**: Introducing a new design system / component library — rejected; existing tokens and components are sufficient and adding more complexity violates YAGNI.

## 5. Empty State Coverage

**Decision**: Ensure all 7 authenticated pages have a `<EmptyState>` component rendered when no data is present  
**Rationale**: The `EmptyState` component already exists at `frontend/src/components/ui/EmptyState.tsx`. Pages that show blank content when data is absent feel broken. Each page should use this existing component with a page-appropriate message.  
**Alternatives considered**: Custom per-page empty state markup — rejected; use the shared component.

## 6. Viewport Test Coverage Gap

**Decision**: Extend `viewports.test.tsx` to include AlertsRoute, PerformanceRoute, and add 320px to the VIEWPORTS array  
**Rationale**: Existing tests cover 5 routes at [375, 768, 1280, 1920]px. AlertsRoute and PerformanceRoute are missing, and 320px (the minimum viewport in the spec) is not included. The test infrastructure is already in place — extending it is straightforward.  
**Alternatives considered**: E2E Playwright tests for real layout testing — out of scope for this feature; the existing jsdom approach is consistent with the project's current test strategy.

## 7. BottomTabBar Active State

**Decision**: Retain existing active state treatment (accent color on icon + label)  
**Rationale**: The existing `BottomTabBar` already uses `text-accent` for active items and `text-text-muted` for inactive items. This is a clear visual distinction. No change needed.  
**Alternatives considered**: Adding an indicator dot or underline bar — not required; existing treatment is sufficient.
