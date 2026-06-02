---

description: "Task list for CI/CD Pipeline and AWS Deployment"
---

# Tasks: CI/CD Pipeline and AWS Deployment

**Input**: Design documents from `/specs/003-ci-cd-aws/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Application unit/integration tests already exist in `backend/` and `frontend/` and are *exercised* by the CI pipeline; this feature does not add new application-level tests. Pipeline behaviour is verified via documented manual smoke runs in `quickstart.md` and the post-deploy smoke step in `cd.yml`.

**Organization**: Tasks grouped by user story. US1 + US2 (both P1) form the MVP.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Maps task to a user story (US1..US5)

## Path Conventions

- Existing trees: `backend/`, `frontend/` (unchanged structure)
- New trees this feature: `infra/`, `.github/workflows/`, `scripts/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the directory layout and bootstrap files that everything else depends on.

- [X] T001 Create top-level layout: `infra/bootstrap/`, `infra/envs/production/`, `infra/modules/`, `.github/workflows/`, `scripts/` (empty `.gitkeep` where needed)
- [X] T002 [P] Add `infra/.terraform-version` pinning Terraform 1.7.x and `infra/.tflint.hcl` with the AWS plugin enabled
- [X] T003 [P] Add `infra/README.md` summarising the module/env layout, state-bucket bootstrap, and the manual one-time steps from `quickstart.md`
- [X] T004 [P] Update `.gitignore` to exclude `**/.terraform/`, `*.tfstate*`, `*.tfplan`, and Lambda packaging output `backend/target/function.zip`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Bootstrap Terraform state + GitHub→AWS OIDC, and add the Quarkus Lambda packaging hook. These unblock every user story.

**Critical**: No user story phase may start until this phase is complete.

- [X] T005 Author `infra/bootstrap/main.tf` provisioning: S3 state bucket (versioning + SSE-S3), DynamoDB lock table, GitHub OIDC provider, IAM roles `gha-plan-production` and `gha-deploy-production` with the trust + permission policies described in `contracts/github-oidc-trust.md`
- [X] T006 [P] Author `infra/bootstrap/variables.tf` (`github_org`, `github_repo`, `aws_region`) and `infra/bootstrap/outputs.tf` (state bucket, lock table, role ARNs)
- [X] T007 [P] Add `infra/envs/production/backend.tf` configured for the S3 + DynamoDB backend (placeholders to be filled in after bootstrap apply)
- [X] T008 Add a Maven profile `aws-lambda` in `backend/pom.xml` that adds the `quarkus-amazon-lambda-http` extension and produces `backend/target/function.zip`
- [X] T009 [P] Author `scripts/package-lambda.sh` invoking `./mvnw -B -Paws-lambda package` and verifying `backend/target/function.zip` exists; exit non-zero if missing
- [X] T010 [P] Author `scripts/smoke-check.sh` taking `<frontend-url> <api-url>` args and asserting `200` from `<frontend-url>/` and `<api-url>/q/health`

**Checkpoint**: Bootstrap can be applied manually; OIDC roles + state backend exist; backend can be packaged for Lambda.

---

## Phase 3: User Story 1 — Automated PR Validation (Priority: P1) 🎯 MVP slice 1

**Goal**: Every PR against `main` runs backend tests, frontend tests, lint/typecheck, and (when infra changes) `terraform plan`, with results required by branch protection.

**Independent Test**: Open a PR with a deliberately failing backend test → `gates` check fails, merge blocked. Push a fix → `gates` turns green, merge allowed.

### Implementation for User Story 1

