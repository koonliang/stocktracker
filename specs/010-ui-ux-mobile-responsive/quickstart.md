# Quickstart: UI/UX Consistency & Mobile Responsive Design

## Run the Frontend

```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:5173
```

## Test in Mobile Viewport

In Chrome DevTools → Toggle device toolbar → Select "iPhone SE" (375px) or set custom width to 320px.

Verify: no horizontal scrollbar, bottom nav labels visible, all content accessible.

## Run Tests

```bash
cd frontend
npm test                    # all tests
npm test -- viewports       # only viewport tests
npm run typecheck           # TypeScript check (required before PR)
npm run build               # production build check
```

## Verify Bottom Nav at 320px

1. Open Chrome DevTools
2. Set viewport to 320px wide
3. Check bottom nav: all 5 labels (Dashboard, Watchlists, Trades, Returns, Alerts) are fully visible with no overlap

## Verify Zero Horizontal Overflow

For each page:
1. Open Chrome DevTools → Console
2. Run: `document.querySelectorAll('*').forEach(el => { if (el.scrollWidth > window.innerWidth) console.warn(el); })`
3. No elements should log at 375px or 320px

## Pages to Verify

| Page | Route | Key Check |
|------|-------|-----------|
| Dashboard | `/` | Cards fit, chart responsive |
| Watchlists | `/watchlists` | List fits, empty state shows |
| Watchlist Detail | `/watchlists/:id` | Table scrolls horizontally within container |
| Transactions | `/transactions` | Table scrolls horizontally within container |
| Performance | `/performance` | Chart + metrics fit, empty state shows |
| Alerts | `/alerts` | Cards fit, empty state shows |
| Analysis | `/analysis/:ticker` | All panels fit |
