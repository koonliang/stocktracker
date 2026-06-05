# Phase 1 Data Model: Automated Web Regression Testing

This feature has no application data model of its own — it exercises the existing
StockTracker schema through the UI. The "entities" here are the test-domain
constructs from the spec, mapped to concrete Java types in the `e2e/` project.

## Test Suite

The whole collection of journey tests for one run.

- Maps to: the `e2e/` Maven project; `mvn verify` executes it.
- Attributes: target base URL, browser/headless config, overall pass/fail.
- Relationships: contains many **User Journey Tests**; produces one **Test Report**.

## User Journey Test

One end-to-end scenario covering a single core flow.

- Maps to: a JUnit 5 `@Test` method inside a `*JourneyTest` class.
- Attributes: journey name, ordered steps (navigate → interact → assert),
  expected outcome.
- Relationships: uses one or more **Page Objects**; on failure produces a
  **Failure Artifact**.
- Coverage (v1, one journey each — see `contracts/journeys.md`):
  - Dashboard journey
  - Watchlist journey (add + remove)
  - Analysis journey
  - CSV import/export journey

## Page Object

Encapsulates the locators and actions for one screen/route.

- Maps to: `pages/*Page.java`.
- Attributes: route path, locators (accessible-first, minimal `data-testid`),
  action methods, query methods.
- Relationships: used by **User Journey Tests**; one per major route
  (`/`, `/watchlists`, `/analysis/:ticker`, `/transactions`).

## Application Under Test (ephemeral)

The running stack the tests drive — not owned by the suite, started per run.

- Maps to: `docker-compose.yml` services (mysql, backend, frontend).
- Attributes: frontend URL `http://localhost:5173`, backend URL
  `http://localhost:8080`, deterministic seed/bootstrap data, ephemeral MySQL
  volume.
- Lifecycle: `up -d --wait` (pre-tests) → tests run → `down -v` (post-tests).
- State: reset every run; no persistence between runs.

## Test Report

Summary of a suite run.

- Maps to: Maven Surefire XML/text reports under `e2e/target/surefire-reports/`.
- Attributes: per-scenario pass/fail, failure messages/stack traces, totals.
- Relationships: uploaded as a CI artifact; one per **Test Suite** run.

## Failure Artifact

Evidence captured when a scenario fails.

- Maps to: PNG screenshot under `e2e/target/screenshots/<TestName>.png` (plus
  optional compose logs).
- Attributes: screenshot image, failing test name, timestamp.
- Relationships: produced by a failing **User Journey Test**; uploaded as a CI
  artifact with `if: always()`.
