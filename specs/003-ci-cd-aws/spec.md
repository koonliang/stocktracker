# Feature Specification: CI/CD Pipeline and AWS Deployment

**Feature Branch**: `003-ci-cd-aws`
**Created**: 2026-05-01
**Status**: Draft
**Input**: User description: "CI feature: github action for PR creation and merge to main. CD feature to AWS via terraform: deploy backend to AWS lambda, provision MySQL RDS, frontend to S3 bucket. Frontend flow: CloudFront CDN -> S3 (private origin, OAC). Backend flow: Lambda -> MySQL"

## Clarifications

### Session 2026-06-01

- Q: How should the RDS MySQL master password be coordinated with the Secrets Manager secret that Lambda reads? → A: RDS-managed secret (`manage_master_user_password = true`); RDS owns the secret in Secrets Manager and Lambda reads that RDS-managed secret ARN.
- Q: When the DB master password is rotated, how should the running backend Lambda pick up the new value? → A: On the next deploy / cold start; the secret is resolved at startup and cached for the container lifetime (no in-lifetime refresh).
- Q: What scope should the gitleaks secret-scan gate cover on PRs? → A: Full git history (scan the entire repository on each PR, not just the PR diff).
- Q: Are there any other runtime third-party secrets the backend needs in v1? → A: None. The RDS-managed DB password is the only runtime secret. The existing `RDS_MASTER_PASSWORD` GitHub repo secret is interim and MUST be removed once Phase 7 (RDS-managed secret) is implemented.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automated PR Validation (Priority: P1)

As a developer, when I open a pull request against `main`, an automated pipeline runs build, tests, and quality checks for both the backend and frontend so that broken code never lands on `main`.

**Why this priority**: Protecting `main` from regressions is the foundation of any deployment pipeline. Without trustworthy CI, automated CD is unsafe. This is the minimum viable slice.

**Independent Test**: Open a PR with intentionally failing tests; verify the PR is blocked from merging. Open a PR with passing tests; verify all required checks turn green and the PR becomes mergeable.

**Acceptance Scenarios**:

1. **Given** a PR is opened against `main`, **When** the pipeline runs, **Then** backend build/tests and frontend build/tests both execute and report status back to the PR.
2. **Given** any required check fails on a PR, **When** a reviewer attempts to merge, **Then** the merge is blocked until the check passes.
3. **Given** a PR is updated with a new commit, **When** the pipeline runs, **Then** stale results are superseded and only the latest commit's status is required for merge.

---

### User Story 2 - Continuous Deployment to AWS on Merge (Priority: P1)

As a maintainer, when a PR is merged into `main`, the latest backend and frontend artifacts are automatically deployed to AWS so that the production environment always reflects the trunk.

**Why this priority**: This is the headline value of the feature — pushing accepted changes to a live, internet-reachable environment without manual steps.

**Independent Test**: Merge a PR that changes a visible string in the frontend and a response field in the backend. Within a documented time window, verify the change is visible at the production URL and the backend API endpoint returns the new field.

**Acceptance Scenarios**:

1. **Given** a commit lands on `main`, **When** the deployment pipeline runs, **Then** the backend artifact is deployed to AWS Lambda and the frontend bundle is uploaded to S3.
2. **Given** the frontend has been deployed, **When** an end user requests the site URL, **Then** the response is served via AWS CloudFront backed by the private S3 origin.
3. **Given** a deployment fails partway through, **When** the pipeline detects the failure, **Then** it halts subsequent steps, surfaces the error, and leaves the previously running version serving traffic.
4. **Given** a deployment completes, **When** a smoke check runs against the public URL and a backend health endpoint, **Then** both return success before the deployment is marked complete.

---

### User Story 3 - Reproducible Infrastructure as Code (Priority: P2)

As an operator, I can provision or rebuild the entire AWS environment (Lambda, MySQL RDS, S3 bucket, CloudFront distribution, networking, IAM) from version-controlled infrastructure definitions so that the environment is reproducible and auditable.

**Why this priority**: Reproducibility, disaster recovery, and review-ability of infrastructure changes. Important but depends on US1/US2 being in place to be valuable.

**Independent Test**: From a clean AWS account (or a separate environment name), run the infrastructure provisioning workflow and verify all resources come up healthy and the deployment pipeline can target the new environment without code changes.

**Acceptance Scenarios**:

1. **Given** the infrastructure definition is changed in a PR, **When** the PR pipeline runs, **Then** a plan/preview of infrastructure changes is produced and attached to the PR for review.
2. **Given** an infrastructure change is approved and merged, **When** the apply step runs, **Then** AWS resources converge to the declared state and the change is recorded.
3. **Given** an infrastructure resource is manually altered in AWS, **When** the next plan runs, **Then** the drift is detected and reported.

---

### User Story 4 - Database Schema Migrations on Deploy (Priority: P2)

As a developer, when backend changes include schema updates, the deployment pipeline applies migrations to the managed MySQL database before the new backend version starts serving traffic so that runtime errors from schema mismatches are avoided.

