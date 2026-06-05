# Phase 0 Research: Automated Web Regression Testing

All Technical Context items were resolvable from the existing repository and the
clarified spec. No `NEEDS CLARIFICATION` markers remain. Decisions below.

## 1. Test stack: Selenium + JUnit 5 + Maven

- **Decision**: Java 21 Maven project using Selenium Java 4.x, JUnit 5 (Jupiter),
  AssertJ assertions, and Maven Surefire for the test report.
- **Rationale**: User mandated Selenium + Java. Java 21 matches the backend
  toolchain (`actions/setup-java` temurin 21 already in CI). Selenium 4.6+ ships
  **Selenium Manager**, which auto-resolves the matching ChromeDriver — no
  WebDriverManager dependency or manual driver download needed. JUnit 5 +
  Surefire produces standard XML reports that CI can surface.
- **Alternatives considered**: TestNG (no advantage over JUnit 5 here, more
  config); WebDriverManager (redundant now that Selenium Manager is built in);
  Gradle (repo standardises on Maven `mvnw` for Java).

## 2. Project placement: isolated `e2e/` Maven module

- **Decision**: New top-level `e2e/` Maven project, separate from `backend/`.
- **Rationale**: Keeps Selenium/browser dependencies out of the deployable
  backend artifact and leaves the existing `backend-test` job untouched (Simplicity,
  Compilation Integrity). The suite is black-box over HTTP, so it belongs to
  neither app module.
- **Alternatives considered**: Add as a backend test profile (couples browser
  deps to the service build, slows backend CI); put under `frontend/` (wrong
  language/toolchain).

## 3. Headless browser configuration

- **Decision**: Headless Chromium via ChromeOptions: `--headless=new`,
  `--no-sandbox`, `--disable-dev-shm-usage`, `--disable-gpu`, fixed window size
  `1920,1080`.
- **Rationale**: `ubuntu-latest` GitHub runners ship Chrome; `--headless=new` is
  the current stable headless mode; `--disable-dev-shm-usage` avoids `/dev/shm`
  crashes in containers; fixed viewport keeps layout-dependent assertions
  deterministic (desktop-only per spec).
- **Alternatives considered**: Firefox/geckodriver (cross-browser is out of scope
  for v1); Selenium Grid/Selenoid (unnecessary for single-browser single-runner).

## 4. Application-under-test lifecycle (ephemeral DB)

- **Decision**: Bring up the full stack with the existing `docker-compose.yml`
  (`docker compose up -d --wait`) in the CI job before tests, tear down with
  `docker compose down -v` after. Tests target the frontend at
  `http://localhost:5173`; backend at `http://localhost:8080`.
- **Rationale**: Matches the clarified decision (ephemeral MySQL in CI). Compose
  already has healthchecks and `depends_on: service_healthy` ordering, and the
  backend seeds reference + demo data on startup
  (`STOCKTRACKER_DEV_BOOTSTRAP_ENABLED=true`), giving a deterministic state with
  no extra fixtures. `down -v` drops the volume so no state leaks between runs.
- **Alternatives considered**: Testcontainers from within the Java suite (would
  duplicate the compose topology and re-specify env/seed wiring — more complex);
  shared/persistent DB (flaky, rejected in clarification); managed cloud DB per
  run (slow/costly, rejected).

## 5. Readiness gating before tests

- **Decision**: Use `docker compose up -d --wait` (honours healthchecks) and add
  a short frontend reachability poll in the workflow before running `mvn`. The
  Java `BaseTest` also waits for a known landing element before each journey.
- **Rationale**: Satisfies FR-010 and the "slow to start" edge case; distinguishes
  environment-startup failure (compose `--wait` fails the job early) from genuine
  test failures (Selenium assertions).
- **Alternatives considered**: Fixed sleep before tests (violates the no-fixed-delay
  constraint, flaky).

## 6. Stable element selection strategy

- **Decision**: Prefer accessible locators (role/label/visible text) consistent
  with the frontend's Testing Library style; add a **minimal** set of
  `data-testid` attributes only at the specific assertion/interaction points the
  journeys need (the repo currently has just one `data-testid`). Page Objects
  encapsulate all locators.
- **Rationale**: Keeps selectors resilient and avoids brittle CSS/XPath. A small,
  intentional set of test ids at journey-critical nodes (e.g. holdings table,
  portfolio summary, watchlist add/remove controls, key-stats grid, CSV
  import/export controls) gives stability without littering the UI. Page Objects
  localise change (Simplicity).
- **Alternatives considered**: Pure CSS/XPath against generated class names
  (brittle); adding test ids everywhere (unnecessary churn, YAGNI).

## 7. Failure diagnostics

- **Decision**: A JUnit 5 extension captures a full-page PNG screenshot on test
  failure into `e2e/target/screenshots/`; the workflow uploads
  `e2e/target/surefire-reports/**` and the screenshots via
  `actions/upload-artifact` with `if: always()`. Optionally capture
  `docker compose logs` on failure.
- **Rationale**: Satisfies FR-008/FR-009 and SC-005 — readable report plus
  point-of-failure screenshot available without re-running locally.
- **Alternatives considered**: Video recording (heavier, not needed for v1);
  Allure reporting (extra dependency/infra — defer, YAGNI).

## 8. CI triggers

- **Decision**: New `.github/workflows/regression.yml` with
  `on: { pull_request: [branches: main], push: [branches: main], workflow_dispatch: {} }`,
  a concurrency group to cancel superseded runs, and `permissions: contents: read`.
- **Rationale**: Matches the clarified trigger set (PR to main + push to main +
  manual). Mirrors conventions in the existing `ci.yml`.
- **Alternatives considered**: Folding into `ci.yml` (mixes concerns and the
  push-to-main trigger differs from `ci.yml`'s PR-only model — a separate
  workflow is clearer).
