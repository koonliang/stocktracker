# Tasks: Alert Notifications and Currency Views

**Input**: Design documents from `/specs/007-alert-currency-dashboard/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md
**Tests**: Required by StockTracker Constitution Principles I-III; write test tasks before implementation tasks for each story.
**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Each task includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared fixtures, docs, and implementation touchpoints used by all stories.

- [ ] T001 Update backend stub quote and FX fixture coverage for AAPL crossing and stale FX scenarios in backend/src/main/resources/provider-stub/quotes.json and backend/src/main/resources/provider-stub/fx-rates.json
- [ ] T002 [P] Add notification dialog MSW fixture responses for unread, read, empty, and error states in frontend/src/test/msw/handlers.ts
- [ ] T003 [P] Add shared frontend notification fixture builders in frontend/src/test/fixtures/notifications.ts
- [ ] T004 [P] Add backend test data helper methods for alerts, notifications, FX rates, and legacy transactions in backend/src/test/java/com/stocktracker/support/IntegrationTestSupport.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core schema, DTO, repository, and API primitives that all user stories depend on.

**CRITICAL**: No user story work should begin until this phase is complete.

- [ ] T005 Create Flyway migration for notification crossing fields, transaction currency source fields, legacy backfill, and required indexes in backend/src/main/resources/db/migration/V5__alert_currency_dashboard_refinements.sql
- [ ] T006 Update Alert entity crossing/re-arm fields in backend/src/main/java/com/stocktracker/domain/Alert.java
- [ ] T007 Update Notification entity with trigger snapshot fields and crossing key in backend/src/main/java/com/stocktracker/domain/Notification.java
- [ ] T008 Update PortfolioTransaction entity with currencySource and currencyBackfilledAt in backend/src/main/java/com/stocktracker/domain/PortfolioTransaction.java
- [ ] T009 [P] Extend NotificationRepository query methods for newest-first history, unread count, mark-all-read, and delete-by-alert in backend/src/main/java/com/stocktracker/persistence/NotificationRepository.java
- [ ] T010 [P] Extend PortfolioTransactionRepository query methods for missing-currency backfill candidates in backend/src/main/java/com/stocktracker/persistence/PortfolioTransactionRepository.java
- [ ] T011 [P] Add notification DTOs for history list, unread count, read-all response, and delete contract in backend/src/main/java/com/stocktracker/dto/NotificationDtos.java
- [ ] T012 [P] Add conversion metadata DTOs for fxDate and fxStatus in backend/src/main/java/com/stocktracker/dto/ConversionDtos.java
- [ ] T013 [P] Update frontend API shared types for notifications, conversion metadata, and transaction currency source in frontend/src/api/types.ts
- [ ] T014 [P] Update frontend formatting helpers for stale/unavailable conversion labels in frontend/src/lib/format.ts

**Checkpoint**: Database, DTO, repository, and shared type foundations are ready.

---

## Phase 3: User Story 1 - Approve Notification Dialog Mockup (Priority: P1) MVP

**Goal**: Provide a reviewable frontend notification dialog mockup that matches the spec wireframe and covers filled, empty, unread, and overflow states.

**Independent Test**: Render the dialog with representative fixtures and verify title, unread count, rows, empty state, scroll behavior, and actions are visible and accessible.

### Tests for User Story 1

- [ ] T015 [P] [US1] Add component tests for notification dialog filled, empty, overflow, and action visibility states in frontend/src/features/alerts/NotificationDialog.test.tsx
- [ ] T016 [P] [US1] Add accessibility tests for focus trap, keyboard close, and labeled actions in frontend/src/features/alerts/NotificationDialog.a11y.test.tsx

### Implementation for User Story 1

- [ ] T017 [P] [US1] Implement NotificationDialog component using existing Dialog, Button, Badge, EmptyState, and Table primitives in frontend/src/features/alerts/NotificationDialog.tsx
- [ ] T018 [P] [US1] Implement notification row rendering with read/unread state and stable data-testid values in frontend/src/features/alerts/NotificationRow.tsx
- [ ] T019 [US1] Add notification dialog trigger with unread badge to the top bar in frontend/src/components/layout/TopBar.tsx
- [ ] T020 [US1] Wire NotificationDialog into the application shell without route navigation in frontend/src/components/layout/AppShell.tsx
- [ ] T021 [US1] Add static mock/demo fixture support for stakeholder review in frontend/src/features/alerts/NotificationDialog.mock.ts

**Checkpoint**: User Story 1 is independently reviewable and testable as a frontend mockup.

---

## Phase 4: User Story 2 - Review Triggered Alerts (Priority: P2)

**Goal**: Let users review triggered alert notifications, mark one/all as read, delete history, and receive one notification per threshold crossing.

**Independent Test**: Create active alerts, simulate quote crossings, open the dialog, verify notification content/history/read/delete behavior, and confirm re-arm only after condition clears and crosses again.

### Tests for User Story 2

- [ ] T022 [P] [US2] Add backend API tests for GET /api/notifications pagination, unread filtering, ownership, empty state, and ordering in backend/src/test/java/com/stocktracker/api/NotificationsResourceTest.java
- [ ] T023 [P] [US2] Add backend API tests for POST /api/notifications/{id}/read, POST /api/notifications/read-all, and DELETE /api/notifications/{id} in backend/src/test/java/com/stocktracker/api/NotificationsResourceTest.java
- [ ] T024 [P] [US2] Add alert crossing and re-arm service tests for once-per-crossing behavior in backend/src/test/java/com/stocktracker/service/AlertEvaluationTest.java
- [ ] T025 [P] [US2] Add frontend API/store tests for notification history, unread count, mark-read, mark-all-read, and delete flows in frontend/src/stores/notificationsStore.test.ts
- [ ] T026 [P] [US2] Add notification dialog integration tests for loading data, marking read, deleting, and empty state after deletion in frontend/src/features/alerts/NotificationDialog.integration.test.tsx

### Implementation for User Story 2

- [ ] T027 [US2] Implement NotificationService history, unread count, mark-read, mark-all-read, delete, and delete-by-alert behavior in backend/src/main/java/com/stocktracker/service/NotificationService.java
- [ ] T028 [US2] Implement NotificationsResource endpoints for list, mark-read, mark-all-read, and delete in backend/src/main/java/com/stocktracker/api/NotificationsResource.java
- [ ] T029 [US2] Update AlertEvaluationService to create one notification per crossing key and re-arm only after condition clears in backend/src/main/java/com/stocktracker/service/AlertEvaluationService.java
- [ ] T030 [US2] Update AlertService delete behavior to remove associated notification history in backend/src/main/java/com/stocktracker/service/AlertService.java
- [ ] T031 [US2] Update backend notification DTO mapping with symbol, condition, threshold, observed value, triggeredAt, read state, and message in backend/src/main/java/com/stocktracker/dto/NotificationDtos.java
- [ ] T032 [US2] Implement frontend notifications API methods for list, mark-read, mark-all-read, and delete in frontend/src/api/notificationsApi.ts
- [ ] T033 [US2] Implement notificationsStore for dialog history, unread count, optimistic read/delete updates, and error state in frontend/src/stores/notificationsStore.ts
- [ ] T034 [US2] Connect NotificationDialog actions to notificationsStore and backend APIs in frontend/src/features/alerts/NotificationDialog.tsx
- [ ] T035 [US2] Update NotificationToaster to share unread state with notificationsStore without duplicate toasts in frontend/src/components/layout/NotificationToaster.tsx
- [ ] T036 [US2] Add Selenium e2e coverage for alert crossing, dialog history, mark-read, mark-all-read, delete, and re-arm behavior in e2e/src/test/java/com/stocktracker/e2e/journeys/AlertsTest.java

**Checkpoint**: User Story 2 is fully functional and independently testable.

---

## Phase 5: User Story 3 - Record Transaction Currency (Priority: P3)

**Goal**: Require, default, validate, import, export, display, and backfill transaction currency for all monetary transactions.

**Independent Test**: Add/edit/import/export transactions across multiple transaction types and verify valid currency is preserved or row-level validation is shown.

### Tests for User Story 3

- [ ] T037 [P] [US3] Add backend transaction validation tests for required/defaulted currency and unsupported currency errors in backend/src/test/java/com/stocktracker/service/TransactionValidationServiceTest.java
- [ ] T038 [P] [US3] Add backend legacy currency backfill tests for instrument-linked and cash-only transactions in backend/src/test/java/com/stocktracker/service/TransactionCurrencyBackfillTest.java
- [ ] T039 [P] [US3] Add backend CSV import/export currency round-trip tests in backend/src/test/java/com/stocktracker/service/CsvImportV2Test.java
- [ ] T040 [P] [US3] Add frontend transaction form currency field tests for security and cash transaction types in frontend/src/features/transactions/TransactionForm.test.tsx
- [ ] T041 [P] [US3] Add frontend import preview tests for missing/unsupported currency row-level errors in frontend/src/features/transactions/ImportPreview.test.tsx

### Implementation for User Story 3

- [ ] T042 [US3] Implement currency source and legacy backfill service logic in backend/src/main/java/com/stocktracker/service/TransactionCurrencyBackfillService.java
- [ ] T043 [US3] Update TransactionValidationService to enforce currency rules for buy, sell, dividend, fee, deposit, withdrawal, and split rows in backend/src/main/java/com/stocktracker/service/TransactionValidationService.java
- [ ] T044 [US3] Update TransactionsResource create/update mapping to include currency and currencySource in backend/src/main/java/com/stocktracker/api/TransactionsResource.java
- [ ] T045 [US3] Update TransactionRequest and TransactionResponse DTOs with currency and currencySource fields in backend/src/main/java/com/stocktracker/dto/TransactionRequest.java and backend/src/main/java/com/stocktracker/dto/TransactionResponse.java
- [ ] T046 [US3] Update TransactionImportService to parse, default, validate, and preview currency per row in backend/src/main/java/com/stocktracker/service/TransactionImportService.java
- [ ] T047 [US3] Update TransactionExportService to always export resolved currency in the v2 CSV header in backend/src/main/java/com/stocktracker/service/TransactionExportService.java
- [ ] T048 [US3] Update frontend transaction API types and mapping for currencySource in frontend/src/api/transactionsApi.ts
- [ ] T049 [US3] Update TransactionForm currency controls, defaults, and validation messages in frontend/src/features/transactions/TransactionForm.tsx
- [ ] T050 [US3] Update TransactionsTable to display transaction monetary values with transaction currency in frontend/src/features/transactions/TransactionsTable.tsx
- [ ] T051 [US3] Update ImportPreview to show currency values and row-level validation issues in frontend/src/features/transactions/ImportPreview.tsx
- [ ] T052 [US3] Update ExportButton expectations for v2 currency export in frontend/src/features/transactions/ExportButton.tsx
- [ ] T053 [US3] Add Selenium e2e coverage for transaction currency entry, v1/v2 import, validation, and export round-trip in e2e/src/test/java/com/stocktracker/e2e/journeys/CsvImportExportJourneyTest.java

**Checkpoint**: User Story 3 is fully functional and independently testable.

---

## Phase 6: User Story 4 - View Dashboard and Performance in Base Currency (Priority: P4)

**Goal**: Let users choose a base currency and see dashboard/performance values converted using deterministic FX date rules and stale/unavailable conversion indicators.

**Independent Test**: Set a base currency, use mixed-currency holdings/transactions, and verify dashboard/performance monetary values, percentage consistency, preference persistence, and stale FX indicators.

### Tests for User Story 4

- [ ] T054 [P] [US4] Add backend CurrencyService tests for transaction-date FX, valuation-date FX, latest-prior stale fallback, and unavailable status in backend/src/test/java/com/stocktracker/service/CurrencyServiceTest.java
- [ ] T055 [P] [US4] Add backend dashboard conversion metadata tests in backend/src/test/java/com/stocktracker/api/DashboardResourceTest.java
- [ ] T056 [P] [US4] Add backend performance conversion metadata and percentage consistency tests in backend/src/test/java/com/stocktracker/api/PerformanceResourceTest.java
- [ ] T057 [P] [US4] Add frontend BaseCurrencySelect persistence and refetch tests in frontend/src/components/layout/BaseCurrencySelect.test.tsx
- [ ] T058 [P] [US4] Add frontend dashboard stale/unavailable FX indicator tests in frontend/src/features/dashboard/SummaryTiles.test.tsx and frontend/src/features/dashboard/HoldingsTable.test.tsx
- [ ] T059 [P] [US4] Add frontend performance conversion status tests in frontend/src/routes/PerformanceRoute.test.tsx

### Implementation for User Story 4

- [ ] T060 [US4] Update CurrencyService conversion methods to return amount, fxDate, and fxStatus for transaction-date and valuation-date calls in backend/src/main/java/com/stocktracker/service/CurrencyService.java
- [ ] T061 [US4] Update DashboardResponse DTOs to include baseCurrency, native amount, converted amount, fxDate, fxStatus, and warnings in backend/src/main/java/com/stocktracker/dto/DashboardResponse.java
- [ ] T062 [US4] Update PerformanceResponse DTOs to include conversion metadata for realized lots, income events, current holdings, and contribution values in backend/src/main/java/com/stocktracker/dto/PerformanceResponse.java
- [ ] T063 [US4] Update DashboardResource and PortfolioService to use valuation-date FX for holdings and expose conversion warnings in backend/src/main/java/com/stocktracker/api/DashboardResource.java and backend/src/main/java/com/stocktracker/service/PortfolioService.java
- [ ] T064 [US4] Update PerformanceResource and PerformanceService to use transaction-date FX for transaction values and valuation-date FX for current holdings in backend/src/main/java/com/stocktracker/api/PerformanceResource.java and backend/src/main/java/com/stocktracker/service/PerformanceService.java
- [ ] T065 [US4] Update SettingsService base-currency persistence/refetch behavior if needed for dashboard/performance invalidation in backend/src/main/java/com/stocktracker/service/SettingsService.java
- [ ] T066 [US4] Update frontend dashboard API types and mapping for conversion metadata in frontend/src/api/dashboardApi.ts
- [ ] T067 [US4] Update frontend performance API types and mapping for conversion metadata in frontend/src/api/performanceApi.ts
- [ ] T068 [US4] Update BaseCurrencySelect to refetch dashboard and performance data after preference changes in frontend/src/components/layout/BaseCurrencySelect.tsx
- [ ] T069 [US4] Update SummaryTiles and HoldingsTable to render base currency, native currency, stale FX, and unavailable FX indicators in frontend/src/features/dashboard/SummaryTiles.tsx and frontend/src/features/dashboard/HoldingsTable.tsx
- [ ] T070 [US4] Update PerformanceRoute to render base currency, conversion status, stale FX, and unavailable FX indicators in frontend/src/routes/PerformanceRoute.tsx
- [ ] T071 [US4] Add Selenium e2e coverage for base-currency switching, stale FX display, and dashboard/performance consistency in e2e/src/test/java/com/stocktracker/e2e/journeys/PerformanceTest.java

**Checkpoint**: User Story 4 is fully functional and independently testable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verify gates, documentation, and cross-story behavior.

- [ ] T072 [P] Update backend API and CSV examples for notification history, transaction currency, and conversion status in README.md
- [ ] T073 [P] Update quickstart smoke validation notes after implementation in specs/007-alert-currency-dashboard/quickstart.md
- [ ] T074 Run backend verification gate from backend/pom.xml using cd backend && ./mvnw -B verify
- [ ] T075 Run frontend verification gate from frontend/package.json using cd frontend && npm run verify
- [ ] T076 Run e2e verification gate from e2e/pom.xml using cd e2e && ./mvnw -B test
- [ ] T077 Review UI text wrapping and responsive dialog layout at mobile and desktop widths in frontend/src/features/alerts/NotificationDialog.tsx
- [ ] T078 Confirm no unrelated changes were introduced and review final git diff for AGENTS.md, specs/007-alert-currency-dashboard/tasks.md, backend/src, frontend/src, and e2e/src

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Phase 1 and blocks all story implementation.
- **US1 (Phase 3)**: depends on Phase 2; provides the frontend dialog mockup MVP.
- **US2 (Phase 4)**: depends on Phase 2 and integrates with the US1 dialog surface.
- **US3 (Phase 5)**: depends on Phase 2; can run in parallel with US2 after shared schema/DTO foundations.
- **US4 (Phase 6)**: depends on Phase 2 and benefits from US3 currency enforcement for full end-to-end correctness.
- **Polish (Phase 7)**: depends on all desired story phases.

### User Story Dependencies

- **US1**: independent after foundation; suggested MVP.
- **US2**: can start after foundation but uses the dialog shell from US1 for full UX completion.
- **US3**: independent after foundation.
- **US4**: can start after foundation; final acceptance should include US3 transaction-currency behavior.

### Parallel Opportunities

- Setup fixtures T002-T004 can run in parallel.
- Foundational repository/DTO/frontend type tasks T009-T014 can run in parallel after schema/entity changes are understood.
- Test tasks inside each story can run in parallel because they touch separate backend/frontend/e2e files.
- US2 backend API/service work and frontend store/dialog work can proceed in parallel after DTO contracts are stable.
- US3 backend validation/import/export and frontend form/import/table work can proceed in parallel after DTO contracts are stable.
- US4 backend conversion services/resources and frontend dashboard/performance rendering can proceed in parallel after conversion DTOs are stable.

---

## Parallel Execution Examples

### US1

```text
Task: "T015 [P] [US1] Add component tests in frontend/src/features/alerts/NotificationDialog.test.tsx"
Task: "T016 [P] [US1] Add accessibility tests in frontend/src/features/alerts/NotificationDialog.a11y.test.tsx"
Task: "T017 [P] [US1] Implement NotificationDialog in frontend/src/features/alerts/NotificationDialog.tsx"
Task: "T018 [P] [US1] Implement NotificationRow in frontend/src/features/alerts/NotificationRow.tsx"
```

### US2

```text
Task: "T022 [P] [US2] Add notification API tests in backend/src/test/java/com/stocktracker/api/NotificationsResourceTest.java"
Task: "T024 [P] [US2] Add alert crossing tests in backend/src/test/java/com/stocktracker/service/AlertEvaluationTest.java"
Task: "T025 [P] [US2] Add store tests in frontend/src/stores/notificationsStore.test.ts"
Task: "T026 [P] [US2] Add dialog integration tests in frontend/src/features/alerts/NotificationDialog.integration.test.tsx"
```

### US3

```text
Task: "T037 [P] [US3] Add transaction validation tests in backend/src/test/java/com/stocktracker/service/TransactionValidationServiceTest.java"
Task: "T039 [P] [US3] Add CSV tests in backend/src/test/java/com/stocktracker/service/CsvImportV2Test.java"
Task: "T040 [P] [US3] Add form tests in frontend/src/features/transactions/TransactionForm.test.tsx"
Task: "T041 [P] [US3] Add import preview tests in frontend/src/features/transactions/ImportPreview.test.tsx"
```

### US4

```text
Task: "T054 [P] [US4] Add CurrencyService tests in backend/src/test/java/com/stocktracker/service/CurrencyServiceTest.java"
Task: "T055 [P] [US4] Add dashboard API tests in backend/src/test/java/com/stocktracker/api/DashboardResourceTest.java"
Task: "T057 [P] [US4] Add BaseCurrencySelect tests in frontend/src/components/layout/BaseCurrencySelect.test.tsx"
Task: "T059 [P] [US4] Add PerformanceRoute tests in frontend/src/routes/PerformanceRoute.test.tsx"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 setup.
2. Complete Phase 2 foundation.
3. Complete Phase 3 US1 notification dialog mockup.
4. Stop and validate the dialog mockup with component/a11y tests and stakeholder review.

### Incremental Delivery

1. Deliver US1 dialog mockup.
2. Add US2 notification history, mark-read/delete, and re-arm behavior.
3. Add US3 transaction currency enforcement/import/export/backfill.
4. Add US4 dashboard/performance base-currency conversion metadata.
5. Run all verification gates in Phase 7.

### Quality Gates

Before the feature is complete:
- Backend must pass `cd backend && ./mvnw -B verify`.
- Frontend must pass `cd frontend && npm run verify`.
- E2E must pass `cd e2e && ./mvnw -B test`.

## Notes

- Tasks marked [P] touch different files and can be worked independently.
- User-story labels map directly to the four stories in spec.md.
- Tests are intentionally included because the project constitution makes test verification non-negotiable.
- Keep feature-006 provider abstractions intact; this feature is a behavior/UI/contract refinement layer.
