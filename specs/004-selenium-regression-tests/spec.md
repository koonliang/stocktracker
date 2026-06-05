# Feature Specification: Automated Web Regression Testing

**Feature Branch**: `004-selenium-regression-tests`
**Created**: 2026-06-05
**Status**: Draft
**Input**: User description: "for regression testing, i want to use selenium with java for automated web testing; this should be triggered as part of ci pipeline (run in headless mode)"

## Clarifications

### Session 2026-06-05

- Q: When should the regression suite be triggered in CI? → A: On pull requests targeting main AND on push to main.
- Q: Where should the database for regression testing live? → A: Ephemeral MySQL container spun up inside the CI job via the existing docker-compose stack (fresh, deterministic, torn down after the run).
- Q: Should the suite also be runnable manually? → A: Yes — also allow manual triggering via workflow_dispatch.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Regression suite gates every pull request (Priority: P1)

The team needs confidence that changes to the application do not break existing
behaviour. On every pull request, an automated browser-based regression suite
runs against a running instance of the application without any human interaction
and without a visible browser window. If a core user journey breaks, the pull
request is blocked until the regression is fixed.

**Why this priority**: This is the core value of the feature. Catching
regressions automatically before merge is what prevents broken behaviour from
reaching the main branch. Without CI integration the tests provide little
ongoing protection.

**Independent Test**: Open a pull request that intentionally breaks a core flow
(for example, a dashboard element that no longer renders). The pipeline must run
the regression suite, fail, and block the merge. Reverting the breakage makes
the pipeline pass.

**Acceptance Scenarios**:

1. **Given** a pull request against the main branch, **When** the pipeline runs, **Then** the regression suite executes automatically in headless mode without manual steps.
2. **Given** all core user journeys behave correctly, **When** the regression suite finishes, **Then** the pipeline reports success and the pull request is mergeable.
3. **Given** a regression in a core user journey, **When** the regression suite runs, **Then** at least one test fails and the pipeline blocks the merge.

---

### User Story 2 - Core user journeys are covered (Priority: P2)

The regression suite exercises the application's primary end-to-end flows
through the browser the way a real user would, validating that each flow
completes successfully and shows the expected outcome.

**Why this priority**: The protection from Story 1 is only meaningful if the
suite actually covers the journeys that matter. The four core flows are the
product's main value.

**Independent Test**: Run the suite against a known-good build and confirm each
core journey (portfolio dashboard, watchlist management, stock analysis, CSV
import/export) is exercised and asserted, with all checks passing.

**Acceptance Scenarios**:

1. **Given** a running application, **When** the suite runs the dashboard journey, **Then** holdings and portfolio summary data render as expected.
2. **Given** a running application, **When** the suite runs the watchlist journey, **Then** an instrument can be added to and removed from the watchlist.
3. **Given** a running application, **When** the suite runs the analysis journey, **Then** stock price and key statistics are displayed for a selected instrument.
4. **Given** a running application, **When** the suite runs the CSV journey, **Then** transactions can be imported and exported successfully.

---

### User Story 3 - Failures are easy to diagnose (Priority: P3)

When a regression test fails in the pipeline, a developer can quickly understand
what went wrong without re-running the suite locally, using artifacts captured
during the failed run.

**Why this priority**: Headless CI runs have no live browser to watch, so
diagnostic artifacts are needed to triage failures efficiently. Valuable but
secondary to having the suite run and cover the flows.

**Independent Test**: Force a failure and confirm the pipeline surfaces a
readable test report plus diagnostic artifacts (such as a screenshot at the
point of failure) attached to the run.

**Acceptance Scenarios**:

1. **Given** a failing regression test, **When** the run completes, **Then** a test report identifying the failed scenario(s) is available from the pipeline.
2. **Given** a failing regression test, **When** the run completes, **Then** a screenshot captured at the point of failure is retained as a downloadable artifact.

---

### Edge Cases

