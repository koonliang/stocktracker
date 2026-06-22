# Implementation Plan: CRUD Toast Feedback and Live Seed Accuracy

**Branch**: `main` | **Date**: 2026-06-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-transaction-toasts-live-seed/spec.md`

## Summary

Add consistent in-app toast feedback for authenticated add/update/delete flows plus transaction import/export, and make seeded demo portfolio valuations use the active live market-data provider whenever that provider is enabled.

Technical approach: reuse the existing frontend `toastStore`/`NotificationToaster` surface by introducing a shared action-feedback pattern across transaction, watchlist, and alert workflows, rather than adding backend-generated toast state. On the backend, keep seeded transactions deterministic but refresh seeded/demo portfolio quote-backed values through the existing `MarketDataProvider` and quote cache when live-provider mode is selected, with graceful fallback to stub or last-known values when live data is unavailable.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.6 / React 18.3 (frontend)  
**Primary Dependencies**: Quarkus 3.15.2, Hibernate ORM Panache, Flyway, MySQL JDBC, scheduler, REST/Jackson DTOs; React Router 6, Zustand, Vite 5, React Hook Form + Zod, existing UI primitives, lucide-react  
**Storage**: MySQL 8.4 via existing Flyway migrations and quote cache tables; no new persistent store required by default  
**Testing**: Backend JUnit 5 + QuarkusTest + REST-Assured + Testcontainers MySQL; frontend Vitest + Testing Library + MSW; manual verification against authenticated CRUD flows and provider-mode startup behavior  
**Target Platform**: Web application with Quarkus backend and React frontend; local docker-compose/dev servers and deployed AWS runtime  
**Project Type**: web application  
**Performance Goals**: Action feedback appears immediately after each completed user action; live-provider-backed seed portfolios show current or latest available quote-driven values during the same session without blocking app startup  
**Constraints**: Reuse existing toast infrastructure and provider-selection config; keep deterministic stub behavior for tests/offline dev; avoid speculative abstractions or new persistence unless design proves necessary; preserve per-user data isolation  
**Scale/Scope**: Authenticated CRUD flows across transactions, watchlists, watchlist tickers, and alerts; transaction import/export result feedback; dev/demo seed bootstrap and quote refresh behavior for seeded accounts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Assessment | Status |
|------|------------|--------|
| I. Automated Tests & Manual Verification (NON-NEGOTIABLE) | Plan adds automated coverage for toast-triggering frontend flows and live-seed backend behavior, plus manual verification across authenticated CRUD flows and provider-mode startup. | PASS |
| II. Compilation Integrity (NON-NEGOTIABLE) | Work stays inside existing Quarkus and React modules; backend `./mvnw -B verify` and frontend `npm run verify` remain the compile/test gates. | PASS |
| III. Simplicity & YAGNI | Reuses existing toast store, API error shape, stores, routes, bootstrap, and provider seams. No new persistence or message bus is introduced. | PASS |
| IV. Specification-Driven Development | Plan traces directly to `specs/011-transaction-toasts-live-seed/spec.md`; scope broadening from transaction-only to all authenticated CRUD actions is already captured in the spec. | PASS |

*Post-design re-check*: Phase 1 artifacts keep the same bounded approach: frontend action-feedback standardization plus backend seed-quote mode alignment. No Constitution violations introduced.

## Project Structure

### Documentation (this feature)

```text
specs/011-transaction-toasts-live-seed/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── action-feedback-ui.md
│   └── live-seed-runtime.md
└── tasks.md              # generated later by /speckit.tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/stocktracker/
├── api/
│   ├── TransactionsResource.java
│   ├── WatchlistResource.java
│   └── AlertsResource.java
├── bootstrap/
│   └── DevDataBootstrap.java
├── service/
│   ├── PortfolioService.java
│   ├── AlertService.java
│   ├── WatchlistService.java
│   ├── MarketDataService.java
│   └── QuoteCacheService.java
└── service/provider/
    └── ProviderConfig.java

frontend/src/
├── api/
│   ├── client.ts
│   ├── transactionsApi.ts
│   ├── watchlistsApi.ts
│   └── alertsApi.ts
├── components/layout/
│   └── NotificationToaster.tsx
├── features/
│   ├── transactions/TransactionForm.tsx
│   ├── watchlist/NewWatchlistDialog.tsx
│   └── alerts/NotificationDialog.tsx
├── routes/
│   └── AlertsRoute.tsx
└── stores/
    ├── toastStore.ts
    ├── watchlistStore.ts
    └── notificationsStore.ts
```

**Structure Decision**: Reuse the existing web split (`backend/` + `frontend/`). This feature does not justify a new project or storage layer. Frontend changes concentrate on shared action-feedback behavior across existing stores/routes/components. Backend changes concentrate on `DevDataBootstrap` and existing provider/quote services.

## Implementation Phases

### Phase 0: Research

Resolve these design points and capture them in `research.md`:

1. Where toast ownership should live so success/failure feedback stays consistent across store-driven and route-local mutations.
2. Which authenticated operations are in scope for guaranteed action-feedback toasts versus adjacent actions that should remain out of scope.
3. How live-provider-backed seed values should interact with existing deterministic seed transactions, quote cache, and startup fallback behavior.

### Phase 1: Design & Contracts

1. Model the derived action-feedback and seed-resolution entities in `data-model.md`.
2. Define the user-facing CRUD/import/export feedback contract in `contracts/action-feedback-ui.md`.
3. Define the runtime seed/provider contract in `contracts/live-seed-runtime.md`.
4. Document local verification and provider-mode smoke flows in `quickstart.md`.
5. Update `AGENTS.md` to reference this feature plan.

### Phase 2: Implementation Planning Readiness

Expected implementation areas for `/speckit-tasks`:

1. Frontend shared toast dispatcher/helper and integration into watchlist, alert, and transaction flows.
2. Frontend tests proving exactly-one-toast behavior and failure/success copy across representative actions.
3. Backend/dev-bootstrap updates so seeded/demo portfolios refresh live-backed quotes when `stocktracker.marketdata.provider=yahoo`.
4. Backend tests proving live-seed mode, stub fallback mode, and graceful provider-failure handling.

## Complexity Tracking

> No Constitution Check violations; section intentionally empty.
