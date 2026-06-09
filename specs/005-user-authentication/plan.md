# Implementation Plan: User Authentication & Account Management

**Branch**: `005-user-authentication` | **Date**: 2026-06-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-user-authentication/spec.md`

## Summary

Add email+password authentication, self-service sign-up with email verification,
password reset, and Google/Facebook social login to StockTracker, with all
portfolio/watchlist/transaction data scoped per user. The design keeps the
**resource-protection layer mode-agnostic**: every protected endpoint validates a
signed JWT bearer token and resolves the current user from it. Only *token
issuance and account management* differ by mode:

- **Dev mode** (`stocktracker.auth.mode=dev`, the docker-compose / local default):
  the backend owns the flows — a new `AuthResource` handles sign-up, email
  verification, sign-in (issues a locally-signed RS256 JWT), and reset against an
  `app_user` table with BCrypt-hashed passwords. A **dev-only token endpoint**
  exposes the latest verification/reset token so the e2e suite drives the real
  flows without a live inbox. No external services required.
- **Production mode** (`stocktracker.auth.mode=cognito`): an **Amazon Cognito User
  Pool** (provisioned via Terraform, feature 003 infra) owns sign-up, verification,
  reset, and **Google + Facebook federation**; the frontend uses Cognito's hosted
  flows; the backend validates Cognito-issued JWTs against the pool's JWKS. Social
  login is therefore a Cognito-federation capability (not hand-rolled OAuth) and is
  covered by mocked-provider integration tests, not browser e2e.

A new Selenium **authentication regression journey** (feature 004 `e2e/` module)
exercises the dev-mode flows headlessly in CI.

## Technical Context

**Language/Version**: Java 21 (Quarkus 3.15.2 backend); TypeScript 5.6 / React 18.3
(Vite 5.4 frontend); Java 21 (Selenium e2e); Terraform ≥1.7 (infra)
**Primary Dependencies**: Backend adds `quarkus-smallrye-jwt` +
`quarkus-smallrye-jwt-build` (JWT validate/issue), `quarkus-security`,
`quarkus-elytron-security-common` (`BcryptUtil`), `quarkus-mailer` (verification/
reset email in prod-like setups; logging sink in dev), `quarkus-scheduler` (token
cleanup). Frontend reuses React Router 6, Zustand, React Hook Form. Infra adds an
`aws_cognito_user_pool` module. e2e reuses Selenium 4.27 + JUnit 5.
**Storage**: MySQL 8.4 via Hibernate ORM Panache + Flyway. New migration `V2`
adds `app_user`, `auth_credential`, `social_identity`, `verification_token`
tables and `user_id` columns on `watchlist` and `portfolio_transaction`. Cognito
holds prod user records (mirrored locally via JIT provisioning on first token).
**Testing**: Backend JUnit 5 + `@QuarkusTest` (RestAssured) unit/integration,
incl. mocked social-provider integration tests (FR-T04); frontend Vitest +
Testing Library + MSW; e2e Selenium journey (dev mode).
**Target Platform**: Local docker-compose + GitHub Actions `ubuntu-latest` (e2e);
AWS Lambda + RDS MySQL + Cognito + S3/CloudFront (prod).
**Project Type**: Web application (existing `backend/` + `frontend/`) plus the
`e2e/` regression module and `infra/` Terraform.
**Performance Goals**: Sign-in returns in <10s incl. data load (SC-002); social
onboarding <1min (SC-008); e2e auth journey fits within the suite's <10min budget.
**Constraints**: Resource layer must validate JWTs identically in both modes;
dev-only token endpoint MUST be disabled when `auth.mode=cognito` (FR-T02);
passwords never stored reversibly (FR-019); responses non-enumerating (FR-002,
FR-014, FR-016); existing data preserved via migration (FR-007).
**Scale/Scope**: Small user base (hundreds–low thousands); email+password +
Google + Facebook only; single ordinary-user role; no MFA in v1.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Test Verification (NON-NEGOTIABLE)**: PASS — backend `@QuarkusTest`
  coverage for sign-up/verify/sign-in/reset/isolation and mocked social linking;
  frontend Vitest for auth UI/guards; new Selenium auth journey. Full suites gate
  the PR.
- **II. Lint & Style Compliance (NON-NEGOTIABLE)**: PASS — backend & e2e Spotless,
  frontend ESLint/Prettier run clean on changed files.
- **III. Compilation Integrity (NON-NEGOTIABLE)**: PASS — backend `mvn -B
  test-compile`, frontend `tsc --noEmit` + `vite build`, e2e `mvn -B test-compile`
  all zero-error.
- **IV. Simplicity & YAGNI**: PASS (with justification) — supporting two auth
  modes is *required* by FR-022, not speculative. The uniform-JWT design is the
  simplest way to avoid duplicating authorization logic per mode. Social login is
  delegated to Cognito federation rather than hand-rolled, and rate limiting is a
  minimal per-identity sliding window (prod leans on Cognito). No MFA, roles, or
  refresh-token rotation in v1.
- **V. Specification-Driven Development**: PASS — spec → clarify → plan → tasks →
  implement; four clarifications recorded in spec.

**Result**: No violations. Complexity Tracking section intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/005-user-authentication/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (auth entities + migration)
├── quickstart.md        # Phase 1 output (run dev auth + e2e locally)
├── contracts/           # Phase 1 output
│   ├── auth-api.md          # Dev-mode REST contract (/api/auth/*, dev token endpoint)
│   ├── cognito.md           # Prod Cognito pool + Google/Facebook federation + JWT claims
│   ├── frontend-routes.md   # Login/signup/forgot/reset routes + ProtectedRoute + token handling
│   └── e2e-journey.md       # Authentication regression journey contract
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
backend/src/main/java/com/stocktracker/
├── domain/
│   ├── AppUser.java               # NEW account (email, state, verified, timestamps)
│   ├── AuthCredential.java        # NEW BCrypt password hash (1:1 optional with AppUser)
│   ├── SocialIdentity.java        # NEW linked provider identity (provider, subject, email)
│   └── VerificationToken.java     # NEW single-use token (purpose, expiry, used)
├── persistence/
│   ├── AppUserRepository.java
│   ├── SocialIdentityRepository.java
│   └── VerificationTokenRepository.java
├── api/
│   ├── AuthResource.java          # NEW dev-mode /api/auth/* (signup, verify, login, reset)
│   └── DevAuthTokenResource.java  # NEW dev-only token retrieval (FR-T02; off in cognito mode)
├── service/
│   ├── AuthService.java           # NEW sign-up/verify/login/reset, BCrypt, JWT issue (dev)
│   ├── AccountLinkingService.java # NEW verified-email auto-link (FR-S03/S04)
│   └── TokenIssuer.java           # NEW dev RS256 JWT issuance (smallrye-jwt-build)
├── security/
│   ├── CurrentUser.java           # NEW @RequestScoped: resolves AppUser from JWT (JIT provision)
│   └── AuthMode.java              # NEW config enum (dev|cognito) gating dev endpoints
├── dto/                           # NEW request/response records (SignUp, Login, Reset, etc.)
└── domain/{Watchlist,PortfolioTransaction}.java   # MODIFIED: add owning AppUser

backend/src/main/resources/
├── db/migration/V2__auth_and_user_scoping.sql     # NEW tables + user_id FKs + seed user backfill
└── application.properties                         # MODIFIED: auth.mode, mp.jwt.* per profile

frontend/src/
├── api/authApi.ts                 # NEW dev-mode auth calls
├── api/client.ts                  # MODIFIED: attach Authorization: Bearer; 401 → sign-out
├── stores/authStore.ts            # NEW Zustand: token, user, login/logout
├── auth/AuthProvider.tsx          # NEW abstraction: dev (backend) vs cognito (hosted) strategy
├── auth/ProtectedRoute.tsx        # NEW guard: redirect unauthenticated to /login
├── routes/LoginRoute.tsx          # NEW
├── routes/SignupRoute.tsx         # NEW
├── routes/ForgotPasswordRoute.tsx # NEW
├── routes/ResetPasswordRoute.tsx  # NEW
├── routes/VerifyEmailRoute.tsx    # NEW
└── App.tsx                        # MODIFIED: public auth routes + wrap app in ProtectedRoute

e2e/src/test/java/com/stocktracker/e2e/
├── pages/{LoginPage,SignupPage,ForgotPasswordPage}.java   # NEW page objects
├── support/DevTokenClient.java                            # NEW HTTP client for dev token endpoint
└── journeys/AuthJourneyTest.java                          # NEW full happy + negative journey

infra/modules/cognito/                # NEW Terraform module: user pool, app client,
                                       # Google + Facebook IdPs, hosted UI domain
infra/envs/production*/main.tf         # MODIFIED: instantiate cognito module, pass JWKS/issuer
                                       # + COGNITO_* env vars to lambda_backend
```

**Structure Decision**: Web application. Auth lands in the existing `backend/`
(new `security/` + `api/AuthResource` + auth `service/` classes, reusing the
established Panache/JAX-RS/DTO conventions) and `frontend/` (new auth routes +
`authStore` + `ProtectedRoute`, reusing React Router/Zustand/RHF). The e2e
journey extends the feature-004 `e2e/` Page-Object suite. Cognito and federation
live in a new `infra/modules/cognito` consumed by the existing production
environments. No new top-level module is introduced.

## Complexity Tracking

> No constitution violations — section intentionally empty.