- What happens when the application under test is slow to become ready? The suite must wait for the application to be reachable before starting and fail with a clear message if it never becomes ready.
- How does the system handle flaky timing (elements not yet loaded)? Tests wait for expected conditions rather than fixed delays to reduce intermittent failures.
- What happens when seed/reference data is missing or empty? Tests rely on a known, deterministic data state so assertions remain stable across runs.
- How does the pipeline behave if the regression suite cannot start at all (environment failure)? The job fails clearly and distinguishes environment startup failure from genuine test failures.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The regression suite MUST exercise the application end-to-end through a real browser, simulating user interactions.
- **FR-002**: The regression suite MUST run in headless mode (no visible browser window) so it can execute in the CI environment.
- **FR-003**: The regression suite MUST be triggered automatically as part of the CI pipeline on pull requests targeting the main branch and on push (merge) to the main branch, and MUST also be runnable manually on demand.
- **FR-004**: A failing regression test MUST cause the pipeline to fail and block the pull request from merging.
- **FR-005**: The suite MUST cover the four core user journeys: portfolio dashboard, watchlist management, stock analysis, and CSV import/export.
- **FR-006**: The suite MUST run against a running instance of the full application (frontend, backend, and database) with deterministic seed data. The database MUST be an ephemeral instance created for the run and torn down afterwards, so no state persists between runs.
- **FR-007**: Each test MUST wait for expected application conditions before asserting, rather than relying on fixed delays, to minimise flakiness.
- **FR-008**: The pipeline MUST produce a test report identifying which scenarios passed and which failed.
- **FR-009**: On test failure, the suite MUST capture diagnostic artifacts (at minimum a screenshot at the point of failure) and retain them with the pipeline run.
- **FR-010**: The suite MUST verify the application is reachable before running tests and fail with a clear message if startup does not complete in a reasonable time.

### Key Entities

- **Regression Test Suite**: The collection of automated end-to-end browser tests organised by user journey.
- **User Journey Test**: A single end-to-end scenario covering one core flow, with its setup, interactions, and assertions.
- **Test Report**: The summary of a suite run listing scenario outcomes (pass/fail) and failure details.
- **Failure Artifact**: Captured evidence from a failed scenario (e.g. screenshot, logs) used for triage.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of pull requests targeting the main branch and 100% of pushes to the main branch automatically run the regression suite with no manual steps.
- **SC-002**: All four core user journeys are covered by at least one passing end-to-end scenario each.
- **SC-003**: A regression introduced into any core journey is detected by the suite and blocks the pull request in 100% of cases.
- **SC-004**: The full regression suite completes within 10 minutes in the CI pipeline so it does not materially slow down the review cycle.
- **SC-005**: Every failed run provides a test report and a failure screenshot, enabling a developer to identify the failing journey without re-running the suite locally.
- **SC-006**: Intermittent (non-deterministic) failures account for less than 2% of suite runs over a rolling period.

## Assumptions

- The user has explicitly mandated the technology approach: browser automation using Selenium with Java, executed in headless mode and triggered from the CI pipeline. These implementation choices are recorded here per the user's request even though the spec is otherwise outcome-focused.
- Regression tests run against a full application stack started within the CI job (frontend, backend, and database) using deterministic seed data, rather than against a separately deployed environment. This keeps the suite self-contained and reproducible per run.
- The database is an ephemeral MySQL container brought up inside the CI job via the existing `docker-compose.yml` stack (MySQL 8.4 + backend with dev bootstrap seeding + frontend), then torn down after the run. No persistent, shared, or in-memory/alternate-engine database is used, preserving production-equivalent Flyway/seed behaviour.
- The target browser for the headless runs is Chromium/Chrome. Cross-browser coverage (Firefox, Safari, Edge) is out of scope for the initial version.
- The four core flows described in the project README (portfolio dashboard, watchlist management, stock analysis, CSV import/export) define the regression scope; additional edge-feature coverage is out of scope for v1.
- The regression suite runs on pull requests to the main branch, on push (merge) to the main branch, and on manual dispatch; scheduled or post-deployment runs are out of scope for v1.
- Mobile/responsive-specific regression coverage is out of scope for v1 (desktop viewport only).
