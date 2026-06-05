# Implementation Plan: Automated Web Regression Testing

**Branch**: `004-selenium-regression-tests` | **Date**: 2026-06-05 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-selenium-regression-tests/spec.md`

## Summary

Add a Selenium + Java end-to-end regression suite that drives a real (headless
Chromium) browser through StockTracker's four core journeys — portfolio
dashboard, watchlist management, stock analysis, and CSV import/export. The suite
runs against an ephemeral full stack (MySQL + backend + frontend) brought up with
the existing `docker-compose.yml` inside a GitHub Actions job, triggered on pull
requests to `main`, pushes to `main`, and manual `workflow_dispatch`. Failures
block the merge and produce a JUnit/Surefire report plus a screenshot artifact at
the point of failure.

## Technical Context

**Language/Version**: Java 21 (Temurin) — matches the backend toolchain
**Primary Dependencies**: Selenium Java 4.x (built-in Selenium Manager for driver
provisioning), JUnit 5 (Jupiter), Maven (Surefire for reporting), AssertJ for
assertions
**Storage**: Ephemeral MySQL 8.4 container from `docker-compose.yml`; no test-owned
persistence — Flyway migration + backend dev bootstrap (`STOCKTRACKER_DEV_BOOTSTRAP_ENABLED=true`)
provide deterministic seed data
**Testing**: JUnit 5 e2e tests using the Page Object pattern; Selenium WebDriver
(ChromeDriver, headless)
**Target Platform**: GitHub Actions `ubuntu-latest`; Chromium/Chrome headless,
desktop viewport (1920x1080)
**Project Type**: Web application (existing `frontend/` + `backend/`) plus a new
isolated `e2e/` Maven project for the regression suite
**Performance Goals**: Full suite completes in under 10 minutes in CI (SC-004)
**Constraints**: Headless only; explicit waits (no fixed sleeps) to keep flaky
runs <2% (SC-006); stack must be confirmed reachable before tests start
**Scale/Scope**: 4 core journeys, ~4-8 scenarios total for v1; single-browser,
single desktop viewport

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Test Verification (NON-NEGOTIABLE)**: PASS — this feature *is* automated
  regression tests; they run and must pass in CI. The new `e2e/` module's own
  build (`mvn verify`) is the gate.
- **II. Lint & Style Compliance (NON-NEGOTIABLE)**: PASS — the `e2e/` Maven
  project adopts Spotless (same approach as `backend/`) so Java sources are
  formatted/checked clean. Any added frontend `data-testid` hooks pass existing
  ESLint/Prettier.
- **III. Compilation Integrity (NON-NEGOTIABLE)**: PASS — `e2e/` compiles with
  zero errors via `mvn -B test-compile`; the CI job fails on compile errors.
- **IV. Simplicity & YAGNI**: PASS — single browser, single viewport, reuse of
  existing `docker-compose.yml` and seed data, no test-data fixtures framework,
  no cross-browser grid. Page Objects only for the four journeys.
- **V. Specification-Driven Development**: PASS — spec → plan → tasks → implement
  flow followed; clarifications recorded in spec.

**Result**: No violations. Complexity Tracking section not required.

## Project Structure

### Documentation (this feature)

```text
specs/004-selenium-regression-tests/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (test domain entities)
├── quickstart.md        # Phase 1 output (run locally + in CI)
├── contracts/           # Phase 1 output (journey + CI trigger contracts)
│   ├── journeys.md
│   └── ci-workflow.md
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
e2e/                                 # NEW isolated Maven project (Java regression suite)
├── pom.xml                          # Selenium 4.x, JUnit 5, AssertJ, Surefire, Spotless
├── README.md
└── src/test/java/com/stocktracker/e2e/
    ├── support/
    │   ├── DriverFactory.java       # Headless Chrome options, viewport, Selenium Manager
    │   ├── BaseTest.java            # WebDriver lifecycle, base URL, failure screenshot hook
    │   ├── Waits.java               # Explicit-wait helpers (no Thread.sleep)
    │   └── ScreenshotOnFailure.java # JUnit 5 extension capturing PNG on failure
    ├── pages/
    │   ├── DashboardPage.java
    │   ├── WatchlistsPage.java
    │   ├── AnalysisPage.java
    │   └── TransactionsPage.java
    └── journeys/
        ├── DashboardJourneyTest.java
        ├── WatchlistJourneyTest.java
        ├── AnalysisJourneyTest.java
        └── CsvImportExportJourneyTest.java

frontend/src/...                     # MINIMAL change: add stable data-testid hooks
                                     # at journey assertion points (see research.md)

.github/workflows/
└── regression.yml                   # NEW workflow: PR + push to main + workflow_dispatch
```

**Structure Decision**: A new top-level `e2e/` Maven project isolates the Java
Selenium suite from the Quarkus `backend/` build so it does not affect backend
artifacts or the existing `backend-test`/`frontend-test` CI jobs. The suite is
driven entirely through the browser against the running stack, so it lives
outside both app modules. A dedicated `regression.yml` workflow owns the three
triggers and the docker-compose lifecycle.

## Complexity Tracking

> No constitution violations — section intentionally empty.
