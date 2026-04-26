# Quickstart: StockTracker Full-Stack Integration

Use this workflow to start the integrated frontend, backend, and MySQL stack
locally and verify the constitutional quality gates for both codebases.

## Prerequisites

- Docker Desktop or Docker Engine with Compose v2
- Optional for non-containerized debugging:
  - Node.js 20+
  - npm 10+
  - Java 21

## Start the local stack

From the repository root:

```bash
docker compose up --build
```

Expected local endpoints:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`
- Backend health: `http://localhost:8080/q/health`
- MySQL: `localhost:3306`

## Stop or reset the local stack

Stop services but keep database data:

```bash
docker compose down
```

Stop services and reset the local database volume:

```bash
docker compose down -v
```

## Daily development notes

- The Compose workflow is the default integration path for this feature.
- Restarting the stack with `docker compose down` then `docker compose up`
  should preserve saved data.
- Reset with `down -v` only when you intentionally want a clean database.

## Quality gates

Per `.specify/memory/constitution.md`, all test, lint, and compile gates must
pass before implementation is considered complete.

### Frontend

```bash
cd frontend
npm test
npm run lint
npm run typecheck
npm run build
```

### Backend

```bash
cd backend
./mvnw test
./mvnw spotless:check
./mvnw -DskipTests compile
```

### Local stack smoke check

After `docker compose up --build` reports healthy containers:

```bash
./scripts/verify-local-stack.sh
```

## End-to-end verification by user story

### US1: Persist portfolio data

1. Start the stack with Docker Compose.
2. Open the dashboard and confirm seeded portfolio content is visible.
3. Import or add transactions and verify dashboard values change.
4. Restart the stack without `-v`.
5. Reopen the dashboard and verify the saved portfolio state remains.

### US2: Manage watchlists and analysis

1. Create a watchlist and add several supported tickers.
2. Reload the browser and verify the same watchlist remains.
3. Open analysis for a watchlist ticker and confirm price history, stats, and
   held-position summary behavior.

### US3: Import and export transactions

1. On `/transactions`, upload a mixed-validity CSV.
2. Confirm invalid rows are flagged during preview.
3. Commit the valid rows and verify the dashboard updates.
4. Export transactions and verify the file matches the canonical schema.

### US4: Local full-stack workflow

1. Run `docker compose up --build` from a clean checkout.
2. Reach a usable frontend and backend without manual service assembly.
3. Save data, restart the stack, and verify persistence remains intact.

## Troubleshooting

- Frontend loads but shows API errors: confirm backend container is healthy and
  the frontend API base URL targets `http://localhost:8080/api`.
- Backend starts but fails persistence checks: verify MySQL is reachable and the
  schema migrations completed.
- Backend Maven commands fail before compilation: confirm the machine can access
  Maven Central and that the local Maven cache is writable.
- Data unexpectedly reset: confirm you did not run `docker compose down -v`.
