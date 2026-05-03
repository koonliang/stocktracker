# Phase 1 Data Model: CI/CD Pipeline and AWS Deployment

This feature has no application-domain data of its own. The "entities" below
are the operational objects the pipeline produces, references, or manages.
They map onto Terraform resources, GitHub Actions concepts, and AWS service
records.

## Entities

### PipelineRun

A single execution of a GitHub Actions workflow.

- **id**: GitHub `run_id` (numeric)
- **workflow**: `ci.yml` | `cd.yml` | `rollback.yml`
- **trigger**: `pull_request` | `push:main` | `workflow_dispatch`
- **commit_sha**: 40-char hex SHA the run executed against
- **pr_number**: nullable; populated for `pull_request` runs
- **status**: `queued` | `in_progress` | `success` | `failure` | `cancelled`
- **started_at**, **finished_at**: ISO-8601 timestamps
- **logs_url**: link back to GitHub Actions run page

Persistence: GitHub Actions service of record. No additional storage.

### Deployment

The successful or failed promotion of one commit to one environment.

- **id**: `<commit_sha>-<run_id>`
- **environment**: always `production` in v1
- **commit_sha**
- **backend_artifact_version**: Lambda function version number returned by AWS
  on `aws lambda publish-version`
- **frontend_bundle_version**: S3 object version of `index.html` (bucket has
  versioning enabled)
- **infrastructure_revision**: commit SHA of the Terraform code applied
- **smoke_check_result**: `pass` | `fail`
- **outcome**: `succeeded` | `failed` | `rolled_back`
- **started_at**, **finished_at**

Persistence: GitHub Actions run summary (Markdown emitted to
`$GITHUB_STEP_SUMMARY`) and CloudWatch Logs structured log line emitted by
the deploy job. No relational store.

### Environment

A named deployment target. Only `production` exists in v1.

- **name**: `production`
- **aws_account_id**, **aws_region**
- **public_frontend_url**: `https://app.<domain>`
- **public_api_url**: `https://api.<domain>`
- **rds_identifier**, **lambda_function_name**, **api_gateway_id**,
  **s3_bucket_name**: outputs of `terraform apply`

Persistence: Terraform state for the environment (S3 + DynamoDB lock).

### InfrastructureRevision

A version-controlled snapshot of declared AWS + Cloudflare resources.

- **commit_sha**
- **plan_summary**: text emitted by `terraform plan` and posted to the PR
- **apply_outcome**: `applied` | `failed` | `not_applied`
- **drift_detected**: `yes` | `no` (populated by scheduled drift-check)

Persistence: Terraform state file in S3 (versioned bucket).

### Migration

A Flyway migration applied against the production MySQL database.

- **version**: e.g., `V1.0.1__add_dividends_column`
- **applied_at**
- **status**: `success` | `failed`
- **applied_by_run_id**: link back to the CD `PipelineRun` that applied it

Persistence: Flyway's `flyway_schema_history` table inside the application DB.

### Secret

A named sensitive value held in AWS Secrets Manager.

- **name**: `stocktracker/db/master_password`,
  `stocktracker/cloudflare/origin_shared_secret`,
  `stocktracker/cloudflare/api_token`
- **scope**: `runtime` (read by Lambda) | `pipeline` (read by GitHub Actions
  via OIDC role)
- **last_rotated_at**

Persistence: AWS Secrets Manager. Version history kept by the service.

## Relationships

- A `Deployment` references exactly one `PipelineRun` (the CD run that
  produced it) and one `InfrastructureRevision` (the Terraform commit).
- An `Environment` has many `Deployment` records over time.
- A `Migration` is applied during exactly one CD `PipelineRun` and lives in
  the schema-history table of the `Environment`'s database.
- A `Secret` is referenced by the `Environment` (Lambda env vars resolve to
  Secret ARNs) but is not part of Terraform state outside of its name/ARN.

## State Transitions

### PipelineRun

```text
queued → in_progress → (success | failure | cancelled)
```

### Deployment

```text
                    ┌─ smoke_check fails ──→ failed
in_progress ────────┤
                    └─ smoke_check passes ─→ succeeded
                                              │
                              rollback run ───┴──→ rolled_back  (target was redeployed
                                                                 to a prior commit)
```

### Migration

```text
pending → (success | failed)
```

`failed` is terminal for a given migration version; the next CD attempt cannot
proceed until either the migration is fixed in code (new commit) or rolled
forward by a subsequent migration version.

## Validation Rules (mapped to Functional Requirements)

| Rule | Source FR |
|------|-----------|
| A `Deployment` cannot be marked `succeeded` unless its `smoke_check_result` is `pass`. | FR-011 |
| A `Deployment` cannot promote a new `backend_artifact_version` if any `Migration` for that run is `failed`. | FR-021, FR-022 |
| Two `Deployment` records cannot be `in_progress` for the same `Environment` at the same wall-clock time. | FR-014 |
| An `InfrastructureRevision` may only have `apply_outcome: applied` if its commit is on `main`. | FR-017 |
| A `Secret` value must never appear in `PipelineRun.logs_url` content. | FR-026 |
| Every `PipelineRun` triggered by a fork PR must have its AWS OIDC step skipped. | FR-005 |
