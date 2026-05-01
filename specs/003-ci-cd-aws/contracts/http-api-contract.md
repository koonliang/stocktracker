# Contract: API Gateway HTTP API in front of Backend Lambda

Pins the externally-observable shape of the API edge. The application
behind it (Quarkus REST routes) is unchanged from the existing spec.

## Endpoint shape

- **Hostname**: `api.<domain>` (custom domain backed by ACM certificate in
  the same region)
- **Protocol**: HTTPS only; HTTP requests are rejected at API Gateway
- **TLS policy**: `TLS_1_2` minimum
- **Stage**: `$default` (HTTP API auto-deploys to a single stage)

## Routes

A single catch-all route delegates everything to the Lambda:

| Method | Path | Integration | Notes |
|--------|------|-------------|-------|
| `ANY` | `/{proxy+}` | Lambda proxy (AWS_PROXY) → backend Lambda alias `production` | Quarkus handles routing inside the function |
| `ANY` | `/` | same | Required because `{proxy+}` does not match the empty path |

Health endpoint: `/q/health` (Quarkus SmallRye Health, already provided by
the existing `quarkus-smallrye-health` extension); used by the smoke job.

## CORS

Configured at the HTTP API level so the Lambda does not have to emit CORS
headers itself.

| Setting | Value |
|---------|-------|
| `AllowOrigins` | `https://app.<domain>` (production frontend host only) |
| `AllowMethods` | `GET, POST, PUT, DELETE, PATCH, OPTIONS` |
| `AllowHeaders` | `Content-Type, Authorization, X-Requested-With` |
| `ExposeHeaders` | `Content-Length, ETag` |
| `MaxAge` | `600` seconds |
| `AllowCredentials` | `false` (no cookies in v1) |

## Throttling

Per-stage default throttling (HTTP API account-level limits also apply):

| Setting | Value | Notes |
|---------|-------|-------|
| `BurstLimit` | 200 | per stage |
| `RateLimit` | 100 RPS | per stage |

These are well above the v1 expected load (< 100 RPS) and act as a
last-line abuse cap, not as application-level rate limiting.

## Authentication

None in v1. The HTTP API has no authorizer attached. When end-user auth is
added in a later spec, a JWT authorizer is the planned extension point.

## Logging

- **Access logs**: enabled, format includes `requestId`, `ip`, `routeKey`,
  `status`, `protocol`, `responseLength`, `integrationLatency`,
  `responseLatency`. Destination: CloudWatch Logs group
  `/aws/apigw/stocktracker-production-http-api`.
- **Execution logs**: `INFO` level.

## What a contract violation looks like

- Adding a route that bypasses the Lambda (`HTTP_PROXY` integration to an
  external URL) — needs an explicit spec change.
- Loosening `AllowOrigins` to `*` — explicit spec change required.
- Disabling TLS or downgrading to TLS 1.0/1.1 — must be rejected in PR
  review.
