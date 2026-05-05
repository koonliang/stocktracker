# Implementation Plan: CI/CD Pipeline and AWS Deployment

**Branch**: `003-ci-cd-aws` | **Date**: 2026-05-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-ci-cd-aws/spec.md`

## Summary

Wire up a GitHub Actions pipeline that validates every PR (build + test + lint for
backend and frontend, plus `terraform plan` on infra changes) and, on merge to
`main`, deploys both tiers to AWS using Terraform-managed infrastructure. The
backend (Quarkus REST + Hibernate ORM, MySQL) runs on AWS Lambda fronted by an
**API Gateway HTTP API** (decision recorded in research.md — answers the user's
question), with a private MySQL RDS instance reachable only from the Lambda over
a VPC; Flyway migrations run as a one-shot Lambda invocation before the new
backend version is promoted. The frontend (Vite/React static bundle) is uploaded
to a private S3 bucket served exclusively through AWS CloudFront with Origin
Access Control (OAC); a CloudFront invalidation for `/` and `/index.html` runs
at the end of every frontend deploy. Infrastructure is split into two Terraform
stacks: a **persistent** stack (CloudFront + frontend bucket) provisioned once
and left up between sessions, and an **ephemeral** stack (VPC, RDS, Lambda, API
Gateway) that can be torn down per session to save cost — CloudFront's 10–20 min
disable-and-delete wait is paid only on full wipes. Authentication from GitHub to AWS
uses OIDC (no long-lived keys), runtime secrets live in AWS Secrets Manager, and
Terraform state lives in S3 with DynamoDB locking.

## Technical Context

**Language/Version**:
- Pipelines: GitHub Actions YAML
- Infrastructure: Terraform 1.7+ (HCL)
- Backend runtime (existing): Java 21 + Quarkus 3.x, packaged for Lambda via
  `quarkus-amazon-lambda-http`
- Frontend runtime (existing): TypeScript 5.5 / Vite 5 static bundle

**Primary Dependencies**:
- AWS provider for Terraform (`hashicorp/aws` ~> 5.x)
- `aws-actions/configure-aws-credentials@v4` (OIDC federation)
- `hashicorp/setup-terraform@v3`
- Quarkus extensions: `quarkus-amazon-lambda-http`, `quarkus-flyway`,
  `quarkus-jdbc-mysql` (already on `pom.xml`)
- Flyway (already used by Quarkus) for DB migrations
- AWS CLI for `aws cloudfront create-invalidation` in the deploy workflow

**Storage**:
- Application data: AWS RDS for MySQL (managed, single-AZ for v1; Multi-AZ in
  prod is an upgrade path noted in research)
- Frontend assets: AWS S3 bucket (private, accessed only by CloudFront via Origin
  Access Control — SigV4 signed origin requests; bucket policy keyed to the
  distribution ARN)
- Terraform state: S3 bucket + DynamoDB table for state locking; persistent and
  ephemeral stacks use distinct state keys in the same backend
- Secrets: AWS Secrets Manager (DB credentials only; CloudFront invalidation uses
  the deploy IAM role and needs no API token)

**Testing**:
- Backend: existing `quarkus-junit5` + `rest-assured` suite runs in CI
- Frontend: existing Vitest suite runs in CI
- Infrastructure: `terraform fmt -check`, `terraform validate`, `terraform plan`
  on PRs; `tflint` for static checks
- Smoke: post-deploy `curl` against `/q/health` (Quarkus SmallRye Health) and the
  CloudFront frontend URL

**Target Platform**:
- Backend: AWS Lambda (x86_64, `java21` managed runtime, regional API Gateway
  HTTP API) in `ap-southeast-1` (Singapore) for proximity to Temus team —
  configurable via Terraform variable
- Frontend: S3 (private origin, OAC) fronted by CloudFront on the default
  `*.cloudfront.net` hostname with the default CloudFront TLS cert (no custom
  domain in v1)

**Project Type**: Web application — existing `backend/` (Quarkus) + `frontend/`
(Vite/React) trees; this feature adds an `infra/` tree for Terraform and a
`.github/workflows/` tree for pipelines.

**Performance Goals**:
- Cold-start P95 for the Quarkus Lambda: < 4 s (mitigated by JVM tuning;
  alternative GraalVM native image deferred — see research)
- Warm P95 backend latency: < 500 ms for non-DB endpoints, < 1 s for DB-backed
  endpoints
- Pipeline wall-clock from merge to deployed: < 15 min (SC-002)
- Cache purge → user sees new bundle: < 5 min (SC-007)

**Constraints**:
- No long-lived AWS access keys in the repo or in GitHub (OIDC only)
- S3 frontend bucket MUST NOT be publicly readable (CloudFront-only access via OAC)
- RDS MUST NOT have a public endpoint
- Lambda cold-start must remain acceptable for an interactive UI; if Quarkus JVM
  cold-start exceeds tolerance, fall back to provisioned concurrency on the hot
  path rather than pivoting to native compilation in this iteration (YAGNI)

**Scale/Scope**:
- Single environment (`production`) in v1; one AWS account, one region
- Expected traffic: < 100 RPS sustained, < 50 concurrent Lambda executions
- Single MySQL RDS instance, `db.t4g.small` class for v1
- Single CloudFront distribution, single S3 bucket

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Test Verification (NON-NEGOTIABLE) | PASS | CI runs the existing backend and frontend test suites on every PR; deployment is gated on green tests. Smoke tests run post-deploy. |
| II. Lint & Style Compliance (NON-NEGOTIABLE) | PASS | CI runs Spotless (backend), ESLint + `tsc --noEmit` (frontend), `terraform fmt -check`, `tflint`. |
| III. Compilation Integrity (NON-NEGOTIABLE) | PASS | CI runs `mvn -B verify` and `npm run build`. Deploy uses the same artifact as CI built. |
| IV. Simplicity & YAGNI | PASS | One environment, one region, single Lambda, single RDS, single S3 bucket, single CloudFront distribution. No multi-account, no blue/green, no Aurora, no native image, no custom domain, no AWS WAF, no second-vendor edge. |
| V. Specification-Driven Development | PASS | Plan derives from `spec.md`; any scope change must update the spec first. |

**Result**: All gates pass with no justified violations. `Complexity Tracking` table omitted.

## Project Structure

### Documentation (this feature)

```text
specs/003-ci-cd-aws/
├── plan.md              # This file
├── research.md          # Phase 0 output — incl. APIGW vs Function URL decision
├── data-model.md        # Phase 1 output — pipeline/deployment/infra entities
├── quickstart.md        # Phase 1 output — operator runbook
├── contracts/           # Phase 1 output — workflow/job contracts, IAM trust
│   ├── github-oidc-trust.md
│   ├── pipeline-contracts.md
│   └── http-api-contract.md
└── tasks.md             # /speckit-tasks output — NOT created here
```

### Source Code (repository root)

```text
.github/
└── workflows/
    ├── ci.yml                      # PR validation: backend + frontend + infra plan (both stacks)
    ├── cd.yml                      # On push to main: build, apply ephemeral stack, deploy app
    ├── cd-persistent.yml           # Apply persistent stack on changes under its paths
    ├── destroy.yml                 # Tear down ephemeral stack only
    ├── destroy-persistent.yml      # Manual-only full wipe of persistent stack (rare)
    ├── drift-check.yml             # Drift detection across both stacks
    └── rollback.yml                # Manual workflow: redeploy a chosen commit SHA

