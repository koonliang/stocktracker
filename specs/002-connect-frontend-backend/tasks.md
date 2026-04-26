# Tasks: StockTracker Full-Stack Integration

**Input**: Design documents from `/specs/002-connect-frontend-backend/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

**Tests**: Tests are required for this feature because the project constitution and
the implementation plan define explicit test gates for frontend and backend work.

**Organization**: Tasks are grouped by user story so each story can be
implemented, verified, and delivered independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on unfinished tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, etc.)
- Every task includes concrete file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the base full-stack project structure and developer entrypoints.

- [x] T001 Scaffold the Quarkus backend project in `backend/pom.xml`, `backend/src/main/resources/application.properties`, and `backend/src/test/resources/application.properties`
- [x] T002 [P] Add container build definitions in `backend/Dockerfile` and `frontend/Dockerfile`
- [x] T003 [P] Create root local-stack orchestration in `docker-compose.yml`
- [x] T004 [P] Add frontend API environment wiring in `frontend/.env.example` and `frontend/src/api/client.ts`
- [x] T005 [P] Add the Maven wrapper files in `backend/mvnw`, `backend/mvnw.cmd`, and `backend/.mvn/wrapper/maven-wrapper.properties`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared persistence, seed, API, and test infrastructure that all user stories depend on.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [x] T006 Create the baseline MySQL schema migration in `backend/src/main/resources/db/migration/V1__init_schema.sql`
- [x] T007 [P] Add backend seed datasets in `backend/src/main/resources/seed/instruments.json`, `backend/src/main/resources/seed/price-bars.json`, and `backend/src/main/resources/seed/instrument-stats.json`
- [x] T008 [P] Implement instrument persistence models and repositories in `backend/src/main/java/com/stocktracker/domain/Instrument.java`, `backend/src/main/java/com/stocktracker/domain/InstrumentPriceBar.java`, `backend/src/main/java/com/stocktracker/domain/InstrumentStat.java`, and `backend/src/main/java/com/stocktracker/persistence/InstrumentRepository.java`
- [x] T009 [P] Implement portfolio and watchlist persistence models and repositories in `backend/src/main/java/com/stocktracker/domain/PortfolioTransaction.java`, `backend/src/main/java/com/stocktracker/domain/Watchlist.java`, `backend/src/main/java/com/stocktracker/domain/WatchlistItem.java`, `backend/src/main/java/com/stocktracker/persistence/PortfolioTransactionRepository.java`, and `backend/src/main/java/com/stocktracker/persistence/WatchlistRepository.java`
- [x] T010 Implement backend bootstrap and error mapping in `backend/src/main/java/com/stocktracker/bootstrap/ReferenceDataBootstrap.java` and `backend/src/main/java/com/stocktracker/api/ApiExceptionMapper.java`
- [x] T011 [P] Add shared backend DTOs and enums in `backend/src/main/java/com/stocktracker/dto/ApiErrorResponse.java`, `backend/src/main/java/com/stocktracker/dto/DashboardResponse.java`, `backend/src/main/java/com/stocktracker/dto/TransactionRequest.java`, and `backend/src/main/java/com/stocktracker/dto/WatchlistResponse.java`
- [x] T012 [P] Add frontend API models and MSW fixtures in `frontend/src/api/types.ts` and `frontend/src/test/msw/handlers.ts`
- [x] T013 Implement frontend API test-server wiring in `frontend/src/test/setup.ts`, `frontend/src/test/server.ts`, and `frontend/src/test/utils.tsx`
- [x] T014 Implement frontend business API modules in `frontend/src/api/dashboardApi.ts`, `frontend/src/api/watchlistsApi.ts`, `frontend/src/api/instrumentsApi.ts`, and `frontend/src/api/transactionsApi.ts`

**Checkpoint**: Database schema, seed data, base DTOs, and frontend/backend API plumbing are ready. User stories can now be implemented.

---

## Phase 3: User Story 1 - Persist Portfolio Data (Priority: P1) 🎯 MVP

**Goal**: Replace browser-only portfolio persistence with server-backed dashboard and transaction data.

**Independent Test**: Save portfolio data, reload the browser and restart the local stack, then confirm dashboard totals, holdings, and position summaries remain unchanged.

### Tests for User Story 1

- [x] T015 [P] [US1] Add backend dashboard and transactions endpoint tests in `backend/src/test/java/com/stocktracker/api/DashboardResourceTest.java` and `backend/src/test/java/com/stocktracker/api/TransactionsResourceTest.java`
- [x] T016 [P] [US1] Add frontend dashboard and portfolio-store integration tests in `frontend/tests/routes/DashboardRoute.test.tsx` and `frontend/tests/stores/portfolioStore.test.ts`

### Implementation for User Story 1

- [x] T017 [P] [US1] Implement portfolio aggregation and transaction validation services in `backend/src/main/java/com/stocktracker/service/PortfolioService.java` and `backend/src/main/java/com/stocktracker/service/TransactionValidationService.java`
- [x] T018 [US1] Implement dashboard and transaction REST resources in `backend/src/main/java/com/stocktracker/api/DashboardResource.java` and `backend/src/main/java/com/stocktracker/api/TransactionsResource.java`
- [x] T019 [P] [US1] Replace local portfolio persistence with API-backed actions in `frontend/src/stores/portfolioStore.ts` and `frontend/src/api/dashboardApi.ts`
- [x] T020 [P] [US1] Update dashboard async loading, error, and empty states in `frontend/src/routes/DashboardRoute.tsx`, `frontend/src/features/dashboard/useHoldings.ts`, `frontend/src/features/dashboard/DashboardEmptyState.tsx`, and `frontend/src/features/dashboard/HoldingsTable.tsx`
- [x] T021 [US1] Wire persisted transaction listing and delete-refresh behavior in `frontend/src/routes/TransactionsRoute.tsx` and `frontend/src/features/transactions/TransactionsTable.tsx`

**Checkpoint**: User Story 1 is complete when the dashboard and transaction history are fully backed by persisted server data and survive app restarts.

---

## Phase 4: User Story 2 - Manage Watchlists And Analysis From Shared Data (Priority: P2)

**Goal**: Move watchlists and analysis views to backend-backed shared data while preserving current route behavior.

**Independent Test**: Create a watchlist, add/remove/reorder tickers, open analysis from the dashboard and watchlist routes, reload the app, and confirm the same data remains available.

### Tests for User Story 2

- [x] T022 [P] [US2] Add backend watchlist and instrument endpoint tests in `backend/src/test/java/com/stocktracker/api/WatchlistResourceTest.java` and `backend/src/test/java/com/stocktracker/api/InstrumentResourceTest.java`
- [x] T023 [P] [US2] Add frontend watchlist and analysis route tests in `frontend/tests/routes/WatchlistsRoute.test.tsx`, `frontend/tests/routes/WatchlistDetailRoute.test.tsx`, and `frontend/tests/routes/AnalysisRoute.test.tsx`

### Implementation for User Story 2

- [x] T024 [P] [US2] Implement watchlist service and mutation DTOs in `backend/src/main/java/com/stocktracker/service/WatchlistService.java` and `backend/src/main/java/com/stocktracker/dto/WatchlistMutationRequest.java`
- [x] T025 [P] [US2] Implement instrument analysis service and response DTOs in `backend/src/main/java/com/stocktracker/service/InstrumentService.java` and `backend/src/main/java/com/stocktracker/dto/InstrumentAnalysisResponse.java`
- [x] T026 [US2] Implement watchlist and instrument REST resources in `backend/src/main/java/com/stocktracker/api/WatchlistResource.java` and `backend/src/main/java/com/stocktracker/api/InstrumentResource.java`
- [x] T027 [P] [US2] Replace persisted watchlist mutations with backend calls in `frontend/src/stores/watchlistStore.ts` and `frontend/src/api/watchlistsApi.ts`
- [x] T028 [US2] Update watchlist and analysis UI for server-backed loading and error states in `frontend/src/routes/WatchlistsRoute.tsx`, `frontend/src/routes/WatchlistDetailRoute.tsx`, `frontend/src/routes/AnalysisRoute.tsx`, `frontend/src/features/watchlist/AddTickerInput.tsx`, `frontend/src/features/watchlist/WatchlistRow.tsx`, `frontend/src/features/analysis/PriceChart.tsx`, `frontend/src/features/analysis/KeyStatsGrid.tsx`, and `frontend/src/features/analysis/PositionSummary.tsx`

**Checkpoint**: User Story 2 is complete when watchlists and analysis views behave consistently across reloads and all data comes from the backend.

---

## Phase 5: User Story 3 - Import And Export Portfolio History (Priority: P2)

**Goal**: Move CSV preview, commit, and export flows to the backend while preserving the canonical CSV contract.

**Independent Test**: Upload a mixed-validity CSV, verify invalid rows are excluded during preview, commit valid rows, confirm the dashboard updates, then export and re-import the resulting file successfully.

### Tests for User Story 3

- [x] T029 [P] [US3] Add backend CSV preview, commit, and export tests in `backend/src/test/java/com/stocktracker/api/TransactionImportResourceTest.java` and `backend/src/test/java/com/stocktracker/api/TransactionExportResourceTest.java`
- [x] T030 [P] [US3] Add frontend import/export flow tests in `frontend/tests/features/transactions/ImportPreview.test.tsx`, `frontend/tests/features/transactions/ExportButton.test.tsx`, and `frontend/tests/routes/TransactionsRoute.test.tsx`

### Implementation for User Story 3

- [x] T031 [P] [US3] Implement backend CSV parsing, normalization, and export services in `backend/src/main/java/com/stocktracker/service/TransactionImportService.java`, `backend/src/main/java/com/stocktracker/service/TransactionExportService.java`, and `backend/src/main/java/com/stocktracker/dto/TransactionImportPreviewResponse.java`
- [x] T032 [US3] Extend transaction REST APIs for `/import/preview`, `/import/commit`, and `/export` in `backend/src/main/java/com/stocktracker/api/TransactionsResource.java` and `backend/src/main/java/com/stocktracker/dto/TransactionImportCommitRequest.java`
- [x] T033 [P] [US3] Connect the frontend transaction client to preview, commit, and export endpoints in `frontend/src/api/transactionsApi.ts` and `frontend/src/lib/csv.ts`
- [x] T034 [US3] Update the import/export UI for backend preview and commit flows in `frontend/src/features/transactions/ImportDropzone.tsx`, `frontend/src/features/transactions/ImportPreview.tsx`, `frontend/src/features/transactions/ExportButton.tsx`, and `frontend/src/routes/TransactionsRoute.tsx`
- [x] T035 [US3] Refresh dashboard and portfolio state after successful import/export actions in `frontend/src/stores/portfolioStore.ts`, `frontend/src/routes/DashboardRoute.tsx`, and `frontend/src/features/dashboard/SummaryTiles.tsx`

**Checkpoint**: User Story 3 is complete when import preview, commit, export, and dashboard refresh all work against persisted backend data and preserve the CSV contract.

---

## Phase 6: User Story 4 - Run The Product Locally End To End (Priority: P3)

**Goal**: Deliver a reproducible Docker Compose workflow that starts the full product and preserves data across normal restarts.

**Independent Test**: Follow the documented startup workflow on a clean checkout, load the app, complete a persisted data flow, restart the stack without reset, and verify the saved data remains.

### Tests for User Story 4

- [x] T036 [P] [US4] Add a local-stack smoke verification script in `scripts/verify-local-stack.sh` and `scripts/wait-for-http.sh`

### Implementation for User Story 4

- [x] T037 [P] [US4] Finalize Compose healthchecks, environment wiring, and persistent volumes in `docker-compose.yml`, `backend/Dockerfile`, and `frontend/Dockerfile`
- [x] T038 [P] [US4] Add development bootstrap behavior for reference and demo data in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java` and `backend/src/main/resources/application.properties`
- [x] T039 [US4] Document the containerized contributor workflow in `specs/002-connect-frontend-backend/quickstart.md` and `frontend/README.md`
- [x] T040 [US4] Add backend local-runbook notes and verification commands in `backend/README.md` and `scripts/verify-local-stack.sh`

