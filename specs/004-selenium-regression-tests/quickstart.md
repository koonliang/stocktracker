# Quickstart: Automated Web Regression Testing

## Prerequisites

- Java 21 (Temurin) and the `e2e/` Maven wrapper (or local Maven)
- Docker + Docker Compose
- Google Chrome / Chromium available locally (Selenium Manager resolves the
  driver automatically)

## Run the full stack + suite locally

```bash
# 1. Bring up the ephemeral stack (mysql + backend + frontend) and wait for health
docker compose up -d --wait

# 2. Run the regression suite headless against the running stack
mvn -B -f e2e/pom.xml test

# 3. Tear down and drop the ephemeral database volume
docker compose down -v
```

Default target URLs (override with system properties if needed):

- Frontend: `http://localhost:5173`  (`-De2e.baseUrl=...`)
- Backend:  `http://localhost:8080`

## Run a single journey

```bash
mvn -B -f e2e/pom.xml test -Dtest=WatchlistJourneyTest
```

## Run headed (debugging only)

```bash
mvn -B -f e2e/pom.xml test -De2e.headless=false
```

## Where output lands

- Test report: `e2e/target/surefire-reports/`
- Failure screenshots: `e2e/target/screenshots/`

## In CI

`.github/workflows/regression.yml` runs the same steps automatically on:

- pull requests targeting `main`
- pushes to `main`
- manual `workflow_dispatch` (Actions tab → Run workflow)

A failing journey fails the job and blocks the PR. Reports and screenshots are
uploaded as build artifacts (`if: always()`).

## Verifying the feature (acceptance)

1. With a known-good build, run the steps above — all four journeys pass (SC-002).
2. Break a core flow (e.g. hide the holdings table), re-run — the dashboard
   journey fails and a screenshot is produced (SC-003/SC-005).
3. Open a PR — the regression workflow runs without manual steps (SC-001) and
   blocks merge while the breakage exists.