**Why this priority**: Required to safely evolve the backend, but only relevant once US1/US2 exist.

**Independent Test**: Submit a PR adding a non-destructive column. After merge, verify the column exists in the managed database and the deployed backend reads/writes it without error.

**Acceptance Scenarios**:

1. **Given** a deployment includes pending migrations, **When** the pipeline runs, **Then** migrations are applied before the new Lambda version is promoted to receive traffic.
2. **Given** a migration fails, **When** the failure occurs, **Then** the new backend version is not promoted and the previous version continues serving.

---

### User Story 5 - Secrets and Configuration Management (Priority: P3)

As an operator, sensitive values (database credentials, API keys, deployment credentials) are stored in a managed secret store and injected into the pipeline and runtime so that no secret is committed to the repository or printed in logs.

**Why this priority**: Necessary for security hygiene; can be hardened iteratively after the pipeline is functional.

**Independent Test**: Inspect the repository, pipeline logs, and Lambda configuration; confirm no plaintext secrets are present, and rotate a secret to confirm the runtime picks up the new value on the next deploy.

**Acceptance Scenarios**:

1. **Given** the pipeline needs cloud credentials, **When** a job runs, **Then** it obtains short-lived credentials via a federated identity rather than long-lived static keys.
2. **Given** the backend needs a database password, **When** Lambda starts, **Then** it retrieves the value from the managed secret store at runtime and caches it for the container lifetime; a rotated password is picked up on the next deploy or cold start.

---

### Edge Cases

- A deployment is triggered while a previous one is still running — the pipeline must serialize or cancel-and-supersede deployments to avoid racing apply steps.
- The frontend deploy succeeds but the backend deploy fails (or vice versa) — the system must surface partial-deploy state clearly and not present a mixed version to users without warning.
- CloudFront cache holds a stale frontend bundle after a new deploy — a CloudFront invalidation must run as part of the frontend deploy.
- The managed database is at capacity or unavailable during deploy — the pipeline must fail fast with an actionable error and not promote the new backend.
- A pull request is opened from a fork — the pipeline must run validation checks but must not expose deployment credentials to untrusted code.
- Rolling back: a deployment introduces a regression caught after merge — operators must be able to redeploy a prior known-good commit through the same pipeline.
- Infrastructure state file becomes corrupted or locked — the system must use durable, locked remote state and surface lock contention to the operator.

## Requirements *(mandatory)*

### Functional Requirements

#### Continuous Integration

- **FR-001**: The system MUST run an automated pipeline on every pull request opened or updated against `main`.
- **FR-002**: The PR pipeline MUST build the backend, run backend tests, build the frontend, and run frontend tests.
- **FR-003**: The PR pipeline MUST report individual check statuses back to the pull request and block merging when any required check fails.
- **FR-004**: The PR pipeline MUST cancel or supersede in-flight runs when a newer commit is pushed to the same PR.
- **FR-005**: The PR pipeline MUST run for PRs from forks but MUST NOT expose deployment credentials to those runs.

#### Continuous Deployment

- **FR-006**: On every merge to `main`, the system MUST trigger a deployment pipeline that builds and deploys both the backend and frontend.
- **FR-007**: The deployment pipeline MUST deploy the backend as an AWS Lambda function reachable over HTTPS by the frontend.
- **FR-008**: The deployment pipeline MUST upload the built frontend bundle to an AWS S3 bucket configured as the static origin.
- **FR-009**: End users MUST reach the frontend via AWS CloudFront, with a private S3 bucket as the origin protected by Origin Access Control (OAC); direct public access to the S3 origin MUST be denied.
- **FR-010**: After uploading new frontend assets, the deployment MUST issue a CloudFront invalidation for `/` and `/index.html` so users receive the new version.
- **FR-011**: The deployment pipeline MUST run a post-deploy smoke check against the public frontend URL and a backend health endpoint, and MUST mark the deployment failed if either fails.
- **FR-012**: If any deployment step fails, the system MUST halt subsequent steps and leave the previously running version serving traffic.
- **FR-013**: Operators MUST be able to redeploy a previously merged commit through the same pipeline (rollback by re-deploy).
- **FR-014**: Concurrent deployments to the same environment MUST be serialized or superseded so that two apply steps cannot run at the same time.

#### Infrastructure as Code

- **FR-015**: The AWS environment (Lambda, MySQL RDS, S3 bucket, CloudFront distribution + OAC, networking, IAM roles/policies) MUST be defined as version-controlled Terraform configuration in the repository.
- **FR-015a**: Terraform configuration MUST be split into two stacks with independent state: a **persistent** stack (CloudFront, frontend S3 bucket) that is provisioned once and left up between test sessions, and an **ephemeral** stack (VPC, RDS, Lambda, API Gateway, security groups) that can be provisioned and destroyed per session. The ephemeral stack reads persistent outputs via `terraform_remote_state`. The persistent stack MUST NOT depend on the ephemeral stack.
- **FR-016**: The infrastructure pipeline MUST produce a plan/preview on PRs that touch infrastructure files and attach it to the PR for review.
- **FR-017**: The infrastructure pipeline MUST apply approved changes only after merge to `main`.
- **FR-018**: Terraform state MUST be stored in durable remote storage with locking to prevent concurrent modification.
- **FR-019**: The system MUST detect and report configuration drift between the declared state and actual AWS resources.