**Checkpoint**: User Story 4 is complete when a contributor can start, restart, and verify the whole stack through Docker Compose without manual assembly.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Finish quality gates and shared behavior that span multiple stories.

- [x] T041 [P] Add cross-cutting frontend API error and retry helpers in `frontend/src/api/client.ts`, `frontend/src/components/ui/EmptyState.tsx`, and `frontend/src/components/ui/Dialog.tsx`
- [x] T042 [P] Add accessibility and responsive regressions for API-backed states in `frontend/tests/a11y/routes.axe.test.tsx` and `frontend/tests/responsive/viewports.test.tsx`
- [x] T043 [P] Add backend repository and service regression coverage in `backend/src/test/java/com/stocktracker/persistence/PortfolioTransactionRepositoryTest.java` and `backend/src/test/java/com/stocktracker/service/PortfolioServiceTest.java`
- [x] T044 Update verification documentation and final quality-gate commands in `specs/002-connect-frontend-backend/quickstart.md`, `frontend/README.md`, and `backend/README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and starts immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user-story work.
- **Phase 3: US1** depends on Phase 2 and is the MVP slice.
- **Phase 4: US2** depends on Phase 2; for least rework, schedule it after US1 stabilizes the portfolio API contracts.
- **Phase 5: US3** depends on Phase 3 because it extends the transaction API and portfolio refresh flow introduced for US1.
- **Phase 6: US4** depends on the stories needed for contributor verification, so it should follow US1 and preferably US2/US3.
- **Phase 7: Polish** depends on all desired user stories being complete.

### User Story Dependency Graph

```text
Setup -> Foundational -> US1 -> {US2, US3} -> US4 -> Polish
```

### Within Each User Story

- Write the story tests first and confirm they fail for the intended behavior gap.
- Implement backend services before REST resources.
- Implement API clients/stores before route-level UI integration.
- Do not mark a story complete until its independent test passes.

### Parallel Opportunities

- Setup tasks `T002` to `T005` can run in parallel after `T001`.
- Foundational tasks `T007`, `T008`, `T009`, `T011`, and `T012` can run in parallel after `T006`.
- After Phase 2, backend test tasks and frontend test tasks for the same story can run in parallel.
- Within US2, `T024`, `T025`, and `T027` can proceed in parallel once the shared API contracts are stable.
- Within US3, `T031` and `T033` can proceed in parallel while the preview/commit contract is fixed.
- Within US4, `T036`, `T037`, and `T038` can proceed in parallel.

---

## Parallel Example: User Story 1

```bash
Task: "T015 [US1] Add backend dashboard and transactions endpoint tests in backend/src/test/java/com/stocktracker/api/DashboardResourceTest.java and backend/src/test/java/com/stocktracker/api/TransactionsResourceTest.java"
Task: "T016 [US1] Add frontend dashboard and portfolio-store integration tests in frontend/tests/routes/DashboardRoute.test.tsx and frontend/tests/stores/portfolioStore.test.ts"

