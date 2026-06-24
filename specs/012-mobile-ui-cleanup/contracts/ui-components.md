# UI Component Contracts: Mobile UI Declutter

**Feature**: 012-mobile-ui-cleanup  
**Date**: 2026-06-24

## FAB Component Contract

**Path**: `frontend/src/components/ui/FAB.tsx`  
**Export**: named export `FAB`

```typescript
interface FABProps {
  onClick: () => void;
  label: string;           // aria-label for accessibility
  icon?: React.ElementType; // defaults to Plus from lucide-react
  className?: string;
}
```

**Rendering contract**:
- MUST render as `<button type="button">`
- MUST be `md:hidden` (invisible at or above md breakpoint)
- MUST be fixed-positioned above the BottomTabBar
- MUST carry `aria-label={label}`
- MUST have `data-testid="fab"`

---

## BottomTabBar Contract (updated)

**Path**: `frontend/src/components/layout/BottomTabBar.tsx`

**Rendering contract** (changed from current):
- MUST NOT render visible text labels for nav items
- MUST apply a pill/highlight indicator to the active nav item (e.g., `bg-accent/10 rounded-xl`)
- MUST render a numeric badge on the Alerts icon when `armedCount > 0`
- Badge: `data-testid="alerts-badge"`, value = `armedCount`
- Active icon: `data-testid="nav-active"` on the active `<NavLink>`
- All nav items MUST retain their `aria-label` equal to the previous label text

---

## Responsive Visibility Contract

All sections hidden on mobile MUST follow this pattern:

| Section | Mobile (default) | Desktop (`sm:` / `md:`) |
|---------|-----------------|------------------------|
| Dashboard — Add Symbol card | `hidden` | `block` (via `sm:block` wrapper) |
| Dashboard — Allocation card | `hidden` | `block` (via `sm:block` wrapper) |
| Transactions — Manual Entry card | `hidden` (when `!isEntryOpen`) | `block` |
| Transactions — Import card | `hidden` (when `!isEntryOpen`) | `block` |
| Performance — FIFO/LIFO toggle | `hidden` | `flex` (via `hidden sm:flex`) |
| Alerts — Create card | `hidden` (when `!isCreateOpen`) | `block` |

The desktop layout (`sm:` and above) MUST remain pixel-identical to the pre-feature state.
