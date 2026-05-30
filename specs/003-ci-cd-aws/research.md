# Phase 0 Research: CI/CD Pipeline and AWS Deployment

This document records architectural decisions, the reasoning behind them, and
the alternatives considered. Each section follows the **Decision / Rationale /
Alternatives** format.

---

## R-001: API Gateway in front of Lambda? (User question)

**Decision**: Yes — use **AWS API Gateway HTTP API** in front of the backend
Lambda. Path: `Browser → CloudFront → S3 (private origin, OAC)` for the static
site, and `Browser → API Gateway HTTP API → Lambda → MySQL RDS (private)` for
the API. v1 uses the default CloudFront and API Gateway hostnames (no custom
domain).

**Rationale**:

1. **It is the canonical pattern for an HTTP backend on Lambda.** API Gateway
   gives a stable HTTPS endpoint with a custom domain, ACM TLS, per-route
   throttling, request/response logging to CloudWatch, structured access logs,
   and a clean separation between "edge" and "compute" concerns. None of this
   has to be reinvented in application code.
2. **HTTP API (not REST API) keeps it cheap and simple.** HTTP API is roughly
   one-third the price of REST API at our expected volume (< 100 RPS) and
   supports the features we need: custom domain, JWT authorizer (future), CORS,
   throttling, automatic deployment stages.
3. **It plays naturally with Quarkus.** The `quarkus-amazon-lambda-http`
   extension is built specifically for the API Gateway HTTP API v2 event format
   (`APIGatewayV2HTTPEvent`). A Lambda Function URL uses a different (similar
   but not identical) event shape; supporting it requires a different adapter
   path and forfeits some Quarkus-supported features (e.g., the bundled
   `awslambda-deployment` profile is tuned for API Gateway).
4. **Single-provider edge.** Both frontend (CloudFront → S3) and API (API
   Gateway → Lambda) live entirely within AWS. One IAM model, one billing
   surface, one set of logs. CloudFront does not sit in front of the API
   either — keeping the API edge on API Gateway avoids extra latency for
   non-idempotent traffic and keeps the failure domain small.
5. **It opens upgrade paths cheaply.** WAF, usage plans, custom authorizers,
   and per-route throttling are configuration, not rework, if we ever need
   them. Function URLs cannot grow into these without bolting on CloudFront.

**Alternatives considered**:

- **Lambda Function URL (no API Gateway).** Cheapest option (free; you only pay
  for Lambda invocations) and simplest to provision. *Rejected* because: (a) no
  custom domain support without putting CloudFront in front (which adds the
  exact latency, cost, and complexity that "just use Function URL" was supposed
  to avoid); (b) IAM-only auth — no JWT authorizer; (c) no per-route throttling
  or request validation; (d) Quarkus's primary Lambda HTTP integration is
  tailored to API Gateway HTTP API events. The savings (~$1/million requests)
  do not justify the lost capability and the future migration work.
- **API Gateway REST API.** *Rejected* — three times the price, and we do not
  need any feature exclusive to REST API (request/response transformation,
  usage plans with API keys, edge-optimized endpoints with built-in CloudFront,
  WebSocket — out of scope). HTTP API is the right tier.
- **Application Load Balancer (ALB) → Lambda target.** *Rejected* — ALB is
  designed for long-lived containers/instances, costs ~$16/month minimum even
  idle, and gives us no API-shaped features (throttling per route, request
  validation). Reasonable for hybrid container+Lambda fleets; overkill here.
- **CloudFront in front of API Gateway as well.** *Rejected for v1* — adds
  edge latency for non-idempotent API traffic, requires extra cache-bypass
  configuration, and provides no benefit until we want a custom domain
  shared between frontend and API. Revisit if a unified edge is ever needed.

**Considerations / trade-offs to be aware of**:

- **Cost**: HTTP API is ~$1.00 per million requests + Lambda costs. At 100 RPS
  sustained that is ~$2.59/day for API Gateway alone — acceptable.
- **Cold start**: API Gateway adds < 10 ms overhead; the dominant cost is the
  Quarkus JVM cold start. Mitigation: see R-003.
- **CORS**: Configured at the HTTP API stage. Allowed origin is sourced from
  the persistent stack's `cloudfront_domain_name` output via
  `terraform_remote_state` so the value is always in sync with the live
  CloudFront distribution. Handled in Terraform; no app-side code needed.
- **Auth**: For v1 the API is unauthenticated (the spec deferred end-user auth).
  When auth is added, an HTTP API JWT authorizer is the natural extension.
- **Observability**: API Gateway access logs → CloudWatch Logs; Lambda logs →
  CloudWatch Logs. Both are queryable via Logs Insights.