Task: "T017 [US1] Implement portfolio aggregation and transaction validation services in backend/src/main/java/com/stocktracker/service/PortfolioService.java and backend/src/main/java/com/stocktracker/service/TransactionValidationService.java"
Task: "T019 [US1] Replace local portfolio persistence with API-backed actions in frontend/src/stores/portfolioStore.ts and frontend/src/api/dashboardApi.ts"
```

## Parallel Example: User Story 2

```bash
Task: "T022 [US2] Add backend watchlist and instrument endpoint tests in backend/src/test/java/com/stocktracker/api/WatchlistResourceTest.java and backend/src/test/java/com/stocktracker/api/InstrumentResourceTest.java"
Task: "T023 [US2] Add frontend watchlist and analysis route tests in frontend/tests/routes/WatchlistsRoute.test.tsx, frontend/tests/routes/WatchlistDetailRoute.test.tsx, and frontend/tests/routes/AnalysisRoute.test.tsx"

Task: "T024 [US2] Implement watchlist service and mutation DTOs in backend/src/main/java/com/stocktracker/service/WatchlistService.java and backend/src/main/java/com/stocktracker/dto/WatchlistMutationRequest.java"
Task: "T025 [US2] Implement instrument analysis service and response DTOs in backend/src/main/java/com/stocktracker/service/InstrumentService.java and backend/src/main/java/com/stocktracker/dto/InstrumentAnalysisResponse.java"
Task: "T027 [US2] Replace persisted watchlist mutations with backend calls in frontend/src/stores/watchlistStore.ts and frontend/src/api/watchlistsApi.ts"
```

## Parallel Example: User Story 3

```bash
Task: "T029 [US3] Add backend CSV preview, commit, and export tests in backend/src/test/java/com/stocktracker/api/TransactionImportResourceTest.java and backend/src/test/java/com/stocktracker/api/TransactionExportResourceTest.java"
Task: "T030 [US3] Add frontend import/export flow tests in frontend/tests/features/transactions/ImportPreview.test.tsx, frontend/tests/features/transactions/ExportButton.test.tsx, and frontend/tests/routes/TransactionsRoute.test.tsx"

