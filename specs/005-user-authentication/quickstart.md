# Quickstart: User Authentication

How to run, exercise, and test the authentication feature locally and in CI.

## Modes
- **dev** (default local / docker-compose): backend owns email+password flows and
  issues JWTs; dev-only token endpoint enabled; no AWS.
- **cognito** (production): Cognito owns flows + Google/Facebook; backend validates
  Cognito JWTs. Selected via `STOCKTRACKER_AUTH_MODE=cognito` + `COGNITO_*` vars.

## Run locally (dev mode)

```bash
# Full stack (MySQL + backend + frontend), auth.mode=dev by default
docker compose up -d --wait

# Frontend: http://localhost:5173   Backend: http://localhost:8080
```

Backend only (Quarkus dev mode):
```bash
cd backend && ./mvnw quarkus:dev      # auth.mode=dev, dev token endpoint on
```

Frontend only:
```bash
cd frontend && npm run dev            # uses VITE_API_BASE_URL
```

## Try the flows (dev mode, via curl)

```bash
# 1. Sign up
curl -s -XPOST localhost:8080/api/auth/signup \
  -H 'content-type: application/json' \
  -d '{"email":"alice@example.com","password":"Passw0rd!"}'

# 2. Grab the verification token (DEV ONLY endpoint)
TOKEN=$(curl -s "localhost:8080/api/dev/auth/latest-token?email=alice@example.com&purpose=EMAIL_VERIFICATION" | jq -r .token)

# 3. Verify
curl -s -XPOST localhost:8080/api/auth/verify-email \
  -H 'content-type: application/json' -d "{\"token\":\"$TOKEN\"}"

# 4. Log in → JWT
JWT=$(curl -s -XPOST localhost:8080/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"alice@example.com","password":"Passw0rd!"}' | jq -r .token)

# 5. Call a protected endpoint
curl -s localhost:8080/api/dashboard -H "Authorization: Bearer $JWT"
```

Password reset mirrors the above with `purpose=PASSWORD_RESET`,
`/api/auth/forgot-password`, then `/api/auth/reset-password`.

## Tests

```bash
# Backend unit/integration (incl. mocked social-provider linking)
cd backend && ./mvnw -B verify

# Frontend (Vitest + Testing Library + MSW)
cd frontend && npm run test && npx tsc --noEmit

# e2e authentication journey (needs the stack up, dev mode)
docker compose up -d --wait
mvn -B -f e2e/pom.xml test          # runs AuthJourneyTest among others
```

## Quality gates (constitution)
- Test: `backend ./mvnw verify`, `frontend npm run test`, `mvn -f e2e test` all green.
- Lint: `backend ./mvnw spotless:check`, `frontend npm run lint`, `e2e spotless:check`.
- Compile: `backend ./mvnw -B test-compile`, `frontend tsc --noEmit && vite build`.

## Production (cognito mode) — config sketch
Set on the Lambda backend (via Terraform `lambda_backend` env vars):
```
STOCKTRACKER_AUTH_MODE=cognito
COGNITO_ISSUER=https://cognito-idp.<region>.amazonaws.com/<poolId>
COGNITO_JWKS_URL=${COGNITO_ISSUER}/.well-known/jwks.json
```
`terraform apply` in `infra/envs/production` provisions the pool, app client,
hosted-UI domain, and Google/Facebook IdPs (see contracts/cognito.md). The
dev-only token endpoint is absent in this mode.

## Gotchas
- The `/api/dev/auth/*` endpoint exists ONLY in dev mode — never deployed to prod.
- Migration `V2` backfills existing watchlist/transaction rows to a seed user;
  run against a fresh dev DB or let Flyway migrate at start.
- e2e isolation scenario needs two seeded verified users (dev bootstrap).
