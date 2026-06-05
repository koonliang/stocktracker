# Tasks: Automated Web Regression Testing

**Input**: Design documents from `/specs/004-selenium-regression-tests/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: This feature *is* an automated test suite. The "implementation" tasks below produce the regression tests themselves; there is no separate test-of-the-test layer (per Simplicity / YAGNI).

**Organization**: Tasks are grouped by user story so each can be implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1 / US2 / US3 maps to the user stories in spec.md

## Path Conventions

- New isolated Maven project at repo-root `e2e/`
- Java sources under `e2e/src/test/java/com/stocktracker/e2e/`
- Frontend hooks under `frontend/src/`
- CI workflow at `.github/workflows/regression.yml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Stand up the isolated Java regression project.

- [X] T001 Create the `e2e/` project directory tree per plan.md (`e2e/src/test/java/com/stocktracker/e2e/{support,pages,journeys}`, `e2e/src/test/resources/`)
- [X] T002 Create `e2e/pom.xml`: Java 21, Selenium Java 4.x (built-in Selenium Manager), JUnit 5 (Jupiter), AssertJ, maven-surefire-plugin, and the Maven wrapper (`e2e/mvnw`)
- [X] T003 [P] Configure Spotless in `e2e/pom.xml` (match `backend/` style) and add `e2e/README.md` documenting how to run the suite locally and in CI

**Checkpoint**: `mvn -B -f e2e/pom.xml test-compile` succeeds on an empty suite.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Browser/driver harness every journey depends on. No journey work can begin until this is done.

**⚠️ CRITICAL**: Blocks all user stories.

- [X] T004 Implement `e2e/src/test/java/com/stocktracker/e2e/support/DriverFactory.java`: headless ChromeOptions (`--headless=new`, `--no-sandbox`, `--disable-dev-shm-usage`, `--disable-gpu`, window 1920x1080), with a `-De2e.headless=false` override for debugging
- [X] T005 [P] Implement `e2e/src/test/java/com/stocktracker/e2e/support/Waits.java`: explicit-wait helpers (visibility, presence, clickable) — no `Thread.sleep` (FR-007)
- [X] T006 Implement `e2e/src/test/java/com/stocktracker/e2e/support/BaseTest.java`: WebDriver lifecycle (create/quit per test), base URL from `-De2e.baseUrl` (default `http://localhost:5173`), navigate helper, and a pre-test wait for the app landing element (FR-010)
- [X] T007 Implement `e2e/src/test/java/com/stocktracker/e2e/journeys/SmokeTest.java`: load `/`, assert the app shell/title renders — a minimal gateable check that the stack is reachable

**Checkpoint**: With `docker compose up -d --wait` running, `mvn -B -f e2e/pom.xml test -Dtest=SmokeTest` passes headless.

---

## Phase 3: User Story 1 - Regression suite gates every pull request (Priority: P1) 🎯 MVP

**Goal**: The suite runs automatically and headless in CI on PR to `main`, push to `main`, and manual dispatch, and a failing test blocks the merge.

**Independent Test**: Open a PR; the workflow runs with no manual steps. Break the smoke check; the workflow fails and blocks merge. Revert; it passes.

- [X] T008 [US1] Create `.github/workflows/regression.yml`: `on` pull_request→main, push→main, `workflow_dispatch`; `concurrency` group `regression-${{ github.ref }}` cancel-in-progress; `permissions: contents: read`; job `web-regression` on `ubuntu-latest` with `actions/checkout@v4` and `actions/setup-java@v4` (temurin 21, cache maven)
- [X] T009 [US1] Add stack lifecycle steps to `.github/workflows/regression.yml`: `docker compose up -d --wait` (fails early on unhealthy services — FR-010), a short `http://localhost:5173` reachability poll, and an `if: always()` `docker compose down -v` teardown (ephemeral DB — FR-006)
- [X] T010 [US1] Add the run step `mvn -B -f e2e/pom.xml test` to `.github/workflows/regression.yml` so a non-zero exit fails the job and blocks the PR (FR-004); confirm wall-clock target under 10 min (SC-004)

**Checkpoint**: CI runs the smoke check on PR/push/manual and gates merges (Story 1 acceptance, FR-002/FR-003/FR-004).

---

## Phase 4: User Story 2 - Core user journeys are covered (Priority: P2)

**Goal**: End-to-end coverage of the four core journeys (dashboard, watchlist, analysis, CSV import/export) per `contracts/journeys.md`.

**Independent Test**: Against a known-good stack, all four journey tests pass; each exercises and asserts its flow.

### Stable selectors (frontend hooks — parallel, different files)

- [X] T011 [P] [US2] Add `data-testid="holdings-table"` and `data-testid="summary-tiles"` to the dashboard components under `frontend/src/features/dashboard/`
- [X] T012 [P] [US2] Add watchlist hooks (`data-testid="watchlist-add"`, `watchlist-item-<ticker>`, `watchlist-remove`) to the watchlist components under `frontend/src/features/watchlist/`
- [X] T013 [P] [US2] Add `data-testid="key-stats-grid"` to `frontend/src/features/analysis/` (KeyStatsGrid); reuse existing `data-testid="price-chart"`
- [X] T014 [P] [US2] Add transaction hooks (`data-testid="csv-import-input"`, `csv-export`, `transactions-table`) to the transactions components under `frontend/src/features/transactions/`

