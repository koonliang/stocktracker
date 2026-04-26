# StockTracker Frontend

React/Vite frontend for the integrated StockTracker product. The dashboard,
watchlists, analysis view, and CSV import/export flows now call the Quarkus
backend instead of storing portfolio state in `localStorage`.

For the full-stack workflow and per-story verification steps, see
[`specs/002-connect-frontend-backend/quickstart.md`](../specs/002-connect-frontend-backend/quickstart.md).

## Local development

Run the full product from the repository root:

```sh
docker compose up --build
```

The frontend is served at `http://localhost:5173` and targets
`VITE_API_BASE_URL`, which defaults to `http://localhost:8080/api` in the local
Compose stack.

## Quality gates

Run all frontend gates:

```sh
npm test
npm run lint
npm run typecheck
npm run build
```

Frontend-only dev server:

```sh
npm run dev
```