- [X] T011 [US1] Create `.github/workflows/ci.yml` triggered on `pull_request` against `main` with `concurrency: ci-${{ github.ref }}` (cancel-in-progress), defaulting permissions to `contents: read`
- [X] T012 [P] [US1] Add `backend-test` job to `.github/workflows/ci.yml`: `actions/setup-java@v4` (Temurin 21), cache `~/.m2`, run `./mvnw -B verify` in `backend/`
- [X] T013 [P] [US1] Add `frontend-test` job to `.github/workflows/ci.yml`: `actions/setup-node@v4` (Node 20, npm cache), run `npm ci && npm run lint && npm run typecheck && npm test && npm run build` in `frontend/`
- [X] T014 [US1] Add `terraform-plan` job to `.github/workflows/ci.yml`: path-filtered to `infra/**`, assumes `AWS_PLAN_ROLE_ARN` via `aws-actions/configure-aws-credentials@v4` (OIDC), runs `terraform fmt -check`, `terraform validate`, `tflint`, and `terraform -chdir=infra/envs/production plan -no-color` and posts the summary to the PR via `actions/github-script`
- [X] T015 [US1] Add fork-safety guard to the `terraform-plan` job in `.github/workflows/ci.yml`: skip when `github.event.pull_request.head.repo.fork == true`
- [X] T016 [US1] Add the aggregating `gates` job to `.github/workflows/ci.yml` that `needs: [backend-test, frontend-test, terraform-plan]` and treats a skipped `terraform-plan` as success
- [X] T017 [US1] Document required GitHub branch protection in `infra/README.md`: require `gates` check on `main`; require linear PR-based merges

**Checkpoint**: PR validation pipeline operates end-to-end (CI only — no AWS deploy yet).

---

## Phase 4: User Story 2 — Continuous Deployment to AWS on Merge (Priority: P1) 🎯 MVP slice 2

**Goal**: A merge to `main` triggers Terraform apply (ephemeral stack), backend deploy to Lambda behind API Gateway HTTP API, frontend deploy to S3 with CloudFront invalidation, and post-deploy smoke checks. Failures halt the workflow without flipping traffic.

**Independent Test**: Merge a PR that changes a visible string in the frontend and a response field in `/api/dashboard`. Within 15 min, the new string is visible at the CloudFront frontend URL and the API returns the new field at the API Gateway URL.

> **Note**: The tasks below were originally completed against a Cloudflare-based design. Phase 9 captures the migration to CloudFront + a persistent/ephemeral stack split. Treat the historical [X] entries here as the original Cloudflare implementation; the migration work in Phase 9 supersedes them.

### Terraform modules for production environment

- [X] T018 [P] [US2] Author `infra/modules/network/` (VPC, two private subnets in different AZs, security groups for Lambda and RDS, VPC endpoints for Secrets Manager and CloudWatch Logs)
- [X] T019 [P] [US2] Author `infra/modules/lambda_backend/` (function from `function.zip`, runtime `java21`, memory 1024 MB, IAM execution role with Secrets Manager + CloudWatch Logs access, alias `production`, optional `provisioned_concurrent_executions` variable defaulting to 0)
- [X] T020 [P] [US2] Author `infra/modules/api_gateway/` (HTTP API per `contracts/http-api-contract.md`: catch-all `ANY /{proxy+}` and `ANY /` proxy integrations to the Lambda alias, custom domain `api.<domain>` bound to ACM cert, CORS/throttling/access-log settings)
- [X] T021 [P] [US2] Author `infra/modules/frontend_bucket/` (private S3 bucket, Block Public Access on, bucket policy allowing `s3:GetObject` only when header `X-Origin-Auth` matches the shared secret in Secrets Manager, versioning enabled)
- [X] T022 [P] [US2] Author `infra/modules/cloudflare/` (DNS records: proxied `app` CNAME to S3 website endpoint, DNS-only `api` CNAME to API Gateway custom-domain target; transform rule injecting `X-Origin-Auth` from secret on `app` requests)
- [X] T023 [US2] Wire `infra/envs/production/main.tf` to compose `network`, `lambda_backend`, `api_gateway`, `frontend_bucket`, and `cloudflare` modules; expose outputs (`api_invoke_url`, `frontend_bucket_name`, `lambda_function_name`, `cloudflare_zone_id`)
- [X] T024 [P] [US2] Add `infra/envs/production/variables.tf` (`aws_region`, `domain_name`, `acm_certificate_arn`, `cloudflare_zone_id`, `provisioned_concurrency`) and `infra/envs/production/outputs.tf`

### CD workflow

