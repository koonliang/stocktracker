# Phase 0 Research: User Authentication & Account Management

All Technical Context unknowns are resolved below. Each item: Decision → Rationale
→ Alternatives considered.

## 1. Unified token validation across dev and prod

**Decision**: Protect all existing resources with a JWT bearer requirement using
`quarkus-smallrye-jwt`. The resource/service layer never branches on mode — it
reads the validated `JsonWebToken` (subject + email/`cognito:username` claim).
Issuance differs: dev mode self-signs RS256 tokens (`quarkus-smallrye-jwt-build`
with a local keypair); prod validates Cognito RS256 tokens via the pool's JWKS
URL. `mp.jwt.verify.publickey.location` and `mp.jwt.verify.issuer` are set per
profile.

**Rationale**: Keeps authorization logic single-sourced (Constitution IV), so
data-isolation enforcement and `@RolesAllowed`/`@Authenticated` annotations are
identical regardless of who issued the token. Cognito and SmallRye both use RS256
+ standard JWT claims, so one verifier handles both.

**Alternatives considered**: (a) Quarkus OIDC (`quarkus-oidc`) for prod — heavier,
introduces a token-introspection/round-trip model and a different programming
model than dev; rejected for asymmetry. (b) Opaque session cookies validated
server-side — would require a shared session store on Lambda and a second code
path; rejected. (c) Branching auth logic per mode in each resource — violates
YAGNI/simplicity.

## 2. Password hashing (dev mode)

**Decision**: BCrypt via `io.quarkus.elytron.security.common.BcryptUtil`
(`quarkus-elytron-security-common`), default cost factor (10–12).

**Rationale**: First-party Quarkus utility, no extra dependency, industry-standard
adaptive hash satisfying FR-019. Prod mode delegates hashing to Cognito entirely.

**Alternatives considered**: Argon2 (stronger but needs an extra lib and tuning;
overkill for dev-only credentials), PBKDF2 (acceptable but BCrypt is the simpler
built-in).

## 3. Verification & reset tokens; dev-only retrieval (FR-T02)

**Decision**: Opaque high-entropy tokens (256-bit, URL-safe). Store only a SHA-256
hash in `verification_token` with `purpose` (EMAIL_VERIFICATION | PASSWORD_RESET),
`expires_at`, and `consumed_at`. Issuing a new token of a purpose supersedes prior
unconsumed ones for that user. Email delivery via `quarkus-mailer`; in dev the
mailer uses the mock/log sink and the token is also retrievable through a
**dev-only** `GET /api/dev/auth/latest-token?email=&purpose=` endpoint guarded by
`@IfBuildProfile`/`auth.mode==dev` so it is absent in prod.

**Rationale**: Hash-at-rest prevents token theft from DB read (defense in depth).
Single-use + expiry satisfies FR-013/FR-017/SC-007. The dev endpoint lets the
headless e2e suite drive the *real* verify/reset flow deterministically (clarify
decision) while being impossible to expose in production.

**Alternatives considered**: Mailpit/MailHog container (tests real SMTP send but
adds a container and email-parsing brittleness — rejected per clarify), auto-
confirm in dev (leaves verify/reset paths untested in e2e — rejected).

## 4. Per-user data isolation & migration (FR-006/007/008)

**Decision**: Add `app_user` and a non-null `user_id` FK to `watchlist` and
`portfolio_transaction` (instrument reference data stays global/shared). Migration
`V2` creates the auth tables, inserts a deterministic **seed/default user**, and
backfills existing `watchlist`/`portfolio_transaction` rows with that user's id
before adding the NOT NULL constraint. Services filter every query by the
`CurrentUser`'s id; cross-user access returns 404 (not 403) to avoid existence
disclosure.

**Rationale**: Preserves existing data (FR-007) with zero loss, enforces isolation
at the query layer, and 404-on-foreign-id avoids enumeration. Reusing the existing
Panache repository pattern keeps the change idiomatic.

**Alternatives considered**: Row-level DB security (MySQL lacks native RLS;
rejected), discarding existing shared data (violates FR-007), a nullable user_id
(weakens the isolation invariant; rejected).

## 5. Social login = Cognito federation (FR-S01–S07, FR-T04)

**Decision**: Implement Google + Facebook strictly as Cognito **identity-provider
federation** configured in Terraform. The frontend's prod auth strategy uses
Cognito Hosted UI (`Continue with Google/Facebook`); Cognito returns a JWT whose
`identities`/`email_verified` claims the backend reads. **Account auto-linking**
(FR-S03) is handled by enabling Cognito's verified-email linking, and the backend
JIT-provisions/links the local `app_user` + `social_identity` row keyed on the
verified email; if `email_verified=false` the link is refused (FR-S04). Dev mode
does not surface social login in the browser (clarify); a mocked-provider
integration test validates the linking/refusal logic directly against
`AccountLinkingService`.