---

## R-002: Quarkus packaging for Lambda — JVM vs native image

**Decision**: JVM Lambda (Java 21 managed runtime) using
`quarkus-amazon-lambda-http`, packaged as a regular Maven build artifact
(`function.zip`). Native (GraalVM) image is **not** used in v1.

**Rationale**: Native compilation is a separate, non-trivial build pipeline
(GraalVM, longer builds, reflection configuration, occasional library
incompatibility). The cold-start improvement is real (≈ 200 ms native vs
2–4 s JVM) but the spec accepts a < 15 min deploy budget and < 1 s warm-path
P95 — both achievable in JVM mode. If cold-start latency turns out to be a
real user problem, the simpler mitigation is **provisioned concurrency** on
the production alias (configurable in Terraform) — a config knob, not a build
overhaul. YAGNI per Principle IV.

**Alternatives considered**:

- **Quarkus native image on `provided.al2` runtime.** Better cold-start, much
  more involved CI build. Defer until measured need.
- **Java 17 instead of Java 21.** No reason to pick the older LTS; Quarkus
  3.x and the project already target Java 21.

---

## R-003: Lambda cold-start mitigation strategy

**Decision**: Two-step strategy — first ship without provisioned concurrency
and observe cold-start frequency in CloudWatch. If P95 cold-start exceeds
4 s on the warm path, add **provisioned concurrency = 1** on the Lambda
production alias via Terraform variable. Tune memory to **1024 MB** initially
(more memory = more vCPU = faster JVM warm-up).

**Rationale**: Avoids paying for provisioned concurrency before we know we
need it; keeps the Terraform change one-line if we do.

**Alternatives considered**:

- **Always-on provisioned concurrency.** Costs continuously even at idle.
  Reject for v1 traffic profile.
- **Lambda SnapStart for Java.** Genuinely interesting (snapshots a
  pre-initialized JVM) and supported on Java 21. *Defer to v1.1* — keep
  research-flagged so we revisit after the first production data.

---

## R-004: API Gateway → Lambda integration — proxy vs non-proxy

**Decision**: AWS_PROXY (Lambda proxy integration) using API Gateway HTTP API
v2 event payload format.

**Rationale**: `quarkus-amazon-lambda-http` already speaks v2 proxy events end
to end; routing is handled inside Quarkus REST. Non-proxy (template-mapped)
integrations duplicate routing in API Gateway and create a second source of
truth. Proxy is the path of least surprise.

**Alternatives considered**: Non-proxy with per-route mapping templates —
more configuration to maintain in Terraform, no benefit at our scale.

---

## R-005: RDS MySQL configuration

**Decision**:

- Engine: MySQL 8.0 on RDS, instance class `db.t4g.small` (2 vCPU, 2 GB RAM).
- Network: deployed into the VPC's private subnets; Lambda runs in the same
  VPC's private subnets and reaches RDS via a security-group rule keyed to
  the Lambda's SG.
- High availability: single-AZ for v1.
- Backups: 7-day automated retention, daily snapshot window outside business
  hours.
- Credentials: master password stored in AWS Secrets Manager; rotation not
  enabled in v1 (manual rotation supported).

**Rationale**: Smallest defensible footprint that satisfies FR-020/-023.
Multi-AZ doubles cost and is unnecessary at the spec's stated availability
expectations; can be flipped on later via a Terraform variable.

**Alternatives considered**:

- **Aurora Serverless v2 MySQL.** Scales to zero-ish, pricier baseline, more
  features. Overkill for v1.