- [X] T025 [US2] Create `.github/workflows/cd.yml` triggered on `push` to `main`, `concurrency: cd-production` with `cancel-in-progress: false`, OIDC permissions only for jobs that need them
- [X] T026 [US2] Add `build` job to `.github/workflows/cd.yml`: runs `scripts/package-lambda.sh` and `npm ci && npm run build` in `frontend/`, uploads artifact `app-${{ github.sha }}` containing `function.zip` and `frontend/dist/**`, retention 30 days
- [X] T027 [US2] Add `terraform-apply` job to `.github/workflows/cd.yml`: assumes `AWS_DEPLOY_ROLE_ARN`, `terraform -chdir=infra/envs/production apply -auto-approve`, exposes outputs (`lambda_function_name`, `frontend_bucket_name`, `api_invoke_url`) as job outputs
- [X] T028 [US2] Add `backend-deploy` job to `.github/workflows/cd.yml` (needs `terraform-apply`): downloads artifact, `aws lambda update-function-code` then `aws lambda publish-version`, then `aws lambda update-alias --name production --function-version <new>`
- [X] T029 [US2] Add `frontend-deploy` job to `.github/workflows/cd.yml` (needs `terraform-apply`): `aws s3 sync frontend/dist/ s3://<bucket>/ --delete`; fetch the Cloudflare API token via `aws secretsmanager get-secret-value --secret-id stocktracker/cloudflare/api_token --query SecretString --output text` into a step output, immediately register it as a masked value via `echo "::add-mask::$TOKEN"`, then `curl` the Cloudflare zone purge endpoint for `https://app.<domain>/` and `https://app.<domain>/index.html`
- [X] T030 [US2] Add `smoke` job to `.github/workflows/cd.yml` (needs `[backend-deploy, frontend-deploy]`): runs `scripts/smoke-check.sh` against the public URLs; fail the workflow on non-2xx
- [X] T031 [US2] Emit a deployment summary to `$GITHUB_STEP_SUMMARY` in `.github/workflows/cd.yml` with the `Deployment` fields from `data-model.md`
- [X] T032 [US2] Create `.github/workflows/rollback.yml` (`workflow_dispatch` with `commit_sha` and `confirm` inputs; require `confirm == "ROLLBACK"`); reuse the `backend-deploy` and `frontend-deploy` steps against the artifact `app-<commit_sha>`

**Checkpoint**: Merging to `main` builds, applies infra, and deploys both tiers; failed smoke does not promote the next deploy. MVP is complete after this phase.

---

## Phase 5: User Story 3 — Reproducible Infrastructure as Code (Priority: P2)

**Goal**: Infrastructure changes flow through PR with a `terraform plan` preview; drift between declared and actual state is reported.

**Independent Test**: Manually edit a managed resource in the AWS console (e.g., add a tag to the Lambda); a scheduled drift-check workflow flags it within 24 h.

- [X] T033 [US3] Confirm the existing `terraform-plan` PR job from US1 attaches the plan output as a PR comment (extend `actions/github-script` step in `.github/workflows/ci.yml` to upsert a single comment keyed by `<!-- tf-plan -->`)
- [X] T034 [US3] Create `.github/workflows/drift-check.yml` on `schedule: cron 0 2 * * *` that runs `terraform plan -detailed-exitcode -refresh-only` against `infra/envs/production/` and opens a GitHub issue when exit code is `2`
- [X] T035 [P] [US3] Add `infra/README.md` section documenting how to provision a new environment by copying `envs/production/` and overriding variables (no module changes required)

**Checkpoint**: Plan/preview visible on every infra PR; drift detected automatically.

---

## Phase 6: User Story 4 — Database Schema Migrations on Deploy (Priority: P2)

**Goal**: Pending Flyway migrations are applied to RDS MySQL before the new application Lambda alias is promoted; failed migrations leave the previous version in service.

**Independent Test**: Add a non-destructive migration (`backend/src/main/resources/db/migration/V*.sql`) creating a new column. Merge → column appears in RDS, backend reads/writes it, `flyway_schema_history` records the version.

