# Implementation Plan: SIT Homelab Profile

**Branch**: `008-sit-homelab-profile` | **Date**: 2026-06-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/008-sit-homelab-profile/spec.md`

## Summary

Introduce a backend-only `sit` deployment path for the private homelab
environment. The implementation keeps the existing frontend and AWS production
paths unchanged, adds a Quarkus `sit` runtime profile plus operator-supplied
host configuration, and provides a manual homelab deployment script that runs
inside the private network, loads deployment inputs from a local `scripts/.env`
file or shell environment, packages the backend, deploys it to the homelab app
host, validates database connectivity, and confirms the public health endpoint
before reporting success.

## Technical Context

**Language/Version**: Java 21 (backend), Bash for deployment automation  
**Primary Dependencies**: Quarkus 3.15.2, Hibernate ORM Panache, Flyway, MySQL JDBC, SmallRye Health, existing repo shell scripts  
**Storage**: MySQL 8.x on a homelab LXC database host  
**Testing**: Backend JUnit 5 + QuarkusTest + Testcontainers MySQL; shell-script dry-run/validation checks; manual homelab smoke verification via HTTP health check  
**Target Platform**: Private homelab Linux hosts running as Proxmox LXC containers; frontend remains hosted on Vercel and calls the public backend URL  
**Project Type**: Web application with backend deployment automation  
**Performance Goals**: SIT backend deployment completes within 15 minutes; backend health endpoint responds successfully immediately after deployment verification; invalid configuration fails before rollout work begins  
**Constraints**: GitHub cannot reach the private homelab directly; deployment is operator-triggered from inside the homelab; backend and database IPs must remain configurable; deployment inputs may be stored in a local `scripts/.env` file but secrets must stay out of git; frontend deployment flow must remain unchanged; existing AWS production deployment must not regress  
**Scale/Scope**: One additional non-production backend environment (`sit`), one app host, one MySQL host, one operator-run deployment script

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment | Status |
|-----------|------------|--------|
| I. Automated Tests & Manual Verification (NON-NEGOTIABLE) | Plan adds automated coverage for `sit` profile config resolution and deployment-script validation paths, with manual verification documented for a real homelab deploy and public health check. | PASS |
| II. Compilation Integrity (NON-NEGOTIABLE) | Backend changes remain under Maven build/test flows and do not alter frontend or infra compile paths. | PASS |
| III. Simplicity & YAGNI | Design reuses existing Quarkus config and shell-script patterns; no new orchestrator, agent, or remote control service is introduced. | PASS |
| IV. Specification-Driven Development | All planned artifacts trace directly to spec `008-sit-homelab-profile`; deployment-trigger clarification is integrated in the spec. | PASS |

Post-design re-check: Phase 1 artifacts preserve the same boundaries and do not
introduce any Constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/008-sit-homelab-profile/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── deployment-script-contract.md
│   └── runtime-config-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── src/main/java/com/stocktracker/
│   ├── config/                 # config-source and runtime config helpers
│   ├── api/                    # health/resources remain the validation surface
│   └── resources/              # no new HTTP surface expected
├── src/main/resources/
│   ├── application.properties  # add sit-profile config entries
│   └── db/migration/           # unchanged unless deployment needs schema support
└── src/test/java/com/stocktracker/
    ├── config/                 # profile/config resolution tests
    └── api/                    # optional health/config integration coverage

scripts/
├── package-lambda.sh           # existing AWS packaging path; unchanged
├── smoke-check.sh              # reusable post-deploy verification
├── deploy-homelab-sit.sh       # new operator-run homelab deployment script
├── test-deploy-homelab-sit.sh  # minimal automated validation for script entry paths
└── .env.example                # documented sample input file for homelab deploys

frontend/
├── src/
└── dist/                       # unchanged for this feature

infra/
└── ...                         # existing AWS production deployment, unchanged
```

**Structure Decision**: The feature extends the existing web application
repository but intentionally limits implementation to the backend config and a
repo-local deployment script. Frontend and AWS infrastructure remain untouched
except as reference points for non-regression.

## Phase 0: Research

Research outcomes are captured in [research.md](./research.md) and resolve the
key planning questions:

1. Use a manual operator-run script inside the homelab rather than GitHub
   reach-in or a new pull agent.
2. Deploy the backend as a JVM application package suitable for the homelab
   app host instead of reusing the AWS Lambda packaging path.
3. Represent homelab differences through a Quarkus `sit` profile plus deploy
   script environment variables, with support for loading a local `scripts/.env`
   file and sensible defaults for the current LXC IPs.
4. Reuse the existing `scripts/smoke-check.sh` health verification pattern for
   post-deploy checks against the public backend URL.

## Phase 1: Design & Contracts

Artifacts generated in this phase:

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [quickstart.md](./quickstart.md)
- [contracts/deployment-script-contract.md](./contracts/deployment-script-contract.md)
- [contracts/runtime-config-contract.md](./contracts/runtime-config-contract.md)

Agent context updated: `AGENTS.md` now points at `specs/008-sit-homelab-profile/plan.md`.

## Complexity Tracking

> No Constitution Check violations; section intentionally empty.
