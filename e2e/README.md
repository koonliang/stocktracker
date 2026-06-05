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

| Property         | Default                 | Purpose                                  |
| ---------------- | ----------------------- | ---------------------------------------- |
| `e2e.baseUrl`    | `http://localhost:5173` | Frontend base URL under test             |
| `e2e.headless`   | `true`                  | Set `false` to watch the browser locally |

Examples:

```bash
# Run a single test class
mvn -B -f e2e/pom.xml test -Dtest=SmokeTest

# Run headed for debugging
mvn -B -f e2e/pom.xml test -De2e.headless=false

# Point at a different frontend
mvn -B -f e2e/pom.xml test -De2e.baseUrl=http://localhost:3000
```

## Output

- Test report: `e2e/target/surefire-reports/`
- Failure screenshots: `e2e/target/screenshots/<TestName>.png`

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

A failing journey fails the job and blocks the PR. Surefire reports and failure
screenshots are uploaded as build artifacts (`if: always()`).
