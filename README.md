# StockTracker

StockTracker is a full-stack stock portfolio tracker with a React frontend, a
Quarkus backend, and a MySQL database.

The app covers these main flows:

- user authentication (sign-up, email verification, sign-in/out, password
  reset) with per-user data scoping
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
├── e2e/                  Selenium + JUnit 5 regression suite (isolated Maven project)
├── infra/                Terraform stacks and AWS deployment docs
├── scripts/              Stack verification helpers
├── specs/                Feature specs, plans, contracts, and quickstarts
├── .github/workflows/    CI/CD and regression pipelines
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

End-to-end regression (drives a headless browser against the running stack):

```bash
docker compose up -d --wait
mvn -B -f e2e/pom.xml test
docker compose down -v
```

The suite runs automatically in CI on PRs to `main`. See
[e2e/README.md](./e2e/README.md) for journeys, configuration, and the Allure
HTML report.

## Authentication

Every API route except the auth endpoints requires a signed-in user, and all
user-owned data (transactions, watchlists) is scoped per user. The backend runs
in one of two modes, selected by `STOCKTRACKER_AUTH_MODE`; the REST layer
validates JWTs the same way in both.

- **`dev` (default, local):** the backend owns sign-up, email verification,
  sign-in/out, and password reset, and issues self-signed RS256 JWTs. **No real
  email is sent** — the verification/reset token is logged and exposed via a
  dev-only endpoint so flows work without an inbox. To complete a sign-up
  locally:

  ```bash
  # 1. Sign up at http://localhost:5173/signup, then fetch the token:
  curl "http://localhost:8080/api/dev/auth/latest-token?email=you@example.com&purpose=EMAIL_VERIFICATION"
  # (or: docker compose logs backend | grep email_dev_sink)

  # 2. Open the verify link with that token:
  #    http://localhost:5173/verify-email?token=<rawToken>
  ```

  Use `purpose=PASSWORD_RESET` and `/reset-password?token=...` for the reset
  flow. Bootstrap also seeds two verified accounts —
  `seed@stocktracker.local` (owns demo data) and `empty@stocktracker.local` —
  both with password `DevPass123!`.

- **`cognito` (production):** an AWS Cognito user pool owns sign-up,
  verification, password reset, and Google/Facebook federation, and sends the
  real emails. The backend only validates Cognito-issued JWTs; the dev
  `/api/auth/*` and dev-token endpoints go dark (return 404). Verified-email
  account linking between local and federated identities is done backend-side
  on first token by `AccountLinkingService`.

`/api/auth/*` is protected by a per-IP and per-email sliding-window rate limit
(`STOCKTRACKER_AUTH_RATE_LIMIT_MAX` / `_WINDOW`). See
[backend/README.md](./backend/README.md) for endpoint and config detail.

## Data Model Notes

The mutable user-owned tables use `BIGINT` primary keys:

- `portfolio_transaction`
- `watchlist`
- `watchlist_item`

The backend keeps holdings and summary values as derived projections rather than
persisted tables.

## Deployment & Infrastructure

StockTracker deploys to AWS via Terraform and GitHub Actions. See
[infra/README.md](./infra/README.md) for the full architecture, provisioning
steps, and pipeline reference.

Summary of what this feature provisions:

- **CI/CD pipelines** in `.github/workflows/` — PR-time gates (backend tests,
  frontend tests, Terraform plan, secret scan) plus manual-trigger CD,
  rollback, drift-check, and destroy workflows. AWS access is via GitHub OIDC,
  with no long-lived credentials.
- **Split Terraform stacks** — a persistent stack (CloudFront + private S3
  frontend bucket, kept up between test sessions, ~$0 idle) and an ephemeral
  stack (VPC, RDS MySQL, Lambda, API Gateway, applied and destroyed per
  session).
- **Frontend edge** — CloudFront serves the private S3 bucket via Origin Access
  Control; the browser calls the backend cross-origin at the API Gateway URL.
- **Backend** — API Gateway (HTTP API) proxies `/api/*` to a Quarkus Lambda
  inside the VPC; a one-shot migrator Lambda runs Flyway on deploy. No NAT
  gateway — private subnets reach AWS services through interface VPC endpoints.
- **Database** — RDS MySQL with an RDS-managed master password in Secrets
  Manager, read by the Lambdas at startup via the AWS SDK.
- **Authentication** — a Cognito user pool (email sign-up/verification,
  password reset, optional Google/Facebook federation) issues the JWTs the
  backend Lambda validates; the backend runs with `STOCKTRACKER_AUTH_MODE=cognito`.
- **Bootstrap** — a one-time stack provisioning the Terraform state backend
  (S3 + DynamoDB lock) and the GitHub OIDC provider + IAM roles.

## Useful Docs

- [backend/README.md](./backend/README.md)
- [frontend/README.md](./frontend/README.md)
- [e2e/README.md](./e2e/README.md)
- [infra/README.md](./infra/README.md)
- [specs/003-ci-cd-aws/plan.md](./specs/003-ci-cd-aws/plan.md)
- [specs/002-connect-frontend-backend/plan.md](./specs/002-connect-frontend-backend/plan.md)
- [specs/002-connect-frontend-backend/quickstart.md](./specs/002-connect-frontend-backend/quickstart.md)
- [specs/002-connect-frontend-backend/contracts/rest-api.md](./specs/002-connect-frontend-backend/contracts/rest-api.md)