#### Database

- **FR-020**: The system MUST provision a managed MySQL database (AWS RDS) reachable only by the backend Lambda over a private network path.
- **FR-021**: The deployment pipeline MUST apply pending database migrations before promoting the new backend version to serve traffic.
- **FR-022**: A failed migration MUST prevent promotion of the new backend version, leaving the previous version in service.
- **FR-023**: The managed database MUST have automated backups enabled with a retention window of at least 7 days.

#### Secrets and Access

- **FR-024**: Pipeline jobs that act on AWS MUST authenticate via a federated identity that issues short-lived credentials (e.g., OIDC), not long-lived static access keys.
- **FR-025**: Runtime secrets (database credentials, third-party API keys) MUST be retrieved from a managed secret store and MUST NOT be committed to the repository.
- **FR-025a**: The RDS master password MUST be an RDS-managed secret (`manage_master_user_password = true`) so that RDS owns and stores the credential in Secrets Manager; the plaintext password MUST NOT appear in Terraform state. The backend Lambda reads this RDS-managed secret ARN at runtime. The DB master password is the only runtime secret in v1; no other third-party API keys are in scope.
- **FR-025b**: The interim `RDS_MASTER_PASSWORD` GitHub Actions repository secret MUST be removed once the RDS-managed secret path (Phase 7) is in place, leaving no statically-stored DB password outside Secrets Manager.
- **FR-026**: Pipeline logs MUST NOT print secret values, and the pipeline MUST mask or redact known secret variables.
- **FR-026a**: The PR pipeline MUST run an automated secret scan over the full git history (not only the PR diff) and fail the required check if any plaintext secret is detected.

#### Observability

- **FR-027**: Backend Lambda invocations and errors MUST be captured in CloudWatch (or equivalent) so operators can diagnose failures after a deploy.
- **FR-028**: Each deployment MUST produce a record (commit, artifact version, timestamp, outcome) discoverable by operators.

### Key Entities *(include if feature involves data)*

- **Pipeline Run**: A single execution of CI or CD triggered by a PR or merge. Attributes: trigger event, commit SHA, status, started/finished timestamps, link to logs.
- **Deployment**: A successful or failed promotion of a specific commit to the AWS environment. Attributes: commit SHA, backend artifact version, frontend bundle version, infrastructure revision, outcome, smoke-check result.
- **Environment**: A named target of deployment (e.g., `production`). Attributes: AWS account/region, public URL, backend endpoint, database identifier.
- **Infrastructure Revision**: A version-controlled snapshot of the declared AWS resources. Attributes: commit SHA, plan summary, apply outcome.
- **Migration**: A schema change applied to the managed database. Attributes: identifier, applied timestamp, status.
- **Secret**: A named sensitive value held in a managed store and consumed by pipeline or runtime. Attributes: name, scope (pipeline/runtime), last-rotated timestamp.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of pull requests targeting `main` automatically run validation checks; no PR can be merged with a failing required check.
- **SC-002**: Average time from merge to fully deployed (frontend visible via CDN and backend serving the new version) is under 15 minutes.
- **SC-003**: At least 95% of deployments triggered from `main` succeed end-to-end without manual intervention over a rolling 30-day window.
- **SC-004**: 0 secrets are committed to the repository or printed in pipeline logs (verified by automated scanning and log review).
- **SC-005**: A clean environment can be provisioned from version-controlled infrastructure definitions in under 60 minutes with no manual cloud-console steps.
- **SC-006**: A previously deployed commit can be redeployed (rollback) by an operator in under 15 minutes.
- **SC-007**: After a frontend deploy, end users see the new bundle within 5 minutes of the deployment being marked complete.
- **SC-008**: Failed deployments leave the previously running version serving traffic in 100% of cases — no failed deploy results in user-visible downtime.

## Assumptions

- The target cloud is a single AWS account and a single region for the initial production environment; multi-region is out of scope for v1.
- Only one deployment environment (`production`) is required for v1; additional environments (staging, preview) may be added later using the same definitions.
- The repository is hosted on GitHub and pipelines are implemented with GitHub Actions.
- v1 uses the default `*.cloudfront.net` hostname for the frontend and the default API Gateway hostname for the API. No custom domain, Route 53 zone, or ACM certificate is provisioned. A custom domain may be added later without breaking the architecture.
- Frontend assets are static; no server-side rendering is required.
- The backend fits within AWS Lambda's runtime, package size, and cold-start tolerance limits established in earlier specs.
- Database schema changes are non-destructive by default and applied forward-only via migrations; destructive changes require an explicit out-of-band process.
- Authentication for end users is out of scope of this feature; only deployment-time and operator authentication are addressed here.
- The team accepts that infrastructure apply runs only after merge to `main`; pre-merge runs are plan/preview only.