backend/                            # Existing Quarkus app
├── pom.xml                         # Add quarkus-amazon-lambda-http profile
└── src/...

frontend/                           # Existing Vite app
└── src/...

infra/                              # NEW — Terraform IaC
├── bootstrap/                      # Remote state + OIDC + plan/deploy IAM roles
├── envs/
│   ├── production-persistent/      # PERSISTENT stack — provisioned once, kept up
│   │   ├── main.tf                 # frontend_bucket + cloudfront modules
│   │   ├── variables.tf
│   │   ├── outputs.tf              # cloudfront_distribution_id, domain_name, bucket_name
│   │   └── backend.tf              # state key: production/persistent.tfstate
│   └── production/                 # EPHEMERAL stack — apply/destroy per session
│       ├── main.tf                 # network + rds + lambda + api_gateway + secrets
│       ├── variables.tf
│       ├── outputs.tf
│       └── backend.tf              # state key: production/terraform.tfstate
│                                   # reads persistent outputs via terraform_remote_state
├── modules/
│   ├── network/                    # VPC, private subnets, NAT/VPC endpoints
│   ├── lambda_backend/             # Application Lambda + execution role + log group
│   ├── lambda_migrator/            # One-shot Flyway migrator Lambda + role + log group
│   ├── api_gateway/                # HTTP API + stage; CORS allowed origin from
│   │                               # persistent CloudFront domain
│   ├── rds_mysql/                  # DB subnet group, parameter group, instance
│   ├── secrets/                    # Secrets Manager entries (DB credentials only)
│   ├── frontend_bucket/            # S3 (private) + bucket policy keyed to the
│   │                               # CloudFront distribution ARN (OAC)
│   └── cloudfront/                 # CloudFront distribution + OAC + SPA error responses
└── README.md

scripts/
├── package-lambda.sh               # Build the Quarkus Lambda zip from backend/
└── smoke-check.sh                  # Post-deploy health probe (used by cd.yml)
```

**Structure Decision**: Multi-tier web app (existing `backend/` + `frontend/`)
plus a new `infra/` tree organized as `envs/<env>` consuming reusable `modules/`.
A single environment (`production`) is shipped in v1; the layout is ready for
`envs/staging` to be added later by copying one directory and changing variable
values, without restructuring modules — but staging is explicitly **out of scope
for this feature** (YAGNI).

## Complexity Tracking

> Constitution Check passed with no violations; this section is intentionally
> empty.