**Rationale**: Cognito federates both providers natively at negligible added cost
(spec cost analysis), so no OAuth handshake, token exchange, or provider SDKs in
our code (Constitution IV). Mocked integration tests give deterministic coverage
of the linking rules without driving real consent screens (FR-T04).

**Alternatives considered**: Hand-rolled OAuth2 with provider SDKs (large surface,
security risk — rejected), a stubbed OAuth provider in the e2e stack (significant
infra for low marginal value — rejected per clarify), real provider test accounts
in CI (flaky, against automation ToS — rejected).

## 6. Production identity backend: Cognito vs DynamoDB

**Decision**: Amazon Cognito User Pool (confirmed; see spec cost analysis).
Terraform module defines the pool, an app client (hosted UI + auth code flow),
Google & Facebook IdPs, a hosted-UI domain, and verified-email account linking.
Lambda receives `COGNITO_ISSUER`/`COGNITO_JWKS_URL`/`auth.mode=cognito` env vars.

**Rationale**: Managed hashing, verification, reset, MFA-readiness, and native
Google/Facebook federation at ~$0 for this scale; DynamoDB would mean building all
of that ourselves. Validated against FR-019, FR-S0x, and the simplicity principle.

**Alternatives considered**: DynamoDB-backed custom auth (documented and rejected
in spec — high engineering/security cost for negligible infra savings).

## 7. Rate limiting (FR-020)

**Decision**: Minimal per-identity + per-IP sliding-window counter applied via a
JAX-RS filter to `/api/auth/*` (configurable threshold/window). In prod, Cognito
additionally throttles its own endpoints.

**Rationale**: Satisfies FR-020/SC-006 simply on the single-instance dev/Lambda
path without adding a distributed rate-limiter dependency (YAGNI). Thresholds are
config-driven so they are testable.

**Alternatives considered**: Bucket4j/Redis token bucket (distributed, unneeded at
this scale — rejected), WAF-only (doesn't cover per-account lockout — insufficient).

## 8. Frontend auth integration

**Decision**: A small `AuthProvider` strategy abstraction selects dev (calls
`/api/auth/*`, stores the JWT in memory + `sessionStorage`) vs cognito (Hosted UI
redirect / Amplify). `client.ts` attaches `Authorization: Bearer <token>` and, on
401, clears auth state and redirects to `/login`. `ProtectedRoute` wraps the
existing app shell; `/login`, `/signup`, `/forgot-password`, `/reset-password`,
`/verify-email` are public. `authStore` (Zustand, matching existing stores) holds
session state.

**Rationale**: Reuses the established React Router + Zustand + fetch-wrapper
patterns; the strategy seam isolates the only real dev/prod difference on the
client. e2e drives only the dev strategy.

**Alternatives considered**: Storing JWT in `localStorage` (XSS persistence risk —
prefer `sessionStorage` + in-memory), httpOnly cookie sessions (needs backend
session store on Lambda + CSRF handling — rejected for v1 scope).

## 9. e2e authentication journey (FR-T03)

**Decision**: New `AuthJourneyTest` + `LoginPage`/`SignupPage`/`ForgotPasswordPage`
page objects and a `DevTokenClient` (java.net.http) that calls the dev-only token
endpoint. The journey: sign-up → fetch verification token → verify → sign-in →
hit a protected page → sign-out → forgot-password → fetch reset token → set new
password → sign-in with new password → assert old password rejected → assert
protected route redirects when signed out → two-user data-isolation check. Adds
stable `data-testid` hooks to the new auth UI. Runs against docker-compose with
`auth.mode=dev` and `STOCKTRACKER_DEV_BOOTSTRAP_ENABLED=true`.

**Rationale**: Directly realizes Story 5 / FR-T03 using the existing Page-Object +
ScreenshotOnFailure infrastructure; the dev token endpoint makes the email-bound
steps deterministic and offline.

**Alternatives considered**: Reading tokens from container logs (brittle parsing —
rejected in favor of the explicit endpoint).

## 10. docker-compose / CI changes

**Decision**: Backend service gains `STOCKTRACKER_AUTH_MODE=dev` (default already
implied) and an `MP_JWT`/local-keypair config baked into the dev image; no new
containers. The existing `regression.yml` already brings up the stack and runs
`mvn -f e2e/pom.xml test`, so the new journey runs with no workflow change beyond
possibly seeding a second user via bootstrap for the isolation check.

**Rationale**: Keeps the ephemeral-stack model and CI untouched structurally
(Constitution IV); only environment configuration and seed data expand.

**Alternatives considered**: A dedicated auth-only compose file (unnecessary
duplication — rejected).
