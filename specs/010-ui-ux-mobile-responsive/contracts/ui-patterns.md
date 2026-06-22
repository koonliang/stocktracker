# UI Pattern Contracts

These patterns define how shared UI elements MUST be implemented across all authenticated pages.

## Table Scroll Container

Any data table that may exceed the viewport width MUST be wrapped in a scroll container:

```tsx
<div className="overflow-x-auto rounded-lg border border-border">
  <Table>...</Table>
</div>
```

**Rules**:
- The wrapper `div` clips the horizontal overflow within a rounded border
- The parent flex/grid element MUST have `min-w-0` to prevent flex blowout
- Do NOT apply `overflow-x-auto` to the page `<main>` — only to the table container

## EmptyState Usage

Every page that displays a list, table, or data collection MUST render an `EmptyState` when that collection is empty:

```tsx
import { EmptyState } from '@/components/ui/EmptyState';

// When collection is empty:
<EmptyState
  title="No [items] yet"
  description="[Action prompt, e.g., Add your first watchlist to get started]"
/>
```

**Rules**:
- Use the shared `<EmptyState>` component — do not create inline empty state markup
- Title follows format: "No [noun] yet"
- Description provides a next-action hint

## PageHeader Usage

Every authenticated page MUST start with a `<PageHeader>` for its top-level heading:

```tsx
import { PageHeader } from '@/components/layout/PageHeader';

<PageHeader title="Dashboard" />
// or with subtitle:
<PageHeader title="Watchlists" subtitle="3 watchlists" />
```

**Rules**:
- No page renders a raw `<h1>` outside of `PageHeader`
- The title is the page name as shown in the bottom nav
- Subtitle is optional and used for aggregate counts

## Responsive Flex/Grid Rules

To prevent horizontal overflow in flex rows:
- Every flex child that contains text MUST have `min-w-0` and `truncate` or `overflow-hidden` if it may exceed its allocated width
- Grid columns on mobile MUST use `grid-cols-1` as the base and scale up: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`
- Never use fixed `w-[Xpx]` on elements that participate in a responsive layout — use `w-full`, `flex-1`, or `max-w-*` instead
