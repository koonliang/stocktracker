# StockTracker Backend

Quarkus REST backend for the integrated StockTracker product. It owns the
portfolio transaction API, watchlist API, instrument analysis API, Flyway
migrations, and the seeded reference data used by the frontend.

## Local development

Preferred path from the repository root:

```sh
docker compose up --build
```

Backend endpoints:

```text
API:    http://localhost:8080/api
Health: http://localhost:8080/q/health
```

For backend-only work outside Docker:

```sh
./mvnw quarkus:dev
```

The application expects MySQL unless you are running the test suite, where
Quarkus Dev Services will provision an ephemeral database.

## Quality gates

```sh
./mvnw test
./mvnw spotless:check
./mvnw -DskipTests compile
```

## Local stack verification

After `docker compose up --build` from the repository root:

```sh
../scripts/verify-local-stack.sh
```

That script waits for the backend and frontend, then checks:

- `/q/health`
- `/api/dashboard`
- `/api/watchlists`
