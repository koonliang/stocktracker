# StockTracker E2E Regression Suite

Selenium + JUnit 5 end-to-end regression tests that drive a real (headless
Chromium) browser through StockTracker's core journeys against an ephemeral
full stack brought up with the repo-root `docker-compose.yml`.

This is an isolated Maven project — it is intentionally separate from
`backend/` so browser/Selenium dependencies stay out of the deployable backend
artifact and the existing `backend-test` job is unaffected.

## Prerequisites

- Java 21 (Temurin) and Maven (the `mvnw` wrapper delegates to a local `mvn`)
- Docker + Docker Compose
- Google Chrome / Chromium installed locally — Selenium Manager (built into
  Selenium 4.6+) resolves the matching driver automatically; no manual driver
  download required.

## Run the full stack + suite locally

```bash
# From the repository root:

# 1. Bring up the ephemeral stack (mysql + backend + frontend) and wait for health
docker compose up -d --wait

# 2. Run the regression suite headless against the running stack
mvn -B -f e2e/pom.xml test

# 3. Tear down and drop the ephemeral database volume
docker compose down -v
```

## Configuration (system properties)

| Property             | Default                 | Purpose                                     |
| -------------------- | ----------------------- | ------------------------------------------- |
| `e2e.baseUrl`        | `http://localhost:5173` | Frontend base URL under test                |
| `e2e.backendBaseUrl` | `http://localhost:8080` | Backend base URL for the dev token endpoint |
| `e2e.headless`       | `true`                  | Set `false` to watch the browser locally    |
| `e2e.slowMo`         | `0`                     | Debug-only ms pause between interactions    |

Examples:

```bash
# Run a single test class
mvn -B -f e2e/pom.xml test -Dtest=SmokeTest

# Run headed for debugging
mvn -B -f e2e/pom.xml test -De2e.headless=false

# Run headed and slowed down so each step is watchable (opt-in; off by default)
mvn -B -f e2e/pom.xml test -De2e.headless=false -De2e.slowMo=500 -Dtest=WatchlistJourneyTest

# Point at a different frontend
mvn -B -f e2e/pom.xml test -De2e.baseUrl=http://localhost:3000
```

## Authentication journey

`AuthJourneyTest` exercises the dev-mode auth flows (the stack runs with
`STOCKTRACKER_AUTH_MODE=dev`): sign-up → verify → sign-in → sign-out, invalid
credentials, protected-route redirect, password reset, and per-user data
isolation. It uses `DevTokenClient` to read verification/reset tokens from the
dev-only endpoint `GET /api/dev/auth/latest-token` (configurable via
`e2e.backendBaseUrl`), so flows run without a live inbox. The isolation scenario
relies on two bootstrap-seeded verified accounts: `seed@stocktracker.local` (owns
demo data) and `empty@stocktracker.local` (no data) — both with password
`DevPass123!`.

```bash
# Run just the auth journey
mvn -B -f e2e/pom.xml test -Dtest=AuthJourneyTest
```

## Output

- Test report (raw): `e2e/target/surefire-reports/`
- Failure screenshots: `e2e/target/screenshots/<TestClass>.<testMethod>.png`
  (written automatically at the point of failure by `ScreenshotOnFailure`)
- Allure HTML report: `e2e/target/site/allure-maven-plugin/index.html` (see below)

## Allure HTML report

The suite writes Allure result JSON to `e2e/target/allure-results/` during the
run. Turn it into a browsable HTML report (dashboard, per-journey results, with
failure screenshots attached inline) after a run:

```bash
# Generate the report into e2e/target/site/allure-maven-plugin/
mvn -B -f e2e/pom.xml allure:report

# Or generate + open it in a browser in one step
mvn -B -f e2e/pom.xml allure:serve
```

The report is generated in **single-file** mode (`<singleFile>true</singleFile>`
in `pom.xml`): `e2e/target/site/allure-maven-plugin/index.html` is fully
self-contained (data, JS and CSS inlined), so you can open it directly in a
browser — including the downloaded CI artifact — with no local server and no
`file://` restrictions.

## Formatting

Java sources are formatted with Spotless (google-java-format), matching
`backend/`:

```bash
mvn -B -f e2e/pom.xml spotless:check   # verify
mvn -B -f e2e/pom.xml spotless:apply   # auto-format
```

## In CI

`.github/workflows/regression.yml` runs the same steps automatically on:

- pull requests targeting `main`
- pushes to `main`
- manual `workflow_dispatch` (Actions tab -> Run workflow)

A failing journey fails the job and blocks the PR. Surefire reports
(`surefire-reports`), failure screenshots (`failure-screenshots`), and the Allure
HTML report (`allure-report`) are uploaded as build artifacts on every run
(`if: always()`), and the container logs are dumped to the job log when a test
fails (`if: failure()`) — together they make a failed run easy to triage from the
Actions UI. (Download the `allure-report` artifact and open its `index.html`
directly — it is a self-contained single file.)
