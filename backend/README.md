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

## Database migrations

Migrations use [Flyway](https://flywaydb.org/) and live in
`src/main/resources/db/migration/`.

### Filename pattern

```
V<version>__<description>.sql
```

Example: `V2__add_dividends_column.sql`

### Conventions

- **Forward-only**: never edit or delete a migration that has been merged to
  `main`. Fix mistakes with a new migration.
- **Non-destructive**: prefer `ADD COLUMN` with defaults over `DROP COLUMN`.
  If a column must be removed, do it in a separate migration after the
  application code no longer references it.
- **Idempotent where possible**: use `IF NOT EXISTS` / `IF EXISTS` guards.
- **No data transformations in DDL migrations**: separate schema changes from
  large data backfills to keep migration time predictable.

### How migrations run in production

On every merge to `main`, the CD pipeline invokes a dedicated **migrator
Lambda** (`MigrationHandler`) before promoting the new application code.
If the migration fails, the application Lambda alias is **not** updated and
the previous version keeps serving. See
[quickstart.md](../specs/003-ci-cd-aws/quickstart.md) for troubleshooting.

### Local development

Flyway runs automatically on startup (`quarkus.flyway.migrate-at-start=true`).
With `docker compose up --build` from the repo root, the MySQL container is
provisioned and migrations apply on first backend boot.

## Local stack verification

After `docker compose up --build` from the repository root:

```sh
../scripts/verify-local-stack.sh
```

That script waits for the backend and frontend, then checks:

- `/q/health`
- `/api/dashboard`
- `/api/watchlists`
