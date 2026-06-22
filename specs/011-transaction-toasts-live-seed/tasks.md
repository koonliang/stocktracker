# Tasks: CRUD Toast Feedback and Live Seed Accuracy

**Input**: Design documents from `/specs/011-transaction-toasts-live-seed/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Automated test tasks are required for feature behavior. Completion is verified manually against the acceptance scenarios in `specs/011-transaction-toasts-live-seed/quickstart.md`.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: User story label (`[US1]`, `[US2]`, `[US3]`)
- Every task includes an exact file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish shared feedback utilities and test support used across the feature

- [X] T001 Create shared action-feedback descriptors and toast copy helpers in `frontend/src/lib/actionFeedback.ts`
- [X] T002 [P] Create reusable toast assertion helpers for frontend tests in `frontend/tests/support/actionFeedback.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared plumbing that MUST exist before user story work starts

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [X] T003 Refactor shared toast dispatch helpers in `frontend/src/stores/toastStore.ts` to support consistent CRUD/import/export action feedback
- [X] T004 [P] Normalize API error-to-feedback message mapping in `frontend/src/api/client.ts` and `frontend/src/stores/portfolioStore.ts`

**Checkpoint**: Shared action-feedback infrastructure is ready for story implementation

---

## Phase 3: User Story 1 - Receive Immediate Feedback for Add, Update, and Delete Actions (Priority: P1) 🎯 MVP

**Goal**: Show exactly one success or failure toast after every authenticated add/update/delete action across transactions, watchlists, watchlist tickers, and alerts

**Independent Test**: Perform representative create/update/delete actions for transactions, watchlists, watchlist tickers, and alerts and confirm exactly one clear toast appears after each completed action

### Tests for User Story 1 (REQUIRED) ⚠️

- [X] T005 [P] [US1] Extend transaction CRUD toast coverage in `frontend/tests/stores/portfolioStore.test.ts`
- [X] T006 [P] [US1] Add watchlist create/rename/delete and ticker add/remove toast coverage in `frontend/tests/stores/watchlistStore.test.ts`, `frontend/tests/routes/WatchlistsRoute.test.tsx`, and `frontend/tests/routes/WatchlistDetailRoute.test.tsx`
- [X] T007 [P] [US1] Add alert CRUD toast coverage in `frontend/tests/routes/AlertsRoute.test.tsx`
- [X] T008 [P] [US1] Add shared action-feedback rendering coverage in `frontend/tests/components/layout/NotificationToaster.test.tsx`

### Implementation for User Story 1

- [X] T009 [US1] Wire transaction create/delete success and failure toasts in `frontend/src/stores/portfolioStore.ts` and `frontend/src/routes/TransactionsRoute.tsx`
- [X] T010 [US1] Wire watchlist create/rename/delete success and failure toasts in `frontend/src/stores/watchlistStore.ts`, `frontend/src/features/watchlist/NewWatchlistDialog.tsx`, and `frontend/src/features/watchlist/WatchlistHeader.tsx`
- [X] T011 [US1] Wire watchlist ticker add/remove success and failure toasts in `frontend/src/stores/watchlistStore.ts`, `frontend/src/routes/WatchlistDetailRoute.tsx`, and `frontend/src/features/watchlist/WatchlistRow.tsx`
- [X] T012 [US1] Add alert update UI and wire alert create/update/delete success and failure toasts in `frontend/src/routes/AlertsRoute.tsx` and `frontend/src/api/alertsApi.ts`

**Checkpoint**: User Story 1 is fully functional and independently testable

---

## Phase 4: User Story 2 - Understand Transaction Import and Export Results (Priority: P2)

**Goal**: Show accurate toast summaries for transaction import and export completion outcomes

**Independent Test**: Import valid and partially invalid transaction files and export the current ledger; confirm the resulting toast clearly summarizes success, partial success, or failure

### Tests for User Story 2 (REQUIRED) ⚠️

- [X] T013 [P] [US2] Extend import/export toast behavior coverage in `frontend/tests/stores/portfolioStore.test.ts` and `frontend/tests/routes/TransactionsRoute.test.tsx`
- [X] T014 [P] [US2] Add import/export action-feedback coverage in `frontend/tests/features/transactions/ImportPreview.test.tsx` and `frontend/tests/features/transactions/ExportButton.test.tsx`

### Implementation for User Story 2

- [X] T015 [US2] Emit import commit success, partial-result, and failure toasts in `frontend/src/stores/portfolioStore.ts` and `frontend/src/features/transactions/ImportPreview.tsx`
- [X] T016 [US2] Emit export success and failure toasts in `frontend/src/features/transactions/ExportButton.tsx` and `frontend/src/api/transactionsApi.ts`

**Checkpoint**: User Stories 1 and 2 both work independently, with import/export feedback now covered

---

## Phase 5: User Story 3 - See Demo Portfolio Data Match the Live Provider (Priority: P2)

**Goal**: Keep seeded/demo transactions deterministic while refreshing seeded quote-backed values from the active live provider when live-provider mode is enabled

