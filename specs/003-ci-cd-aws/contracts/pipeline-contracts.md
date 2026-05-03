# Pipeline Contracts

These are the externally-observable contracts of the GitHub Actions workflows
introduced by this feature. They are intentionally narrow — internal job
structure, step names, and runner choices are implementation details.

## Workflow: `ci.yml`

**Trigger**: `pull_request` with branches `[main]`, types `[opened,
synchronize, reopened]`.

**Required status checks** (these names are referenced by GitHub branch
protection on `main`; renaming them is a breaking change):

| Check name | Pass condition |
|------------|----------------|
| `backend-test` | `mvn -B verify` exits 0 in `backend/` |
| `frontend-test` | `npm ci && npm run lint && npm run typecheck && npm test && npm run build` all exit 0 in `frontend/` |
| `terraform-plan` | `terraform fmt -check`, `terraform validate`, `tflint`, and `terraform plan` all exit 0 in `infra/envs/production/`. **Skipped (reported as success) when no files under `infra/**` change in the PR diff.** |
| `gates` | All of the above conclude with `success`. This is the single check that branch protection requires. |

**Concurrency**: `group: ci-${{ github.ref }}`, `cancel-in-progress: true` —
satisfies FR-004.

**Fork-safety contract**: When `github.event.pull_request.head.repo.fork ==
true`, the `terraform-plan` job is skipped entirely and no AWS OIDC role is
assumed. Other jobs run with `permissions: { contents: read }` only —
satisfies FR-005.

## Workflow: `cd.yml`

**Trigger**: `push` to `main`.

**Concurrency**: `group: cd-production`, `cancel-in-progress: false` — at most
one CD run per environment, and an in-flight run is **not** cancelled by a
later push (the later push waits) — satisfies FR-014.

**Job order (each must succeed before the next runs)**:

1. `build` — produces `backend/target/function.zip` and
   `frontend/dist/**`, uploads both as workflow artifacts retained for 30
   days. Artifact name: `app-${{ github.sha }}`.
2. `terraform-apply` — applies `infra/envs/production/`. Outputs are exposed
   to subsequent jobs as job outputs (Lambda function name, API Gateway
   stage, S3 bucket, etc.).
3. `db-migrate` — invokes the migrator Lambda synchronously and fails the
   workflow if Flyway exits non-zero — satisfies FR-021/-022.
4. `backend-deploy` — uploads `function.zip` to the application Lambda,
   publishes a new version, and updates the `production` alias to point at
   the new version — satisfies FR-007.
5. `frontend-deploy` — `aws s3 sync frontend/dist/ s3://<bucket>/ --delete`,
   then calls Cloudflare API to purge the index URLs — satisfies
   FR-008/-010.
6. `smoke` — runs `scripts/smoke-check.sh` against `https://app.<domain>`
   and `https://api.<domain>/q/health`. Non-2xx fails the workflow —
   satisfies FR-011. **The previous Lambda alias is not changed when this
   step fails** (alias swap already happened in step 4); a true rollback
   requires running `rollback.yml`. (Documented limitation — see Edge Cases
   in the spec; full automatic rollback is deferred per Principle IV.)

**Outputs**: A Markdown summary written to `$GITHUB_STEP_SUMMARY` containing
the `Deployment` entity fields from `data-model.md` — satisfies FR-028.

## Workflow: `rollback.yml`

**Trigger**: `workflow_dispatch` with required inputs:

| Input | Type | Description |
|-------|------|-------------|
| `commit_sha` | string | The 40-char SHA of a previously-successful CD run to redeploy |
| `confirm` | string | Must equal the literal string `ROLLBACK` to proceed |

**Behavior**: Re-runs the `backend-deploy` and `frontend-deploy` jobs from
`cd.yml` using the artifact bundle named `app-<commit_sha>`. Skips
`terraform-apply` and `db-migrate` (Terraform rollback and DB rollback are
out of scope — Principle IV) — satisfies FR-013.

## Required GitHub Repository Configuration

The following must exist in the repo (configured manually once or via a
separate Terraform `github` provider configuration — out of scope for this
spec):

- Branch protection on `main` requiring the `gates` check.
- Repository variable `AWS_DEPLOY_ROLE_ARN` (non-secret).
- Repository variable `AWS_REGION`.
- Environment named `production` with required reviewers (optional;
  recommended).
