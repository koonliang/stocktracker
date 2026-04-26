# Implementation Plan: StockTracker Full-Stack Integration

**Branch**: `002-connect-frontend-backend` | **Date**: 2026-04-25 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-connect-frontend-backend/spec.md`

## Summary

Upgrade the existing StockTracker frontend prototype into a full-stack web
application by keeping the current React/Vite user experience, introducing a
Java Quarkus REST backend backed by MySQL, and replacing browser-only
portfolio/watchlist persistence with shared server-backed data. Local
development becomes a three-service Docker Compose stack for frontend, backend,
and database, with seeded market reference data available on first startup.

## Technical Context

**Language/Version**:
- Frontend: TypeScript 5.6+, React 18
- Backend: Java 21, Quarkus 3.x

**Primary Dependencies**:
- Frontend: React Router v6, Zustand 4, React Hook Form, Zod, Recharts
- Backend: Quarkus REST (JSON), Hibernate ORM with Panache, Hibernate
  Validator, Flyway, MySQL JDBC driver
- Local development: Docker Compose

**Storage**:
- MySQL 8 for transactions, watchlists, ticker catalog, price history, and key
  statistics
- Named Docker volume for persistent local development data

**Testing**:
- Frontend: Vitest, Testing Library, vitest-axe, MSW for API mocking
- Backend: JUnit 5, `@QuarkusTest`, RestAssured, Testcontainers MySQL

**Target Platform**:
- Dockerized local development on contributor machines
- Evergreen desktop/mobile browsers for the frontend
- Linux container runtime for backend and database services

**Project Type**:
- Web application with SPA frontend, REST API backend, and relational database

**Performance Goals**:
- Dashboard, watchlist, and analysis views render meaningful data within 2
  seconds after route navigation on a warm local stack
- CSV preview/commit for 50 rows completes within 5 seconds on local
  development hardware
- Typical read/write API requests complete within 250 ms p95 on the local stack

**Constraints**:
- Preserve the existing frontend route structure and user-facing features while
  changing persistence from `localStorage` to backend APIs
- No authentication or multi-user scope in this iteration
- Docker Compose MUST start frontend, backend, and MySQL together for local
  development
- Seed market/instrument data MUST be available after first startup without
  external vendor calls
- Canonical CSV import/export contract from the prototype MUST remain stable

**Scale/Scope**:
- Single default portfolio context for development and review
- Tens of watchlists, hundreds of transactions, and dozens of tracked
  instruments are sufficient for this iteration
- Four primary product areas remain in scope: dashboard, watchlists, stock
  analysis, import/export

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Gates derived from `.specify/memory/constitution.md` v1.0.0:

| Principle | How this plan satisfies it | Status |
|-----------|----------------------------|--------|
| I. Test Verification (NON-NEGOTIABLE) | Frontend keeps Vitest/RTL/a11y coverage and adds MSW-backed integration tests for API calls. Backend adds `@QuarkusTest` + RestAssured endpoint tests and MySQL-backed repository/service tests. | PASS |
| II. Lint & Style Compliance (NON-NEGOTIABLE) | Frontend keeps `npm run lint`; backend adds Maven formatting/lint checks (`spotless:check`) so changed Java code has an explicit style gate. | PASS |
| III. Compilation Integrity (NON-NEGOTIABLE) | Frontend keeps `npm run typecheck` and `npm run build`; backend adds `./mvnw -DskipTests compile` as the compile gate. | PASS |
| IV. Simplicity & YAGNI | Existing frontend route/component structure is preserved. Server state stays in current Zustand domain stores instead of adding a second client state framework. Holdings/portfolio remain derived, not persisted. | PASS |
| V. Specification-Driven Development | This plan implements the approved `002-connect-frontend-backend` spec and defines concrete research, contracts, and data model outputs before tasks or code changes. | PASS |

**Post-design re-check (after Phase 1)**: PASS — the data model, contracts, and
quickstart introduce no unjustified abstractions beyond a standard REST API,
relational persistence, and Docker Compose workflow needed by the spec.

## Project Structure

### Documentation (this feature)

```text
specs/002-connect-frontend-backend/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rest-api.md
│   ├── ui-routes.md
│   └── csv-transaction-schema.md
└── tasks.md
```

### Source Code (repository root)

```text
docker-compose.yml
backend/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/stocktracker/
│   │   │   ├── api/
│   │   │   ├── domain/
│   │   │   ├── dto/
│   │   │   ├── persistence/
│   │   │   ├── service/
│   │   │   └── bootstrap/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/
│   │       └── seed/
│   └── test/
│       └── java/com/stocktracker/
│           ├── api/
│           ├── persistence/
│           └── service/
frontend/
├── package.json
├── Dockerfile
├── src/
│   ├── api/
│   │   ├── client.ts
│   │   ├── dashboardApi.ts
│   │   ├── instrumentsApi.ts
│   │   ├── transactionsApi.ts
│   │   └── watchlistsApi.ts
│   ├── components/
│   ├── features/
│   ├── lib/
│   ├── routes/
│   ├── stores/
│   ├── styles/
│   └── test/
└── tests/
```

**Structure Decision**: Keep the current single `frontend/` package intact and
add a sibling `backend/` service plus root-level `docker-compose.yml`. On the
frontend, introduce an `api/` layer and convert `portfolioStore` and
`watchlistStore` from persisted local stores into async server-backed stores.
On the backend, organize by API, service, and persistence layers with Flyway
migrations and seed resources. This keeps the migration from the prototype
incremental rather than rewriting the app structure.

## Complexity Tracking

No constitutional violations. This section intentionally remains empty.
