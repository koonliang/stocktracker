# Research: SIT Homelab Profile

## R-001: Deployment trigger for a private homelab

- **Decision**: Trigger SIT deployment through an operator-run shell script
  executed from a machine inside the homelab network.
- **Rationale**: The spec clarifies that GitHub cannot deploy directly into the
  private environment. A local operator-run script satisfies that constraint
  without introducing a new always-on pull agent, self-hosted GitHub runner, or
  inbound exposure into the homelab.
- **Alternatives considered**:
  - Self-hosted GitHub Actions runner in homelab: workable, but out of scope
    because the user explicitly chose a manual script trigger.
  - Pull-based deployment agent: adds lifecycle, authentication, and update
    complexity not required by the spec.

## R-002: Backend packaging strategy for homelab

- **Decision**: Package the backend as a standard Quarkus JVM application for
  the homelab deployment path and keep the existing AWS Lambda package flow
  separate.
- **Rationale**: The current `scripts/package-lambda.sh` is specific to the
  AWS Lambda profile. The homelab app server is a private Linux host, so the
  simplest path is to use the default Quarkus JVM packaging already supported by
  the backend build, then start or restart the service on the target host.
- **Alternatives considered**:
  - Reuse Lambda packaging in homelab: mismatched runtime model and unnecessary
    AWS-specific baggage.
  - Add container orchestration: no evidence of Docker or orchestration as a
    requirement for the homelab environment.

## R-003: Configuration model for the SIT environment

- **Decision**: Express homelab runtime differences through a dedicated Quarkus
  `sit` profile plus deployment-script-provided environment variables for host
  configuration, with those values optionally loaded from a local `scripts/.env`
  file.
- **Rationale**: `application.properties` already uses environment-variable
  overrides extensively. A `sit` profile keeps environment-specific defaults
  isolated while allowing the deployment script to inject `backend` and `db`
  host values at deploy time. A `.env` file in `scripts/` keeps repeated inputs
  near the deployment entry point without hard-coding secrets into committed
  application config.
- **Alternatives considered**:
  - Hard-code homelab IPs in committed config: violates the spec requirement
    that host values remain configurable.
  - Create a separate backend application branch or module: unnecessary
    duplication for one environment profile.
  - Require operators to export every variable manually on each run: workable,
    but more error-prone than sourcing a local `.env` file.

## R-004: Post-deploy verification strategy

- **Decision**: Reuse the existing health-check pattern and validate the public
  backend endpoint as the final deployment gate.
- **Rationale**: The repo already includes `scripts/smoke-check.sh`, and the
  spec requires the frontend-facing public endpoint to be reachable before the
  SIT deployment is considered complete. Reusing the same verification pattern
  keeps operations consistent across environments.
- **Alternatives considered**:
  - Validate only local process startup on the app host: insufficient because
    the spec requires verification through the public endpoint used by the
    frontend.
  - Add deeper synthetic transaction probes: useful later, but beyond the
    current scope of environment bring-up.

## R-005: Scope boundary with existing production infrastructure

- **Decision**: Keep the AWS production path, Terraform stacks, and frontend
  deployment flow unchanged while adding a parallel homelab-only deployment path.
- **Rationale**: The feature is intentionally limited to backend SIT deployment.
  The current AWS production path is already documented and operational; mixing
  homelab deployment changes into those flows would increase regression risk.
- **Alternatives considered**:
  - Fold homelab SIT into Terraform/AWS workflows: infeasible because the
    private network is not reachable from GitHub-hosted automation.
  - Rework the Vercel frontend deployment: explicitly out of scope in the spec.
