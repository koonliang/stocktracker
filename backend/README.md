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

## Authentication

The backend gates every non-auth API route behind a signed-in user and scopes
user-owned data per user (`CurrentUser`). It runs in one of two modes via
`STOCKTRACKER_AUTH_MODE`; the resource layer validates JWTs identically in both
(dev self-signed RS256 vs Cognito-issued).

| Mode               | Sign-up / verify / reset            | JWTs                  | Email                                  |
| ------------------ | ----------------------------------- | --------------------- | -------------------------------------- |
| `dev` (default)    | Owned by `AuthService` (`/api/auth/*`) | Self-signed RS256     | Not sent — token logged + dev endpoint |
| `cognito` (prod)   | Owned by the Cognito user pool      | Cognito-issued, validated | Sent by Cognito                    |

Endpoints (dev mode):

```text
POST /api/auth/signup                 sign up (issues an email-verification token)
POST /api/auth/verify-email           activate an account from a verification token
POST /api/auth/resend-verification    re-issue verification for a pending account
POST /api/auth/login                  sign in (returns an access token)
POST /api/auth/forgot-password        request a password-reset token
POST /api/auth/reset-password         set a new password from a reset token
GET  /api/dev/auth/latest-token       dev-only: read the latest raw token (404 in cognito mode)
```

In dev mode **no real email is sent**: `EmailSender` logs the raw token
(`event=email_dev_sink ...`) and `DevTokenStore` mirrors it for the dev-only
`GET /api/dev/auth/latest-token?email=<email>&purpose=EMAIL_VERIFICATION|PASSWORD_RESET`
endpoint, so verify/reset flows run without an inbox. The DB only ever stores
the SHA-256 hash of each token. To verify a local sign-up:

```sh
curl "http://localhost:8080/api/dev/auth/latest-token?email=you@example.com&purpose=EMAIL_VERIFICATION"
# then open http://localhost:5173/verify-email?token=<rawToken>
```

In `cognito` mode the `/api/auth/*` and dev-token endpoints return 404; Cognito
owns those flows and the backend only validates pool-issued JWTs via
`COGNITO_ISSUER` / `COGNITO_JWKS_URL`. Verified-email linking of local and
federated identities is performed on first token by `AccountLinkingService`.

Relevant config (`application.properties`):

- `stocktracker.auth.mode` — `dev` or `cognito` (`STOCKTRACKER_AUTH_MODE`)
- `stocktracker.auth.verification-token.ttl-seconds` / `reset-token.ttl-seconds`
- `stocktracker.auth.access-token.ttl-seconds` (`STOCKTRACKER_AUTH_TOKEN_TTL`)
- `stocktracker.auth.rate-limit.max-attempts` / `window-seconds` — per-IP and
  per-email sliding-window throttle on `/api/auth/*`

Password policy: minimum 8 characters with an uppercase, lowercase, and digit
(`PasswordPolicy`), enforced identically at sign-up and reset.

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
Lambda** before promoting the new application code. The migrator runs the same
deployment artifact under the `migrate` Quarkus profile, which applies Flyway
migrations at startup (`quarkus.flyway.migrate-at-start`). The backend HTTP
Lambda has migration-at-start disabled (`QUARKUS_FLYWAY_MIGRATE_AT_START=false`)
so schema changes only ever apply through the migrator.
If the migration fails, the migrator's startup fails and the application Lambda
alias is **not** updated, so the previous version keeps serving. See
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