- [X] T036 [P] [US4] Author `infra/modules/rds_mysql/` (DB subnet group across two private subnets, parameter group, MySQL 8.0 `db.t4g.small`, no public endpoint, 7-day automated backup retention, security-group ingress only from Lambda SG)
- [X] T036a [US4/US5] **REWORK per spec FR-025a clarification (2026-06-01)**: set `manage_master_user_password = true` on the RDS instance in `infra/modules/rds_mysql/` so RDS owns the master credential in Secrets Manager (no plaintext in Terraform state). Output the RDS-managed secret ARN (`master_user_secret[0].secret_arn`) for the Lambda to consume. Remove any module-supplied `master_password`/`random_password` plumbing.
- [X] T037 [US4] Author sibling module `infra/modules/lambda_migrator/` — same `function.zip` artifact, different handler entry that runs Flyway and exits, with its own IAM role (Secrets Manager + RDS network access only) and its own CloudWatch log group; wire into `infra/envs/production/main.tf`
- [X] T038 [US4] Add a Quarkus profile or alternate main class in `backend/` so the migrator Lambda invocation runs Flyway `migrate` against the configured datasource and exits with non-zero on failure (use Quarkus's existing `quarkus-flyway` extension)
- [X] T039 [US4] Insert a `db-migrate` job into `.github/workflows/cd.yml` between `terraform-apply` and `backend-deploy` that synchronously invokes the migrator Lambda (`aws lambda invoke --invocation-type RequestResponse`) and fails the workflow on non-zero status or non-empty `FunctionError`
- [X] T040 [US4] Document migration authoring conventions and the forward-only rule in `backend/README.md` (filename pattern, non-destructive guidance, link to `quickstart.md` troubleshooting)

**Checkpoint**: Schema migrations gate the backend promote; failure preserves prior version.

---

## Phase 7: User Story 5 — Secrets and Configuration Management (Priority: P3)

**Goal**: No long-lived AWS keys, no plaintext secrets in code or logs; runtime values fetched from Secrets Manager.

**Independent Test**: Inspect repo + workflow logs (no secrets present); rotate `stocktracker/db/master_password` and confirm next deploy's Lambda picks up the new value.

- [X] T041 [US5] **Per FR-025a clarification (2026-06-01)**: the RDS master password is now an RDS-managed secret (see T036a), so `infra/modules/secrets/` MUST NOT create a `stocktracker/db/master_password` entry. Since no other runtime third-party secrets exist in v1 (Cloudflare dropped per Phase 9), the `secrets` module is not required — do not author it. If a future feature introduces a standalone runtime secret, create the module then.
- [X] T042 [US5] In `infra/envs/production/main.tf`, pass the **RDS-managed** secret ARN (from the `rds_mysql` module output, T036a) into `lambda_backend`. No separate `secrets` module wiring (T041 N/A); no Cloudflare/frontend_bucket wiring (Phase 9).
- [X] T043 [US5] Attach the AWS Lambda Secrets Manager Extension layer to the application Lambda in `infra/modules/lambda_backend/` and set `DATASOURCE_PASSWORD_SECRET_ARN` to the **RDS-managed** secret ARN; configure Quarkus datasource password resolution to read the cached secret value at startup (resolved once per container lifetime — a rotated password is picked up on the next deploy/cold start, per spec US5 AS2).
- [X] T044 [US5] Confirm OIDC-only AWS auth (no `AWS_ACCESS_KEY_ID` references anywhere in `.github/workflows/`). Add `add-mask` calls in `cd.yml` only if a future feature pulls secret values into the runner — none currently does (the Cloudflare API token path was removed per Phase 9; CloudFront invalidation uses the deploy IAM role).
- [X] T044a [US5] **Per FR-025b clarification (2026-06-01)**: remove the interim `RDS_MASTER_PASSWORD` GitHub Actions repository secret and any workflow reference to it, once T036a/T043 (RDS-managed secret path) are in place, leaving no statically-stored DB password outside Secrets Manager.
- [X] T045 [US5] Add a `scripts/scan-secrets.sh` invocation to the `gates` job in `.github/workflows/ci.yml` (uses `gitleaks` GitHub Action) scanning the **full git history** (not diff-only — e.g. `gitleaks detect --source . --redact`), so plaintext secrets anywhere in history fail the required check (FR-026a, SC-004)

**Checkpoint**: All FR-024..FR-026 requirements satisfied and enforced at PR time.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T046 [P] Add CloudWatch log retention (14 days) to all log groups created in `infra/modules/lambda_backend/`, `infra/modules/lambda_migrator/`, and `infra/modules/api_gateway/`; verify the application Lambda emits a structured error log line on uncaught exceptions (Quarkus default + custom `ExceptionMapper` in `backend/src/main/java/com/stocktracker/api/ApiExceptionMapper.java` already covers this — assert via local test) — satisfies FR-027
- [ ] T047 [P] Validate `quickstart.md` end-to-end against the provisioned environment and correct any drift in commands or output names
- [ ] T048 Tag all AWS resources via a Terraform `default_tags` block in `infra/envs/production/main.tf` (`Project=stocktracker`, `Environment=production`, `ManagedBy=terraform`)
- [ ] T049 [P] Update `CLAUDE.md` SPECKIT marker to point to `specs/003-ci-cd-aws/plan.md` (verify already updated by `/speckit-plan`)
- [ ] T050 Run a full dry-run from a clean clone: bootstrap apply → first env apply → push to `main` → confirm SC-001..SC-008 measurably hold; record results in the PR description

---

## Phase 9: CloudFront Migration & Stack Split

**Purpose**: Replace the Cloudflare-based frontend edge with AWS CloudFront, and split production into a persistent stack (CloudFront + frontend bucket — kept up between sessions) and an ephemeral stack (VPC + RDS + Lambda + API Gateway — apply/destroy per 2-hour test session) so that the cost-free CloudFront does not pay the 10–20 min disable-and-delete wait on every teardown.

**Why**: Drop the second-vendor dependency, simplify origin auth via CloudFront OAC, eliminate the Cloudflare API token + shared-secret header. Stack split avoids redundant CloudFront recreation per session.

**Independent Test**: After applying the persistent stack once, the ephemeral stack can be applied and destroyed repeatedly in <10 min each direction; the CloudFront URL keeps serving the last-deployed bundle across ephemeral teardowns.

### Bootstrap permissions

- [X] T051 Edit `infra/bootstrap/main.tf` `gha_deploy_permissions` policy: add a new `ManageCloudFront` statement (no region condition — CloudFront is global) allowing `cloudfront:CreateDistribution`, `cloudfront:UpdateDistribution`, `cloudfront:DeleteDistribution`, `cloudfront:GetDistribution*`, `cloudfront:ListDistributions`, `cloudfront:TagResource`, `cloudfront:UntagResource`, `cloudfront:ListTagsForResource`, `cloudfront:CreateOriginAccessControl`, `cloudfront:GetOriginAccessControl`, `cloudfront:UpdateOriginAccessControl`, `cloudfront:DeleteOriginAccessControl`, `cloudfront:ListOriginAccessControls`, `cloudfront:CreateInvalidation`, `cloudfront:GetInvalidation`, `cloudfront:ListInvalidations`. Re-apply bootstrap once.

### Terraform module changes

- [X] T052 Author `infra/modules/cloudfront/` — `aws_cloudfront_origin_access_control` (signing_protocol `sigv4`, origin_type `s3`); `aws_cloudfront_distribution` with single S3 origin (REST endpoint, OAC attached), `default_root_object = "index.html"`, custom_error_response 403 → 200 `/index.html`, custom_error_response 404 → 200 `/index.html` (SPA fallback), `price_class = "PriceClass_100"`, default cert (`cloudfront_default_certificate = true`, no aliases). Outputs: `distribution_id`, `domain_name`, `arn`.
- [X] T053 Update `infra/modules/frontend_bucket/main.tf`: replace the `aws:Referer` shared-secret bucket policy with an OAC bucket policy — `Principal: { Service: "cloudfront.amazonaws.com" }`, `Action: s3:GetObject`, `Condition: StringEquals { "AWS:SourceArn": var.cloudfront_distribution_arn }`. Drop `origin_shared_secret` variable. The bucket no longer needs S3 website configuration; serve via REST endpoint as the CloudFront origin.
- [X] T054 Delete `infra/modules/cloudflare/` (the entire module directory).

### Persistent stack (new)

- [X] T055 Create `infra/envs/production-persistent/` with `main.tf`, `variables.tf`, `outputs.tf`, `backend.tf`. `backend.tf` uses the same S3 backend bucket from bootstrap with state key `production/persistent.tfstate`. `main.tf` composes `module "frontend_bucket"` and `module "cloudfront"` (cloudfront's S3 origin = bucket regional domain name; bucket's `cloudfront_distribution_arn` = cloudfront output). Outputs: `cloudfront_distribution_id`, `cloudfront_domain_name`, `frontend_bucket_name`.

### Ephemeral stack (slim down)

- [X] T056 Edit `infra/envs/production/main.tf`: remove the Cloudflare provider block, the `enable_cloudflare` local, `module "frontend_bucket"`, and `module "cloudflare"`. Add `data "terraform_remote_state" "persistent"` reading `production/persistent.tfstate`. Pass `data.terraform_remote_state.persistent.outputs.cloudfront_domain_name` to the API Gateway CORS allow-origin config.
- [X] T057 Edit `infra/envs/production/variables.tf` and `outputs.tf`: remove `cloudflare_zone_id`, `cloudflare_api_token`, `domain_name`, `acm_certificate_arn` (no custom domain in v1). Remove `frontend_bucket_name` output (it's now a persistent-stack output).

### Workflow changes

- [X] T058 Edit `.github/workflows/cd.yml`: remove `TF_VAR_cloudflare_*` env vars; remove `cloudflare_zone_id` output capture; the `terraform-apply` job operates only on `infra/envs/production/`. In the `frontend-deploy` job: source `DIST_ID` and `BUCKET_NAME` from the persistent stack via a small step that runs `terraform -chdir=infra/envs/production-persistent init -backend=true && terraform -chdir=infra/envs/production-persistent output -raw cloudfront_distribution_id` (and likewise for `frontend_bucket_name`). Replace the Cloudflare cache-purge curl step with `aws cloudfront create-invalidation --distribution-id "$DIST_ID" --paths "/" "/index.html"`. Drop the Cloudflare API token Secrets Manager fetch + `add-mask`.
- [X] T059 Create `.github/workflows/cd-persistent.yml` — triggers: `push` to `main` filtered to paths `infra/envs/production-persistent/**`, `infra/modules/cloudfront/**`, `infra/modules/frontend_bucket/**`, plus `workflow_dispatch`. Single job that assumes `AWS_DEPLOY_ROLE_ARN`, runs `terraform -chdir=infra/envs/production-persistent init/plan/apply`. `concurrency: cd-persistent` group.
- [X] T060 Create `.github/workflows/destroy-persistent.yml` — `workflow_dispatch` only with a `confirm` input requiring the literal string `DESTROY-PERSISTENT`. Runs `terraform -chdir=infra/envs/production-persistent destroy -auto-approve`. Document in `quickstart.md` that this is a rare full-wipe operation.
- [X] T061 Edit `.github/workflows/destroy.yml`: scope to `infra/envs/production/` only (ephemeral). Add a comment header that the persistent stack is intentionally untouched. Remove `TF_VAR_cloudflare_*` env vars.
- [X] T062 Edit `.github/workflows/drift-check.yml`: matrix across both stacks (`production-persistent`, `production`). Remove `TF_VAR_cloudflare_*` env vars.
- [X] T063 Edit `.github/workflows/ci.yml` and `.github/workflows/rollback.yml`: remove `TF_VAR_cloudflare_*` env vars and Cloudflare-token masking. `rollback.yml` swaps the cache purge step for the same `aws cloudfront create-invalidation` call as `cd.yml`. `ci.yml` runs `terraform plan` for both stacks (matrix).

### Secrets module

- [X] T064 Edit `infra/modules/secrets/`: ~~remove cloudflare entries~~. **N/A** — the secrets module has not been created yet (Phase 7 / T041 is pending). When T041 is eventually authored it should create only `stocktracker/db/master_password` and skip the previously-planned Cloudflare entries. The Cloudflare provider variables and `random_password` resource have already been removed from `infra/envs/production/main.tf` as part of T056.

### Verification

- [ ] T065 End-to-end verification (live AWS — defer until next provision cycle): (a) `terraform -chdir=infra/envs/production-persistent apply` succeeds and CloudFront reaches `Deployed`; (b) `terraform -chdir=infra/envs/production apply` succeeds reading persistent outputs; (c) `curl -I https://<cloudfront_domain_name>/` returns `200`; (d) `curl -I https://<bucket>.s3.<region>.amazonaws.com/index.html` returns `403`; (e) trigger `cd.yml` and confirm `aws cloudfront get-invalidation` shows `Status: Completed`; (f) run `destroy.yml` — finishes in <10 min, persistent CloudFront URL still serves; (g) re-apply ephemeral — also <10 min.

**Local-only verification (already done as part of this implementation):**
- `terraform fmt -recursive` clean.
- `terraform validate` passes for `infra/bootstrap/`, `infra/envs/production-persistent/`, `infra/envs/production/`.
- `grep -ri cloudflare infra/ .github/` returns zero hits (excluding `.terraform/` provider caches which will clear on re-init).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Phase 1; **blocks all user stories**.
- **US1 (Phase 3)**: needs Phase 2 (OIDC plan role exists for `terraform-plan` job).
- **US2 (Phase 4)**: needs Phase 2; consumes modules created in this phase.
- **US3 (Phase 5)**: needs Phase 4 (`terraform-plan` job already exists; this phase extends it and adds drift-check).
- **US4 (Phase 6)**: needs Phase 4 (consumes Lambda + networking; inserts a job into `cd.yml`).
- **US5 (Phase 7)**: needs Phase 4 (modules exist to wire secret ARNs into).
- **Polish (Phase 8)**: after all desired user stories.
- **CloudFront Migration (Phase 9)**: supersedes the Cloudflare-specific tasks in Phases 4 and 7. T051 (bootstrap perms) must run first; T052–T055 (modules + persistent stack) before T056–T057 (ephemeral slim-down); T058–T063 (workflows) after the Terraform changes; T064 (secrets) and T065 (verify) last.

### User Story Independence

- US1 (CI) is fully independent of AWS infrastructure and can ship alone.
- US2 is the first story that touches AWS; it depends only on Phase 2 bootstrap.
- US3, US4, US5 each extend US2 but are independently testable.

### Within Each User Story

- Module authoring tasks marked [P] can run in parallel (different files).
- The composing `main.tf` task (e.g., T023) waits on its parallel module tasks.
- Workflow YAML edits to the same file (e.g., `ci.yml`, `cd.yml`) are serial within that file.

---

## Parallel Example: User Story 2

```bash
# All five Terraform module authoring tasks touch different files — run in parallel:
Task: "Author infra/modules/network/"
Task: "Author infra/modules/lambda_backend/"
Task: "Author infra/modules/api_gateway/"
Task: "Author infra/modules/frontend_bucket/"
Task: "Author infra/modules/cloudflare/"

# Then compose:
Task: "Wire infra/envs/production/main.tf"
```

---

## Implementation Strategy

### MVP (US1 + US2)

1. Phase 1 → Phase 2 bootstrap (manual `terraform apply` of `infra/bootstrap/`).
2. Phase 3 (US1) — CI in place; merge protection live.
3. Phase 4 (US2) — first end-to-end deploy to `production`.
4. Stop and validate against SC-001, SC-002, SC-007, SC-008.

### Incremental Delivery

5. Phase 5 (US3): infra plans visible on PRs; drift detection scheduled.
6. Phase 6 (US4): schema migrations safely automated.
7. Phase 7 (US5): secret hygiene hardened and enforced in CI.
8. Phase 8: tags, log retention, end-to-end quickstart validation.

### Parallel Team Strategy

After Phase 2: one engineer drives US1 (CI YAML) while another drafts the US2 Terraform modules in parallel — they meet at T025 (`cd.yml`) once modules and CI are in place.

---

## Notes

- [P] = different files, no dependencies on incomplete tasks.
- [Story] label maps each task to a user story for traceability.
- All YAML edits within a single workflow file are serial; cross-file edits can run [P].
- Constitution gates (test, lint, build) run inside the CI workflow itself — no separate task list needed.
- Avoid: introducing staging or multi-region scaffolding in this feature (out of scope per `research.md` R-014).
