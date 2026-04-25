# StockTracker Frontend

Client-side prototype for the StockTracker portfolio dashboard, watchlists, stock
analysis, and CSV import/export flows. All data is bundled or stored in
`localStorage`; no network calls at runtime.

For setup, scripts, architecture, and per-user-story verification steps, see
[`specs/001-frontend-prototype/quickstart.md`](../specs/001-frontend-prototype/quickstart.md).

## Quality gates

Run all three gates from the constitution in one command:

```sh
npm run verify
```

Individual gates:

```sh
npm run lint        # ESLint + Prettier
npm run typecheck   # tsc --noEmit
npm run test        # Vitest (jsdom)
npm run build       # Vite production build
```

Dev server:

```sh
npm run dev
```