- **Public RDS endpoint.** Rejected — violates FR-020 ("private network path
  only").
- **DynamoDB.** Rejected — domain model uses relational schemas with foreign
  keys (Hibernate ORM/Panache); not a fit.

---

## R-006: Lambda networking — VPC vs no-VPC

**Decision**: Lambda runs **inside the VPC** in private subnets so it can
reach RDS over the private network. Outbound internet access (e.g., to AWS
APIs, Secrets Manager) is via **VPC interface endpoints** for Secrets Manager
and CloudWatch Logs to avoid NAT gateway costs.

**Rationale**: A NAT gateway is ~$32/month idle; VPC endpoints cost
~$7/month each but only for the AWS services we actually call from Lambda
(Secrets Manager, CloudWatch Logs, S3 if any). At v1 traffic this is the
cheaper option.

**Alternatives considered**:

- **NAT gateway.** Simpler to set up, more expensive baseline, only worth it
  if Lambda needs broad public-internet egress (it does not for v1).
- **Lambda outside VPC + RDS Proxy with public endpoint.** Rejected — keeps
  RDS off the public internet only via RDS Proxy, but we still need VPC for
  RDS itself. Adds a service for no net win at v1 scale.

---

## R-007: Database migrations — when and how

**Decision**: Run Flyway migrations as a **separate one-shot Lambda
invocation** (a "migrator" Lambda built from the same Quarkus app with a
different entry point) before promoting the new application Lambda alias. The
CD workflow blocks on the migrator Lambda completing successfully; on
failure, the application alias is **not** updated and the previous version
keeps serving (FR-021/-022).

**Rationale**: Keeps migrations out of the cold-start path of every regular
invocation (Quarkus's Flyway-on-startup mode would re-check schema on every
container init). Also means a failed migration cannot half-apply and then
serve traffic.

**Alternatives considered**:

- **Flyway-on-startup of the application Lambda.** Simple; risks racing
  multiple cold-starts attempting migration at once. Rejected.
- **Run Flyway from the GitHub Actions runner over a public RDS endpoint.**
  Rejected — would require a public RDS endpoint.
- **Run Flyway from the GitHub Actions runner via SSM tunnel / bastion.**
  Works, but adds bastion infrastructure for one job. Reject for v1.

---

## R-008: GitHub → AWS authentication

**Decision**: GitHub OIDC federation. A single AWS IAM role
(`gha-deploy-production`) with a trust policy scoped to the repository and
the `main` branch (and `pull_request` for plan-only) is assumed by jobs via
`aws-actions/configure-aws-credentials@v4`.

**Rationale**: No long-lived access keys (FR-024). Trust scoping by repo and
ref is enforced in the IAM trust policy itself, not just at the workflow
level.

**Alternatives considered**:

- **Long-lived IAM user access keys in GitHub Secrets.** Rejected — directly
  violates FR-024.
- **AWS SSO / IAM Identity Center.** Designed for human access, not
  machine-to-machine.

---

## R-009: Frontend protection — preventing direct S3 access

**Decision**: S3 bucket is **private** (no public ACL, public access fully
blocked). Access is restricted to the CloudFront distribution via **Origin
Access Control (OAC)**:

1. CloudFront signs origin requests with SigV4 using a service principal of
   `cloudfront.amazonaws.com`.
2. The S3 bucket policy allows `s3:GetObject` only when the principal is
   `cloudfront.amazonaws.com` AND the `aws:SourceArn` condition matches the
   distribution ARN.
3. The bucket uses the S3 REST endpoint as the CloudFront origin (not the
   website endpoint). SPA fallback (404 → `/index.html`) is handled by
   CloudFront custom error responses, not S3 website routing.

**Rationale**: AWS-native, no shared secrets, no second-vendor injection rule.
Satisfies FR-009 with a mechanism that is straightforward in Terraform and
has no runtime cost.

**Alternatives considered**:

- **Legacy Origin Access Identity (OAI).** Older mechanism, superseded by
  OAC, which supports SSE-KMS and all S3 regions including newer ones.
- **Shared-secret header injected by CloudFront origin custom headers.**
  Works, but introduces a secret that has to live in Secrets Manager and
  rotate. OAC removes that burden.
- **Public S3 website.** Rejected — violates FR-009.

---

## R-010: CloudFront cache invalidation on deploy

**Decision**: After uploading the new frontend bundle to S3, the CD workflow
runs `aws cloudfront create-invalidation --distribution-id "$DIST_ID"
--paths "/" "/index.html"`. Vite emits hashed asset filenames, so the only
non-hashed asset that needs invalidation is `index.html`; the root `/` is
included so the default-root-object lookup also picks up the new file.

**Rationale**: Surgical purge avoids invalidating long-lived hashed assets
and stays within the always-free 1 000 invalidation paths/month budget
(every deploy uses 2 paths). Matches SC-007 (< 5 min user-visible refresh).
The deploy IAM role's `cloudfront:CreateInvalidation` permission is enough —
no API token, no second-vendor secret.

**Alternatives considered**:

- **Wildcard invalidation `/*`.** Same effect, but counts as 1 path; works,
  but invalidates hashed assets unnecessarily on every deploy.
- **Cache TTL = 0 on `index.html`.** Works but tightens TTL globally;
  per-deploy invalidation is more controlled.

---

## R-011: Terraform state and locking

**Decision**: S3 backend with DynamoDB state locking. State bucket has
versioning enabled and SSE-S3 encryption. Both resources live in a
**bootstrap** Terraform configuration applied once manually before any
environment can be provisioned.

The production environment is split across two state files in the same
backend bucket so their lifecycles can be managed independently:

- `production/persistent.tfstate` — CloudFront distribution, OAC, frontend
  S3 bucket. Provisioned once and left up between test sessions because
  CloudFront takes 10–20 min to disable-and-delete and is free at idle.
- `production/terraform.tfstate` — VPC, NAT, RDS, Lambda, API Gateway,
  Secrets Manager, security groups. Applied at the start of each test
  session and destroyed at the end to stop NAT/RDS hourly charges. Reads
  the persistent state via `terraform_remote_state` to source the
  CloudFront domain for API Gateway CORS.

**Rationale**: Standard pattern; satisfies FR-018.

**Alternatives considered**:

- **Terraform Cloud / HCP Terraform.** Another vendor, monthly seat cost.
  Reject for v1.
- **Local state.** Reject — explicitly violates FR-018.

---

## R-012: Pipeline structure (workflow files)

**Decision**: Six workflows.

- `ci.yml` — `pull_request` to `main`. Jobs: `backend-test`, `frontend-test`,
  `terraform-plan` (matrix across both stacks; persistent or ephemeral plans
  run only when their respective paths change), and a required `gates` job.
  Fork PRs run tests only; no AWS role assumption.
- `cd.yml` — `push` to `main`. Applies the **ephemeral** stack
  (`infra/envs/production/`), runs migrations, deploys backend, deploys
  frontend (`aws s3 sync` + `aws cloudfront create-invalidation`), and runs
  smoke tests. Reads `cloudfront_distribution_id` and `frontend_bucket_name`
  from the persistent stack via `terraform output`. `concurrency: production`
  serializes runs.
- `cd-persistent.yml` — `push` to `main` filtered to
  `infra/envs/production-persistent/**` and `infra/modules/{cloudfront,frontend_bucket}/**`,
  plus `workflow_dispatch`. Applies the persistent stack only.
- `destroy.yml` — `workflow_dispatch`. Destroys the **ephemeral** stack only.
  The persistent stack is intentionally untouched so subsequent re-applies
  skip the CloudFront 10–20 min wait.
- `destroy-persistent.yml` — `workflow_dispatch` only, no schedule. Full wipe
  of the persistent stack. Used only when the project is being torn down.
- `drift-check.yml` — scheduled. Matrix across both stacks; reports drift in
  either.
- `rollback.yml` — `workflow_dispatch` with a `commit_sha` input. Re-runs the
  ephemeral deploy steps using the artifact built from that SHA, including
  the CloudFront invalidation.

**Rationale**: Matches FR-001 / FR-006 / FR-013 / FR-014 directly. Splitting
CI from CD makes status checks composable and lets fork PRs run safely.

**Alternatives considered**:

- **One workflow with conditional jobs.** Harder to reason about required
  status checks; rejected.
- **Reusable workflow callouts.** Premature abstraction at three workflows;
  introduce only if a fourth appears.

---

## R-013: Secrets at runtime

**Decision**: The Lambda execution role has `secretsmanager:GetSecretValue`
scoped to `arn:aws:secretsmanager:<region>:<acct>:secret:stocktracker/*`. The
Quarkus app reads `DATASOURCE_PASSWORD` from a Secrets Manager extension
sidecar (the Lambda Secrets Manager Extension layer) which caches the secret
in-process and refreshes it. Database **username** and **host** are passed
as plain Lambda environment variables.

**Rationale**: Centralizes the sensitive value, avoids re-fetching on every
request, and keeps the username/host (non-sensitive) in plain config.

**Alternatives considered**:

- **Pass password directly via Lambda env var.** Rejected — env vars are
  visible in the Lambda console to anyone with read access.
- **AWS Parameter Store SecureString.** Comparable security; Secrets Manager
  has the rotation hooks if we ever want them. Pick once, stick with it.

---

## R-014: Out-of-scope (explicit, to prevent scope creep)

The following are **explicitly out of scope** for this feature and will not
be planned, tasked, or implemented:

- Multi-region or multi-account deployment
- Staging or preview environments (only `production` ships in v1)
- Blue/green or canary deployment strategies (the alias-pointer swap on a
  failed deploy is the only "safety" mechanism)
- Auto-scaling RDS, read replicas, or Aurora migration
- WAF rules on API Gateway or CloudFront (Shield Standard is on by default)
- Custom domain, Route 53 zone, ACM certificate (default cloudfront.net and
  execute-api hostnames are used in v1)
- End-user authentication (deferred to a later spec)
- SnapStart / GraalVM native image (revisit after v1 metrics)
- Disaster recovery automation beyond RDS automated backups
