# Data Model: Mobile UI Declutter and Navigation Improvement

**Feature**: 012-mobile-ui-cleanup  
**Date**: 2026-06-24

## Overview

This feature introduces no new backend data entities and no new API endpoints. Changes are confined to the React frontend. The "data" relevant to this feature is the component interface (props) for the one new shared component and the state shape for per-route form visibility.

---

## New Component: FAB

**File**: `frontend/src/components/ui/FAB.tsx`

**Props**:

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `onClick` | `() => void` | Yes | Called when the FAB is tapped |
| `label` | `string` | Yes | Accessible label (`aria-label`) for screen readers |
| `icon` | `React.ElementType` | No (default: `Plus`) | Lucide icon to render inside the button |
| `className` | `string` | No | Additional Tailwind classes for positioning override |

**Visual spec**:
- Fixed position: `fixed bottom-[calc(4rem+env(safe-area-inset-bottom)+1rem)] right-4`  
  (4rem = BottomTabBar height; 1rem = gap above the bar)
- Only visible on mobile: `md:hidden`
- Shape: `h-14 w-14 rounded-full`
- Colour: `bg-accent text-on-accent shadow-lg`
- Icon: `Plus` at 24px by default

---

## Per-Route Form Visibility State

Each of the three routes that hide a form behind a FAB manages a local boolean:

| Route | State variable | Initial value | FAB icon |
|-------|---------------|---------------|----------|
| `DashboardRoute` | `isSymbolSearchOpen: boolean` | `false` | `Plus` |
| `TransactionsRoute` | `isEntryOpen: boolean` | `false` | `Plus` |
| `AlertsRoute` | `isCreateOpen: boolean` | `false` | `Plus` |

When the boolean is `true`, the relevant card renders inside the page content (not a portal). This is local `useState` — no store change required.

---

## Alert Badge State

**File**: `frontend/src/components/layout/BottomTabBar.tsx` (or extracted hook)

A lightweight local fetch in `BottomTabBar` (or `AppShell`) maintains:

| Field | Type | Description |
|-------|------|-------------|
| `armedCount` | `number` | Count of `Alert` objects where `armed === true` |

This is fetched once on mount via `listAlerts()`. The badge renders only when `armedCount > 0`.

---

## BottomTabBar Nav Item Shape

The existing `items` array shape is extended to support a badge:

```
{
  to: string           // route path
  label: string        // accessible label (used for aria-label on NavLink; NOT rendered visibly)
  icon: React.ElementType
  badgeCount?: number  // renders a badge when > 0; only Alerts currently uses this
}
```

The `label` field is retained in the data but no longer rendered as visible text.
