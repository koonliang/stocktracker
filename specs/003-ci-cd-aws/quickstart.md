# Quickstart: CI/CD Pipeline and AWS Deployment

Operator-facing runbook. Read top to bottom on first setup; thereafter, jump
to the section you need.

---

## Prerequisites

- An AWS account dedicated to (or at least a clean namespace within) this
  project, with a payer who is okay with the resources in this plan
  (Lambda, API Gateway HTTP API, RDS `db.t4g.small`, S3, Secrets Manager,
  CloudWatch Logs).
- A Cloudflare account that owns the DNS zone you will use (e.g.,
  `stocktracker.example`).
- ACM certificate for `*.stocktracker.example` (or for the two specific
  hostnames `app.<domain>` and `api.<domain>`) in the **same region** as
  the API Gateway.
- Local tools: `aws` CLI v2, `terraform` 1.7+, `tflint`, `node` 20+, `jdk`
  21, `mvn` 3.9+.

## One-time bootstrap (manual)

These steps create resources Terraform itself needs to run; they are
**deliberately not** managed by the main configuration.

1. **Create the Terraform state bucket and lock table.**

   ```bash
   cd infra/bootstrap
   terraform init
   terraform apply
   ```

   Outputs: `state_bucket_name`, `lock_table_name`. Record both into
   `infra/envs/production/backend.tf` (already templated; replace the
   placeholder values).

2. **Create the GitHub OIDC provider and the two deploy roles.** Same
   bootstrap configuration. Outputs: `gha_plan_role_arn`,
   `gha_deploy_role_arn`. Record both as **repository variables** in
   GitHub:
   - `AWS_PLAN_ROLE_ARN`
   - `AWS_DEPLOY_ROLE_ARN`
   - `AWS_REGION`

3. **Seed Secrets Manager** with three secrets — empty placeholder values
   are fine; rotate after first apply:
   - `stocktracker/db/master_password`
   - `stocktracker/cloudflare/api_token`
   - `stocktracker/cloudflare/origin_shared_secret`

4. **Configure GitHub branch protection** on `main`:
   - Require PRs (no direct pushes).
   - Require the `gates` status check (case-sensitive).
   - Require branches to be up to date before merging.

## First-time provision of `production`

```bash
cd infra/envs/production
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

Outputs you will care about: `api_invoke_url`, `frontend_bucket_name`,
`rds_endpoint` (private), `lambda_function_name`.

After the first apply, point the Cloudflare DNS records (managed in the
`cloudflare` Terraform module) at the API Gateway custom domain target and
the S3 bucket website endpoint. Cloudflare records are **proxied** for the
frontend and **DNS-only** for the API.

## Day-to-day developer flow

1. Create a feature branch.
2. Open a PR against `main`. The `ci.yml` workflow runs automatically:
   - `backend-test`, `frontend-test`, optional `terraform-plan`.
   - All required checks must be green before the PR is mergeable.
3. Merge the PR. The `cd.yml` workflow runs automatically:
   - Build → `terraform apply` → `db-migrate` → `backend-deploy` →
     `frontend-deploy` → `smoke`.
   - The deployment summary is in the run's "Summary" tab.
4. Verify in browser: `https://app.<domain>` and
   `https://api.<domain>/q/health`.

## Rollback

When a bad deployment lands:

1. Find the last known-good commit SHA on `main` (the GitHub commit list
   shows green/red CD statuses).
2. Run the **Rollback** workflow (`Actions → Rollback → Run workflow`).
   - `commit_sha`: the SHA from step 1.
   - `confirm`: the literal string `ROLLBACK`.
3. The workflow redeploys that commit's previously-built artifacts. It does
   **not** roll back Terraform or the database — see "Limitations" below.

## Smoke check (manual, for debugging)

```bash
./scripts/smoke-check.sh https://app.<domain> https://api.<domain>
```

The script exits non-zero on any non-2xx response.

## Local development is unchanged

This feature does not modify how `backend/` or `frontend/` run locally.
`./mvnw quarkus:dev` and `npm run dev` work as before.

## Limitations to be aware of

- **Rollback does not undo Terraform changes or DB migrations.** If a bad
  Terraform change or a bad migration lands, rolling back code alone may
  not restore service. Treat infrastructure and migration changes as
  forward-only and review them carefully on PR.
- **Single environment.** `production` is the only deployment target in
  v1. Verifying changes against a staging copy is not available; rely on
  PR-time tests and the smoke check.
- **Cold starts.** Quarkus on Lambda JVM has a multi-second cold start;
  expect occasional slow first requests until provisioned concurrency is
  enabled (see `research.md` R-003).
- **No automatic alarm actions.** CloudWatch logs and metrics exist;
  alerting/on-call is out of scope for this feature.

## Troubleshooting

| Symptom | First place to look |
|---------|---------------------|
| PR is "blocked" but no failed check is visible | Branch protection requires `gates` — check if a job was skipped (e.g. fork PR). |
| `terraform apply` fails with state lock error | Another CD run is in progress, or a previous run was killed mid-apply. Check DynamoDB lock table; force-unlock only after confirming no active run. |
| Frontend shows old version after deploy | Cloudflare cache purge step in the workflow logs; if that succeeded, force-refresh the browser; check Cloudflare dashboard for the purge event. |
| Backend returns 502 | Lambda timed out or threw on init. Inspect `/aws/lambda/stocktracker-production-app` log group; check whether Secrets Manager retrieval is the cause. |
| API requests get CORS errors | Confirm the request origin is listed in `AllowOrigins` (HTTP API stage config); browsers cache preflight up to 600s. |
| `db-migrate` job fails | Inspect the migrator Lambda's log group; the failed Flyway version is named in the error. Fix forward in code; do not edit `flyway_schema_history` by hand. |
