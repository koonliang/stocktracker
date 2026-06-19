# Contract: Homelab SIT Deployment Script

Defines the operator-facing contract for the manual deployment entry point.

## Script

`scripts/deploy-homelab-sit.sh`

## Purpose

Deploy the backend to the private homelab SIT environment from a machine that
already has network reachability to the app host and database host.

## Invocation

```bash
scripts/deploy-homelab-sit.sh [options]
```

Before processing flags, the script may source `scripts/.env` when present.
Explicit shell environment variables or command-line flags override values from
that file.

## Required Inputs

| Input | Source | Description |
|------|--------|-------------|
| `APP_HOST` | `.env`, env, or flag | Backend application server host/IP |
| `DB_HOST` | `.env`, env, or flag | MySQL host/IP |
| `PUBLIC_BASE_URL` | `.env`, env, or flag | Public backend base URL for final verification |
| `DEPLOY_USER` | `.env`, env, or flag | Remote user on the app host |
| `SERVICE_NAME` | `.env`, env, or flag | Service or process identifier to restart |

## Optional Inputs

| Input | Default | Description |
|------|---------|-------------|
| `DB_PORT` | `3306` | MySQL port |
| `QUARKUS_PROFILE` | `sit` | Runtime profile passed to the backend |
| `ARTIFACT_PATH` | build output chosen by the script | Override build artifact location |
| `ENV_FILE` | `scripts/.env` | Override path to the local input file |
| `DRY_RUN` | `false` | Print intended actions without rollout |

## Required Behavior

1. If an env file is configured, load it before validating inputs.
2. Validate that all required inputs are present.
3. Validate that the script is being executed from an environment with private
   network access to `APP_HOST` and `DB_HOST`.
4. Build or locate the backend artifact for the homelab runtime.
5. Transfer the artifact and any required runtime config to `APP_HOST`.
6. Restart or replace the backend process using the `sit` runtime profile.
7. Verify the backend through `PUBLIC_BASE_URL/q/health`.
8. Exit non-zero on the first failed validation, rollout, or verification step.

## Output Contract

### Success

- Exit code `0`
- Human-readable summary including:
  - target profile
  - effective app/db host values
  - artifact used
  - final health-check URL

### Failure

- Non-zero exit code
- Clear single-line error indicating whether the failure occurred during:
  - input validation
  - connectivity validation
  - build/package
  - remote rollout
  - post-deploy verification

## Non-Goals

- No GitHub-triggered execution
- No frontend build or deployment
- No AWS infrastructure changes
- No committed secrets in `scripts/.env`
