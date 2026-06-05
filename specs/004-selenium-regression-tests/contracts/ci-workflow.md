# Contract: CI Workflow (`.github/workflows/regression.yml`)

## Triggers

```yaml
on:
  pull_request:
    branches: [main]
    types: [opened, synchronize, reopened]
  push:
    branches: [main]
  workflow_dispatch: {}
```

- PR to `main`, push (merge) to `main`, and manual run — FR-003.

## Concurrency & permissions

```yaml
concurrency:
  group: regression-${{ github.ref }}
  cancel-in-progress: true
permissions:
  contents: read
```

## Job: `web-regression` (runs-on: ubuntu-latest)

Ordered steps (contract — names may be refined in implementation):

1. **checkout** — `actions/checkout@v4`.
2. **setup-java** — `actions/setup-java@v4`, temurin 21, `cache: maven`.
3. **Start stack** — `docker compose up -d --wait` (honours healthchecks).
   - Fails the job early if any service never becomes healthy (env-startup
     failure, distinct from test failure) — FR-010.
4. **Verify frontend reachable** — short poll of `http://localhost:5173`.
5. **Run regression suite** — `mvn -B -f e2e/pom.xml test` (headless Chrome).
6. **Upload reports** — `actions/upload-artifact@v4`, `if: always()`,
   path `e2e/target/surefire-reports/**` — FR-008.
7. **Upload failure screenshots** — `actions/upload-artifact@v4`, `if: always()`,
   path `e2e/target/screenshots/**` — FR-009/SC-005.
8. **Dump compose logs on failure** (optional) — `if: failure()`,
   `docker compose logs`.
9. **Tear down** — `if: always()`, `docker compose down -v` (drops ephemeral DB).

## Outcomes

- Non-zero `mvn` exit → job fails → PR merge blocked — FR-004/SC-003.
- Green suite → job passes → PR mergeable — Story 1 AS-2.
- Target wall-clock under 10 minutes — SC-004.