Task: "T031 [US3] Implement backend CSV parsing, normalization, and export services in backend/src/main/java/com/stocktracker/service/TransactionImportService.java, backend/src/main/java/com/stocktracker/service/TransactionExportService.java, and backend/src/main/java/com/stocktracker/dto/TransactionImportPreviewResponse.java"
Task: "T033 [US3] Connect the frontend transaction client to preview, commit, and export endpoints in frontend/src/api/transactionsApi.ts and frontend/src/lib/csv.ts"
```

## Parallel Example: User Story 4

```bash
Task: "T036 [US4] Add a local-stack smoke verification script in scripts/verify-local-stack.sh and scripts/wait-for-http.sh"
Task: "T037 [US4] Finalize Compose healthchecks, environment wiring, and persistent volumes in docker-compose.yml, backend/Dockerfile, and frontend/Dockerfile"
Task: "T038 [US4] Add development bootstrap behavior for reference and demo data in backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java and backend/src/main/resources/application.properties"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Validate the US1 independent test before expanding scope.

### Incremental Delivery

1. Deliver US1 to establish persisted portfolio state and dashboard correctness.
2. Deliver US2 to restore watchlists and analysis on shared data.
3. Deliver US3 to complete import/export on the new persistence model.
4. Deliver US4 to lock in the reproducible contributor workflow.
5. Finish with Phase 7 quality and regression coverage.

### Parallel Team Strategy

1. One contributor owns backend foundation (`T006` to `T011`) while another owns frontend API/test foundation (`T012` to `T014`).
2. After US1, split US2 and US3 between separate contributors because they touch different primary flows.
3. Reserve US4 and Phase 7 for integration hardening once the main product flows are stable.

---

## Notes

- `[P]` tasks are safe to split because they touch disjoint files or can proceed once a shared contract is fixed.
- The strict task format is used for every checklist item in this file.
- The suggested MVP scope is **User Story 1 only**.
- Do not start Polish until the desired story phases meet their independent test criteria.