### Page Objects (parallel, different files)

- [X] T015 [P] [US2] Implement `e2e/src/test/java/com/stocktracker/e2e/pages/DashboardPage.java` (route `/`, holdings + summary locators/queries)
- [X] T016 [P] [US2] Implement `e2e/src/test/java/com/stocktracker/e2e/pages/WatchlistsPage.java` (routes `/watchlists`, `/watchlists/:id`; add/remove actions)
- [X] T017 [P] [US2] Implement `e2e/src/test/java/com/stocktracker/e2e/pages/AnalysisPage.java` (route `/analysis/:ticker`; chart + key-stats queries)
- [X] T018 [P] [US2] Implement `e2e/src/test/java/com/stocktracker/e2e/pages/TransactionsPage.java` (route `/transactions`; import/export/table actions; headless download prefs)

### Journey tests

- [X] T019 [P] [US2] Implement `e2e/.../journeys/DashboardJourneyTest.java` (J1): holdings table ≥1 row, summary tiles non-empty (Story 2 AS-1)
- [X] T020 [P] [US2] Implement `e2e/.../journeys/WatchlistJourneyTest.java` (J2): add instrument appears, remove instrument gone (Story 2 AS-2)
- [X] T021 [P] [US2] Implement `e2e/.../journeys/AnalysisJourneyTest.java` (J3): navigate to a seeded ticker, assert price chart + key stats visible (Story 2 AS-3)
- [X] T022 [US2] Add fixture `e2e/src/test/resources/transactions-sample.csv` and implement `e2e/.../journeys/CsvImportExportJourneyTest.java` (J4): import rows appear in table, export succeeds (Story 2 AS-4)

**Checkpoint**: All four journeys pass headless against the compose stack (FR-005, SC-002).

---

## Phase 5: User Story 3 - Failures are easy to diagnose (Priority: P3)

**Goal**: Failed runs produce a readable report plus a point-of-failure screenshot, surfaced from CI.

**Independent Test**: Force a journey failure; a screenshot is written and the CI run exposes the report + screenshot artifacts.

- [X] T023 [US3] Implement `e2e/src/test/java/com/stocktracker/e2e/support/ScreenshotOnFailure.java`: JUnit 5 `TestWatcher`/extension that writes a full-page PNG to `e2e/target/screenshots/<TestName>.png` on failure (FR-009)
- [X] T024 [US3] Wire `ScreenshotOnFailure` into `BaseTest.java` (and ensure the failing test's WebDriver is still available when the screenshot is taken)
- [X] T025 [US3] Add `actions/upload-artifact@v4` step (`if: always()`) to `.github/workflows/regression.yml` for `e2e/target/surefire-reports/**` (FR-008)
- [X] T026 [US3] Add `actions/upload-artifact@v4` step (`if: always()`) for `e2e/target/screenshots/**` and a `docker compose logs` dump (`if: failure()`) to `.github/workflows/regression.yml` (SC-005)

**Checkpoint**: A forced failure yields a screenshot + report downloadable from the CI run (Story 3 acceptance).

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T027 [P] Run `mvn -B -f e2e/pom.xml spotless:check` and resolve formatting; ensure `frontend` `npm run lint` still passes after the `data-testid` additions (Constitution II/III)
- [X] T028 [P] Update `e2e/README.md` and verify `specs/004-selenium-regression-tests/quickstart.md` steps run end-to-end locally
- [X] T029 Review suite stability: confirm all waits are explicit (no `Thread.sleep`) and full-suite wall-clock is under 10 minutes (SC-004/SC-006)
- [X] T030 Add Allure HTML reporting: `allure-junit5` + `allure-maven` in `e2e/pom.xml`, attach failure screenshots to Allure from `ScreenshotOnFailure.java`, and add `allure:report` generate + `allure-report` artifact-upload steps to `.github/workflows/regression.yml`

---

## Dependencies & Execution Order

- **Setup (Phase 1)** → blocks everything.
- **Foundational (Phase 2)** → blocks all user stories (provides DriverFactory/BaseTest/Waits + smoke).
- **US1 (Phase 3)** → depends on Foundational (gates on SmokeTest). Independently deliverable MVP.
- **US2 (Phase 4)** → depends on Foundational; independent of US1 (journeys run locally without the workflow). The workflow from US1 will pick them up automatically.
- **US3 (Phase 5)** → depends on US1 (workflow exists to add upload steps) and is most meaningful after US2 (real journeys to screenshot); T023/T024 can be built right after Foundational.
- **Polish (Phase 6)** → after the stories it touches.

## Parallel Opportunities

- Phase 4 frontend hooks T011–T014 are fully parallel (different feature folders).
- Phase 4 page objects T015–T018 are parallel (different files), after their hooks land.
- Phase 4 journey tests T019–T021 are parallel; T022 adds a resource file.
- T005 parallel with T004/T006 within Foundational.

## Implementation Strategy

- **MVP = Phase 1 + Phase 2 + Phase 3 (US1)**: a headless smoke check gating PR/push/manual runs — delivers the core "block regressions in CI" value immediately.
- **Increment 2 = Phase 4 (US2)**: real coverage of the four journeys.
- **Increment 3 = Phase 5 (US3)**: diagnostics for fast triage.
- Then Phase 6 polish.
