# Contract: Production Cognito Identity Backend

Active when `stocktracker.auth.mode=cognito`. Provisioned by the new
`infra/modules/cognito` Terraform module and consumed by `infra/envs/production*`.
Cognito owns sign-up, verification, password reset, and Google/Facebook
federation; the backend only **validates** Cognito-issued JWTs.

## Cognito User Pool (Terraform)
- `aws_cognito_user_pool`: email as username/alias; auto-verified email; password
  policy ≥8 chars + complexity (mirrors FR-010); account recovery via email.
- `aws_cognito_user_pool_client`: authorization-code flow, allowed OAuth scopes
  `openid email profile`; callback/logout URLs = CloudFront frontend origin.
- `aws_cognito_user_pool_domain`: hosted-UI domain for login/signup/social/reset.
- `aws_cognito_identity_provider` (GOOGLE): client id/secret from Secrets Manager;
  attribute mapping `email`, `email_verified`, `sub`.
- `aws_cognito_identity_provider` (FACEBOOK): app id/secret from Secrets Manager;
  attribute mapping `email`, `sub`.
- **Verified-email account linking** enabled so a federated sign-in whose
  `email_verified=true` resolves to the existing pool user (FR-S03); unverified
  emails are not linked (FR-S04).

## JWT validation contract (backend)
The Lambda backend receives these env vars and configures `quarkus-smallrye-jwt`:

| Env var | Maps to | Example |
|---------|---------|---------|
| `STOCKTRACKER_AUTH_MODE` | `stocktracker.auth.mode` | `cognito` |
| `COGNITO_ISSUER` | `mp.jwt.verify.issuer` | `https://cognito-idp.<region>.amazonaws.com/<poolId>` |
| `COGNITO_JWKS_URL` | `mp.jwt.verify.publickey.location` | `${COGNITO_ISSUER}/.well-known/jwks.json` |

Validated token claims consumed:
- `sub` — stable Cognito subject (maps to `social_identity.provider_subject` or
  the user key for JIT provisioning).
- `email`, `email_verified` — used for linking/JIT decisions (FR-S03/S04).
- `identities` (federated) — provider name (GOOGLE/FACEBOOK) for `social_identity`.

## JIT provisioning / linking (backend)
On first valid Cognito token for a subject, `CurrentUser` resolves or creates the
local `app_user` (status ACTIVE, `email_verified` from claim) and, for federated
tokens, the matching `social_identity` row — auto-linking to an existing
verified-email account per FR-S03. This keeps user-scoped FKs (`watchlist`,
`portfolio_transaction`) referencing a local `app_user.id` in both modes.

## Frontend (prod strategy)
Uses Cognito Hosted UI for sign-in / sign-up / `Continue with Google` /
`Continue with Facebook` / password reset; receives the JWT via the auth-code
redirect; thereafter calls the same `/api/*` endpoints with `Authorization:
Bearer`. The dev `/api/auth/*` endpoints are not used in this mode.

## Secrets
Google/Facebook client credentials stored in AWS Secrets Manager and referenced by
the Cognito IdP resources (consistent with the feature-003 secrets pattern). No
new long-lived runtime secret is read by the backend (it only fetches JWKS over
HTTPS).

## Out of scope (v1)
MFA, additional providers, custom Cognito Lambda triggers, refresh-token rotation
policy beyond Cognito defaults.
