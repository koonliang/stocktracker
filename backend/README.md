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

## SIT homelab deployment

The repository includes a manual deployment path for the private homelab SIT
environment:

```sh
cp scripts/.env.example scripts/.env
# edit scripts/.env on the homelab machine
bash scripts/deploy-homelab-sit.sh --validate-only
bash scripts/deploy-homelab-sit.sh
```

Expected inputs live in `scripts/.env` and are intentionally kept out of git.
The script builds the backend as a Quarkus JVM app, uploads it to the app host,
uploads or updates the systemd unit when `systemctl` is available, restarts the
configured service or process, and verifies
`PUBLIC_BASE_URL/q/health` before succeeding.

The deploy user on the homelab app host must be able to run the required
`sudo install` and `sudo systemctl` commands non-interactively.

## Authentication

The backend gates every non-auth API route behind a signed-in user and scopes
user-owned data per user (`CurrentUser`). It runs in one of two modes via
`STOCKTRACKER_AUTH_MODE`; the resource layer validates JWTs identically in both
(dev self-signed RS256 vs Cognito-issued).

| Mode               | Sign-up / verify / reset            | JWTs                  | Email                                  |
| ------------------ | ----------------------------------- | --------------------- | -------------------------------------- |
| `dev` (default)    | Owned by `AuthService` (`/api/auth/*`) | Self-signed RS256     | Not sent — token logged + dev endpoint |
| `cognito` (prod)   | Owned by the Cognito user pool      | Cognito-issued, validated | Sent by Cognito                    |

The non-production social identity implementation extends the existing `dev`
mode instead of adding a separate auth mode. The backend accepts a Google or
Facebook authorization code, exchanges it server-side, resolves the provider
profile, links it through `AccountLinkingService`, and returns the same
app-issued JWT used by ordinary dev login. Verified provider emails reuse an
existing account when possible; otherwise a new active, verified account is
created immediately. Unverified provider emails are never auto-linked to an
existing user.

Endpoints (dev mode):

```text
POST /api/auth/signup                 sign up (issues an email-verification token)
POST /api/auth/verify-email           activate an account from a verification token
POST /api/auth/resend-verification    re-issue verification for a pending account
POST /api/auth/login                  sign in (returns an access token)
POST /api/auth/social/google/exchange exchange a Google auth code for an app session
POST /api/auth/social/facebook/exchange exchange a Facebook auth code for an app session
GET  /api/auth/demo-users             list reusable passwordless demo users
POST /api/auth/demo-users             create the next demo user and sign in immediately
POST /api/auth/demo-users/{slot}/login sign in as an existing demo user
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

The same non-production profile also supports passwordless demo users.
`DemoUserService` manages up to three demo accounts per environment, each as a
first-class `AppUser` with seeded portfolio data. Creating or selecting a demo
user returns a normal app JWT; there is no separate anonymous session path.

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
- `nonprod.google.client-id` / `nonprod.google.client-secret`
- `nonprod.facebook.client-id` / `nonprod.facebook.client-secret`
- `nonprod.social.redirect-uri`
- `stocktracker.demo-users.enabled` / `stocktracker.demo-user.max` /
  `stocktracker.demo-user.prefix`

Password policy: minimum 8 characters with an uppercase, lowercase, and digit
(`PasswordPolicy`), enforced identically at sign-up and reset.

## Quality gates

```sh
./mvnw test
./mvnw spotless:check
./mvnw -DskipTests compile
```

## Scheduling Jobs

Batch jobs live in `com.stocktracker.scheduler`:

- `QuoteRefreshJob` refreshes cached market quotes for symbols currently held
  or watched.
- `FxRefreshJob` refreshes daily FX rates for currencies in use.
- `TokenCleanupJob` purges expired or consumed verification/reset tokens.

Local development uses Quarkus' in-process scheduler, so `docker compose up`
and `./mvnw quarkus:dev` run those jobs inside the backend process.

Production disables the in-process scheduler on the request-serving Lambda with
`QUARKUS_SCHEDULER_ENABLED=false`. Lambda can freeze execution between requests,
which makes background transactions unsafe for long-running schedulers. Instead,
Terraform creates EventBridge rules that invoke the backend Lambda alias with
API Gateway v2-shaped payloads:

```text
POST /api/internal/jobs/quote-refresh   every 1 minute
POST /api/internal/jobs/token-cleanup   every 1 hour
POST /api/internal/jobs/fx-refresh      daily at 01:00 UTC
```

Those internal endpoints require `x-stocktracker-scheduler-token`; production
sets the token from Terraform and includes it in each EventBridge payload. The
request-serving Lambda also sets `STOCKTRACKER_MARKETDATA_PROVIDER=yahoo` and
`STOCKTRACKER_FX_PROVIDER=frankfurter`, so scheduled refreshes use the real
providers in production.

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
