# StockTracker

StockTracker is a full-stack stock portfolio tracker with a React frontend, a
Quarkus backend, and a MySQL database.

The app covers four main flows:

- portfolio dashboard and derived holdings
- watchlist management
- stock analysis with seeded price/stat data
- CSV import/export for transactions

## How It Works

At a high level:

- `frontend/` is a React 18 + Vite single-page app
- `backend/` is a Java 21 + Quarkus REST API
- MySQL stores transactions, watchlists, instruments, price history, and stats

The frontend calls the backend over HTTP. The backend owns validation,
persistence, and derived portfolio calculations. Reference market data is
seeded from JSON files in `backend/src/main/resources/seed/`.

On startup, the backend:

- runs Flyway migration `V1__init_schema.sql`
- seeds instrument, price-bar, and stat reference data when the target tables
  are empty
- seeds demo transactions when `STOCKTRACKER_DEV_BOOTSTRAP_ENABLED=true`

## Repository Layout

```text
.
├── backend/              Quarkus API, domain model, Flyway schema, seed data
├── frontend/             React app, routes, stores, API client, tests
├── scripts/              Stack verification helpers
├── specs/                Feature specs, plans, contracts, and quickstarts
└── docker-compose.yml    Default local full-stack workflow
```

Important backend packages:

- `api/` REST endpoints
- `service/` business logic and derived portfolio calculations
- `persistence/` Panache repositories
- `domain/` JPA entities
- `bootstrap/` startup seed/bootstrap logic

Important frontend areas:

- `src/routes/` route-level screens
- `src/features/` product-specific UI
- `src/stores/` Zustand state
- `src/api/` backend API wrappers
- `tests/` Vitest and Testing Library coverage

## Local Development

### Preferred: Docker Compose

From the repo root:

```bash
docker compose up --build
```

Endpoints:

- frontend: `http://localhost:5173`
- backend API: `http://localhost:8080/api`
- backend health: `http://localhost:8080/q/health`
- MySQL: `localhost:13306`

Stop services:

```bash
docker compose down
```

Reset the local database volume:

```bash
docker compose down -v
```

Smoke-check the stack after startup:

```bash
./scripts/verify-local-stack.sh
```

### Run Services Outside Docker

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend uses `VITE_API_BASE_URL`, which is `http://localhost:8080/api` in
[`frontend/.env.example`](./frontend/.env.example).

Backend:

```bash
cd backend
./mvnw quarkus:dev
```

Default backend DB settings come from
[`backend/src/main/resources/application.properties`](./backend/src/main/resources/application.properties):

- JDBC URL: `jdbc:mysql://localhost:3306/stocktracker_dev`
- username: `stocktracker`
- password: `stocktracker`

You can override them with `QUARKUS_DATASOURCE_JDBC_URL`,
`QUARKUS_DATASOURCE_USERNAME`, and `QUARKUS_DATASOURCE_PASSWORD`.

## Running the Built Backend Jar

This project uses Quarkus `fast-jar` packaging. Run the backend from
`backend/` with:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

Keep the full `target/quarkus-app/` directory together. `quarkus-run.jar`
depends on the other files in that folder.

## Testing and Quality Gates

Frontend:

```bash
cd frontend
npm test
npm run lint
npm run typecheck
npm run build
```

Backend:

```bash
cd backend
./mvnw test
./mvnw spotless:check
./mvnw -DskipTests compile
```

## Data Model Notes

The mutable user-owned tables use `BIGINT` primary keys:

- `portfolio_transaction`
- `watchlist`
- `watchlist_item`

The backend keeps holdings and summary values as derived projections rather than
persisted tables.

## Useful Docs

- [backend/README.md](./backend/README.md)
- [frontend/README.md](./frontend/README.md)
- [specs/002-connect-frontend-backend/plan.md](./specs/002-connect-frontend-backend/plan.md)
- [specs/002-connect-frontend-backend/quickstart.md](./specs/002-connect-frontend-backend/quickstart.md)
- [specs/002-connect-frontend-backend/contracts/rest-api.md](./specs/002-connect-frontend-backend/contracts/rest-api.md)
