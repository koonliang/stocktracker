# UI Component Model: UI/UX Consistency & Mobile Responsive Design

This feature is UI-only — no new data entities, no schema changes. This file documents the **UI component contracts** that all 7 authenticated pages must conform to.

## Component Usage Map

| Page | PageHeader | Card | Table (wrapped) | EmptyState | Notes |
|------|-----------|------|-----------------|------------|-------|
| Dashboard | required | required | — | required (no positions) | Summary metrics + chart |
| Watchlists | required | required | — | required (no watchlists) | List of watchlist cards |
| WatchlistDetail | required | — | required | required (empty watchlist) | Ticker table per watchlist |
| Transactions | required | — | required | required (no transactions) | All transactions table |
| Performance | required | required | — | required (no positions) | Returns metrics + chart |
| Alerts | required | required | — | required (no alerts) | Alert cards list |
| Analysis | required | required | — | — | Always has ticker context |

## Breakpoint Layout Contracts

Each authenticated page MUST render without horizontal overflow at these breakpoints:

| Breakpoint | Width Range | Layout Expectation |
|-----------|------------|-------------------|
| Mobile S | 320–374px | Single column, table in scroll container |
| Mobile L | 375–767px | Single column, wider table visible |
| Tablet | 768–1023px | Two columns where applicable |
| Desktop | 1024–1279px | Full layout with sidebar |
| Wide | 1280px+ | Max-width container centered |

## BottomTabBar Label Contract

The 5 navigation labels must fit within their allocated slot width at every supported viewport:

| Route | Icon | Label | Max width at 320px |
|-------|------|-------|-------------------|
| / | LayoutDashboard | Dashboard | 64px |
| /watchlists | ListChecks | Watchlists | 64px |
| /transactions | ArrowLeftRight | Trades | 64px |
| /performance | ChartNoAxesCombined | Returns | 64px |
| /alerts | Bell | Alerts | 64px |

Note: "Transactions" renamed to "Trades" — see `research.md` for rationale.

## Typography Scale Contract

All pages use these Tailwind token classes, no ad-hoc font sizes:

| Use | Token | Size |
|-----|-------|------|
| Page title | `text-headline` | 1.375rem |
| Section heading | `text-title` | 1.0625rem |
| Body content | `text-body` | 0.9375rem |
| Supporting label | `text-small` | 0.8125rem |
| Nav / micro label | `text-micro` | 0.6875rem |

## Color Semantic Contract

All pages use these semantic tokens, no raw hex or Tailwind palette colors:

| Semantic | Token | Usage |
|---------|-------|-------|
| Page background | `bg-bg` | Root background |
| Card/panel | `bg-surface` | Content surfaces |
| Elevated panel | `bg-surface-alt` | Hover rows, nested surfaces |
| Dividers | `border-border` | Default borders |
| Strong dividers | `border-border-strong` | Table headers, section dividers |
| Primary text | `text-text` | Main content |
| Secondary text | `text-text-muted` | Labels, captions |
| Tertiary text | `text-text-subtle` | Placeholder, disabled |
| Interactive | `text-accent` / `bg-accent` | Primary actions, active nav |
| Gain | `text-positive` | Positive P&L |
| Loss | `text-negative` | Negative P&L |