**Independent Test**: Start the backend once in stub mode and once in live-provider mode, load the same seeded/demo account, and confirm the portfolio remains usable while seeded valuations switch to live-backed values only in live mode

### Tests for User Story 3 (REQUIRED) ⚠️

- [X] T017 [P] [US3] Add seeded live-provider bootstrap coverage in `backend/src/test/java/com/stocktracker/service/DemoUserServiceTest.java` and `backend/src/test/java/com/stocktracker/api/DashboardResourceTest.java`
- [X] T018 [P] [US3] Add stub-mode and live-fallback coverage for seeded quote refresh in `backend/src/test/java/com/stocktracker/service/QuoteRefreshJobTest.java` and `backend/src/test/java/com/stocktracker/api/DemoUserAuthResourceTest.java`

### Implementation for User Story 3

- [X] T019 [US3] Collect seeded symbols and trigger provider-aware quote refresh during bootstrap in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java`
- [X] T020 [US3] Reuse existing market-data and quote-cache services for seeded live refresh in `backend/src/main/java/com/stocktracker/service/MarketDataService.java` and `backend/src/main/java/com/stocktracker/service/QuoteCacheService.java`
- [X] T021 [US3] Preserve deterministic stub-mode behavior and graceful live fallback in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java` and `backend/src/main/java/com/stocktracker/service/provider/ProviderConfig.java`

**Checkpoint**: All three user stories are independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, documentation alignment, and cross-story cleanup

- [ ] T022 [P] Update developer verification guidance for CRUD feedback and live-seed mode in `backend/README.md` and `frontend/README.md`
- [ ] T023 Run the manual smoke flow and verification gates documented in `specs/011-transaction-toasts-live-seed/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup**: No dependencies
- **Phase 2: Foundational**: Depends on Phase 1 and blocks all user stories
- **Phase 3: User Story 1**: Depends on Phase 2
- **Phase 4: User Story 2**: Depends on Phase 2; recommended after User Story 1 because both touch transaction feedback files
- **Phase 5: User Story 3**: Depends on Phase 2; independent of User Stories 1 and 2 once shared groundwork is done
- **Phase 6: Polish**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: MVP and first delivery slice after foundational work
- **US2 (P2)**: Extends transaction feedback and should not begin until shared toast plumbing is stable
- **US3 (P2)**: Backend-only seed/runtime slice; can run in parallel with US2 after foundational work

### Within Each User Story

- Automated tests are created before or alongside implementation
- Shared helpers before route/store wiring
- Store/service changes before route/component integration
- Manual verification after automated test coverage is in place

### Parallel Opportunities

- **Setup**: `T002` can run in parallel with `T001`
- **Foundational**: `T004` can run in parallel with `T003`
- **US1**: `T005`, `T006`, `T007`, and `T008` can run in parallel; `T010` and `T012` can proceed in parallel after `T009`
- **US2**: `T013` and `T014` can run in parallel
- **US3**: `T017` and `T018` can run in parallel
- **Polish**: `T022` can run in parallel with final verification prep for `T023`

---

## Parallel Example: User Story 1

```bash
# Launch the automated coverage tasks together:
Task: "Extend transaction CRUD toast coverage in frontend/tests/stores/portfolioStore.test.ts"
Task: "Add watchlist create/rename/delete and ticker add/remove toast coverage in frontend/tests/stores/watchlistStore.test.ts, frontend/tests/routes/WatchlistsRoute.test.tsx, and frontend/tests/routes/WatchlistDetailRoute.test.tsx"
Task: "Add alert CRUD toast coverage in frontend/tests/routes/AlertsRoute.test.tsx"
Task: "Add shared action-feedback rendering coverage in frontend/tests/components/layout/NotificationToaster.test.tsx"

# After shared plumbing is ready, split implementation by surface:
Task: "Wire watchlist create/rename/delete success and failure toasts in frontend/src/stores/watchlistStore.ts, frontend/src/features/watchlist/NewWatchlistDialog.tsx, and frontend/src/features/watchlist/WatchlistHeader.tsx"
Task: "Add alert update UI and wire alert create/update/delete success and failure toasts in frontend/src/routes/AlertsRoute.tsx and frontend/src/api/alertsApi.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. Validate CRUD toast behavior manually from `specs/011-transaction-toasts-live-seed/quickstart.md`
5. Demo or ship the MVP slice

### Incremental Delivery

1. Finish Setup + Foundational
2. Deliver **US1** for core CRUD feedback
3. Add **US2** for transaction import/export summaries
4. Add **US3** for live-provider-backed seeded valuations
5. Finish with docs and full verification

### Parallel Team Strategy

1. One developer completes Setup + Foundational
2. Then split by slice:
   Developer A: US1 CRUD toast flows
   Developer B: US2 import/export feedback
   Developer C: US3 backend seed/runtime behavior
3. Rejoin for Phase 6 verification and documentation

---

## Notes

- All tasks follow the required checklist format with IDs, labels, and file paths
- `[P]` tasks touch different files or can be executed without waiting on another incomplete task
- User Story 1 is the suggested MVP scope
- User Story 3 is operationally independent but should still wait for Phase 2 shared groundwork
