# Phase 0 Research: StockTracker Full-Stack Integration

Purpose: resolve the implementation choices needed to turn the existing
frontend-only prototype into a persistent full-stack product without leaving any
technical clarifications open before task generation.

Result: all items below are **Decided**. Zero open `NEEDS CLARIFICATION`.

---

## 1. Runtime architecture

- **Decision**: Use a three-service local architecture: React/Vite frontend,
  Quarkus REST backend, and MySQL database, all started through Docker Compose.
- **Rationale**: This directly satisfies the user request and the spec's local
  full-stack workflow requirements while keeping responsibilities clear between
  UI, application logic, and persistence.
- **Alternatives considered**:
  - Frontend + backend only, with in-memory persistence: rejected because it
    fails the persistence requirement.
  - Backend outside Compose: rejected because the user explicitly requested
    Compose for frontend, backend, and MySQL together.

## 2. Backend framework and Java baseline

- **Decision**: Standardize the backend on Java 21 and Quarkus 3.x with Quarkus
  REST, JSON serialization, Hibernate ORM with Panache, Hibernate Validator,
  Flyway, and the MySQL JDBC driver.
- **Rationale**: Java 21 is the sensible modern LTS baseline. Quarkus aligns
  with the user-requested stack and provides fast local startup, tight testing
  support, and low ceremony for a modest REST domain.
- **Alternatives considered**:
  - Spring Boot: mature, but explicitly outside the requested stack.
  - Plain JAX-RS without Quarkus extensions: more manual setup for no product
    gain.

## 3. Persistence design

- **Decision**: Persist normalized domain tables in MySQL for transactions,
  watchlists, watchlist items, instruments, price history, and key statistics.
  Derive holdings and portfolio summaries in backend services instead of storing
  them as tables.
- **Rationale**: Transactions and watchlists are the source of truth; holdings
  and summary values are projections. Persisting projections would introduce
  synchronization risk with no requirement that justifies it.
- **Alternatives considered**:
  - Persist precomputed holdings: rejected as needless denormalization.
  - Keep market reference data in frontend JSON only: rejected because analysis
    and dashboard data should come from the backend in this iteration.

## 4. Database migrations and seeding

- **Decision**: Manage schema with Flyway and seed reference instrument data
  from backend resource files during application bootstrap when the target
  tables are empty.
- **Rationale**: Flyway gives explicit, reviewable schema evolution. Seed-on-
  empty bootstrap keeps local startup deterministic without forcing seed logic
  into every migration.
- **Alternatives considered**:
  - `import.sql` only: acceptable for prototypes, but less explicit as schema
    evolves.
  - Manual seed scripts: too error-prone for a required local workflow.

## 5. API design style

- **Decision**: Expose resource-oriented REST endpoints with JSON payloads for
  dashboard, watchlists, instruments, and transactions. Keep CSV preview and
  commit as separate endpoints.
- **Rationale**: Separate preview/commit endpoints map cleanly to the current
  UI and avoid persisting partial imports. REST is sufficient at this scale and
  easier to test than introducing GraphQL or evented patterns.
- **Alternatives considered**:
  - GraphQL: unnecessary complexity for a small, stable surface area.
  - One-step import endpoint: rejected because the product requires preview and
    invalid-row reporting before save.

## 6. Frontend state strategy

- **Decision**: Keep Zustand as the frontend state container, but remove
  `persist` for domain data and replace it with async API-backed actions. Use
  Zustand for UI state and request lifecycle status rather than adding a second
  client state library.
- **Rationale**: The app already has domain stores wired into components. This
  reduces migration churn and fits the constitution's simplicity principle.
- **Alternatives considered**:
  - TanStack Query plus store rewrite: viable, but larger migration with
    overlapping responsibilities.
  - Raw per-route fetching only: would duplicate request logic across routes.

## 7. Frontend integration testing

- **Decision**: Add MSW for frontend tests that exercise backend interactions
  while keeping Vitest and Testing Library as the core test runner stack.
- **Rationale**: Existing tests are already in Vitest. MSW lets route and store
  tests validate loading, success, and error states without needing the Quarkus
  backend running.
- **Alternatives considered**:
  - Hand-rolled fetch mocks: less realistic and harder to reuse.
  - Browser E2E as the only integration coverage: too heavy for every change.

## 8. Backend testing strategy

- **Decision**: Use `@QuarkusTest` and RestAssured for HTTP contract tests, and
  Testcontainers MySQL for persistence-sensitive tests.
- **Rationale**: The risky part of this feature is the persistence boundary.
  Testing against MySQL-compatible behavior is more reliable than using an
  in-memory substitute with different SQL semantics.
- **Alternatives considered**:
  - H2 for all backend tests: simpler, but risky because MySQL behavior and SQL
    dialect edge cases matter for migrations and persistence.

## 9. CSV contract continuity

- **Decision**: Keep the canonical CSV schema unchanged:
  `date,ticker,type,quantity,price,fees`. Validation moves server-side for
  preview/commit, and export continues to emit the same schema.
- **Rationale**: This preserves the prototype's documented import/export
  behavior and avoids forcing frontend or user-facing contract changes during
  the persistence migration.
- **Alternatives considered**:
  - Introducing backend-specific metadata columns: rejected because it breaks
    round-trip expectations for no user benefit.

## 10. Local development workflow

- **Decision**: Make Docker Compose the primary documented workflow. The
  frontend runs in a containerized Vite dev server, the backend runs in a
  containerized Quarkus dev/runtime image, and MySQL uses a named volume for
  persistence across restarts.
- **Rationale**: This gives one startup path for contributors and reviewers and
  directly supports the spec's persistence-after-restart requirement.
- **Alternatives considered**:
  - Dockerizing only the database: easier, but fails the explicit request.
  - Serving a static frontend build in Compose: acceptable for demoing, but
    worse for daily development than a dev server with live reload.

## 11. Quality gates for the new stack

- **Decision**:
  - Frontend test gate: `cd frontend && npm test`
  - Frontend lint gate: `cd frontend && npm run lint`
  - Frontend compile gate: `cd frontend && npm run typecheck && npm run build`
  - Backend test gate: `cd backend && ./mvnw test`
  - Backend lint gate: `cd backend && ./mvnw spotless:check`
  - Backend compile gate: `cd backend && ./mvnw -DskipTests compile`
- **Rationale**: The constitution requires explicit tests, lint, and compile
  checks. These commands keep those boundaries visible instead of hiding them in
  one opaque task.
- **Alternatives considered**:
  - Single root verify command only: convenient later, but too implicit for the
    initial planning artifact.

---

## Open questions

None. All research items required for planning are resolved.
