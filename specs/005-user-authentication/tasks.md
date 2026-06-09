# Tasks: User Authentication & Account Management

**Input**: Design documents from `/specs/005-user-authentication/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: INCLUDED — required by Constitution Principle I (Test Verification,
non-negotiable) and explicitly by the spec (Story 5 regression journey, FR-T03–T05,
mocked social integration tests FR-T04, SC-004).

**Organization**: Tasks are grouped by user story (priority order) so each story is
independently implementable and testable.

## Path Conventions

Web app: backend `backend/src/main/java/com/stocktracker/`, frontend
`frontend/src/`, e2e `e2e/src/test/java/com/stocktracker/e2e/`, infra `infra/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add dependencies and base configuration for both auth modes.

- [X] T001 Add auth dependencies (`quarkus-smallrye-jwt`, `quarkus-smallrye-jwt-build`, `quarkus-security`, `quarkus-elytron-security-common`, `quarkus-mailer`, `quarkus-scheduler`) to `backend/pom.xml`
- [X] T002 [P] Add auth config to `backend/src/main/resources/application.properties`: `stocktracker.auth.mode=${STOCKTRACKER_AUTH_MODE:dev}`, per-profile `mp.jwt.verify.issuer`/`mp.jwt.verify.publickey.location`, `smallrye.jwt.sign.key.location`, token TTLs, rate-limit thresholds, dev mailer log sink
- [X] T003 [P] Generate dev RS256 keypair (`backend/src/main/resources/jwt/privateKey.pem`, `publicKey.pem`) for dev-mode token signing/verification; ensure private key excluded from prod packaging
- [X] T004 [P] Create `AuthMode` config accessor in `backend/src/main/java/com/stocktracker/security/AuthMode.java` (dev|cognito) used to gate dev-only beans/endpoints
- [X] T005 [P] Create `frontend/src/auth/` directory and add auth env handling (`VITE_AUTH_MODE`) in `frontend/src/auth/authConfig.ts`

**Checkpoint**: Project builds with new deps; `auth.mode` resolvable.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, entities, mode-agnostic JWT validation, current-user
resolution, per-user data scoping, and protected-resource enforcement. **Every
user story depends on this phase.**

- [X] T006 Create Flyway migration `backend/src/main/resources/db/migration/V2__auth_and_user_scoping.sql`: create `app_user` (+ `sessions_invalid_before` column for FR-018), `auth_credential`, `social_identity`, `verification_token`; insert deterministic seed user; add nullable `user_id` to `watchlist` and `portfolio_transaction`, backfill to seed user, then NOT NULL + FK + index
- [X] T007 [P] Create `AppUser` Panache entity in `backend/src/main/java/com/stocktracker/domain/AppUser.java` (email normalization, status enum, verified, timestamps, sessions_invalid_before)
- [X] T008 [P] Create `AuthCredential` entity in `backend/src/main/java/com/stocktracker/domain/AuthCredential.java` (1:1 optional with AppUser, BCrypt hash)
- [X] T009 [P] Create `SocialIdentity` entity in `backend/src/main/java/com/stocktracker/domain/SocialIdentity.java` (provider, subject, email, email_verified; unique provider+subject)
- [X] T010 [P] Create `VerificationToken` entity in `backend/src/main/java/com/stocktracker/domain/VerificationToken.java` (purpose enum, token_hash, expires_at, consumed_at)
- [X] T011 Modify `backend/src/main/java/com/stocktracker/domain/Watchlist.java` and `PortfolioTransaction.java` to add the owning `AppUser` relationship (depends on T007)
- [X] T012 [P] Create `AppUserRepository` in `backend/src/main/java/com/stocktracker/persistence/AppUserRepository.java` (findByNormalizedEmail, etc.)
- [X] T013 [P] Create `SocialIdentityRepository` in `backend/src/main/java/com/stocktracker/persistence/SocialIdentityRepository.java`
- [X] T014 [P] Create `VerificationTokenRepository` in `backend/src/main/java/com/stocktracker/persistence/VerificationTokenRepository.java` (latest unconsumed by user+purpose; supersede prior)
- [X] T015 Configure mode-agnostic JWT validation (required claims `sub`, `email`) and document dev vs cognito issuer/JWKS wiring in `application.properties`; verify `quarkus-smallrye-jwt` validates both
- [X] T016 Create `CurrentUser` `@RequestScoped` resolver in `backend/src/main/java/com/stocktracker/security/CurrentUser.java`: resolve `AppUser` from validated JWT, JIT-provision on first cognito token, reject tokens issued before `sessions_invalid_before` (FR-018)
- [X] T017 Add `@Authenticated` to existing data resources (`DashboardResource`, `WatchlistResource`, `TransactionsResource`) and scope all queries by `CurrentUser.id` in `WatchlistService`, `PortfolioService`, `TransactionImport/ExportService`; foreign-owned id → 404 (FR-004/006); keep `InstrumentResource` global
- [X] T018 [P] Backend test: unauthenticated requests to protected endpoints return 401; instrument reference endpoint stays public — `backend/src/test/java/com/stocktracker/api/AuthGuardTest.java`
- [X] T019 Frontend foundation: attach `Authorization: Bearer` + handle 401→logout/redirect in `frontend/src/api/client.ts`; create `frontend/src/stores/authStore.ts`, `frontend/src/auth/ProtectedRoute.tsx`, `frontend/src/auth/AuthProvider.tsx` (dev/cognito strategy seam); wrap protected routes and add public route slots in `frontend/src/App.tsx`
- [X] T020 [P] Frontend test: `ProtectedRoute` redirects anonymous users to `/login` and renders children when authenticated — `frontend/src/auth/ProtectedRoute.test.tsx`

**Checkpoint**: Existing endpoints require a valid JWT and are user-scoped; app
shell is gated. No story-specific flows yet.

---

## Phase 3: User Story 1 - Sign In to Access My Portfolio (Priority: P1) 🎯 MVP

**Goal**: A verified user signs in with email+password, receives a JWT, sees only
their own data, and can sign out.

**Independent Test**: Seed one verified account; correct credentials → dashboard
scoped to that account; wrong credentials → denied; sign out → protected pages
redirect.

- [X] T021 [P] [US1] Create login/session DTO records (`LoginRequest`, `LoginResponse`, `UserResponse`) in `backend/src/main/java/com/stocktracker/dto/`
- [X] T022 [P] [US1] Create `TokenIssuer` (dev RS256 JWT via smallrye-jwt-build) in `backend/src/main/java/com/stocktracker/service/TokenIssuer.java`
- [X] T023 [US1] Implement `AuthService.login` (BCrypt verify via `BcryptUtil`, generic failure, unverified→403, update last_login_at) in `backend/src/main/java/com/stocktracker/service/AuthService.java`
- [X] T024 [US1] Create `AuthResource` with `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` in `backend/src/main/java/com/stocktracker/api/AuthResource.java` (non-enumerating errors per contracts/auth-api.md)
- [X] T025 [US1] Add per-identity + per-IP sliding-window rate-limit filter for `/api/auth/*` in `backend/src/main/java/com/stocktracker/security/AuthRateLimitFilter.java` (FR-020/SC-006)
- [X] T026 [P] [US1] Create `frontend/src/api/authApi.ts` (login/me/logout) and implement dev strategy in `AuthProvider`
- [X] T027 [US1] Create `frontend/src/routes/LoginRoute.tsx` (email/password form, generic error, links, social-button slots) with `data-testid` hooks; wire `authStore.login` and post-login redirect; add logout control to `frontend/src/components/layout/TopBar.tsx`
- [X] T028 [P] [US1] Backend test: login success / wrong password / unknown email (generic) / unverified→403 — `backend/src/test/java/com/stocktracker/api/AuthLoginTest.java`
- [X] T029 [P] [US1] Backend test: data isolation — user A cannot read/modify user B's watchlist/transactions (foreign id → 404) — `backend/src/test/java/com/stocktracker/service/DataIsolationTest.java`
- [X] T030 [P] [US1] Frontend test: login form submits, stores session, redirects; shows generic error on 401 — `frontend/src/routes/LoginRoute.test.tsx`

**Checkpoint**: Sign-in MVP works end-to-end with per-user isolation.

---

## Phase 4: User Story 2 - Create a New Account (Priority: P1)

**Goal**: Self-service sign-up with required email verification; account inactive
until verified; empty private dataset on first sign-in.

**Independent Test**: Sign up new email → verification token issued, sign-in
refused until verified → after verify, sign-in succeeds with empty dataset.

- [X] T031 [P] [US2] Create sign-up/verify DTO records (`SignUpRequest`, `VerifyEmailRequest`, `ResendVerificationRequest`, status responses) in `backend/src/main/java/com/stocktracker/dto/`
- [X] T032 [P] [US2] Create `EmailSender` abstraction with dev log/mock sink and `quarkus-mailer` impl in `backend/src/main/java/com/stocktracker/service/EmailSender.java`
- [X] T033 [US2] Implement token issuance/validation helpers (opaque token + SHA-256 hash, expiry, single-use, supersede prior) in `AuthService` using `VerificationTokenRepository`
- [X] T034 [US2] Implement `AuthService.signup`, `verifyEmail`, `resendVerification` (email normalization, password policy, non-enumerating, unverified→ACTIVE on verify) in `backend/src/main/java/com/stocktracker/service/AuthService.java`; also make the dev seed account sign-in-capable: in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java` provision a policy-compliant `auth_credential` for `seed@stocktracker.local` (bcrypt-hash default dev password `DevPass123!` via `BcryptUtil.bcryptHash`), guarded by the existing dev `enabled` flag so it never runs in production (FR-007)
- [X] T035 [US2] Add `POST /api/auth/signup`, `/verify-email`, `/resend-verification` to `AuthResource` per contracts/auth-api.md
- [X] T036 [US2] Create dev-only `DevAuthTokenResource` (`GET /api/dev/auth/latest-token`) gated by `AuthMode==dev` (absent in cognito) in `backend/src/main/java/com/stocktracker/api/DevAuthTokenResource.java` (FR-T02)
- [X] T037 [P] [US2] Add `@Scheduled` token-cleanup job (purge expired/consumed) in `backend/src/main/java/com/stocktracker/service/TokenCleanupJob.java`
- [X] T038 [P] [US2] Create `frontend/src/routes/SignupRoute.tsx` (form, password-policy feedback, "check your email" state) and `frontend/src/routes/VerifyEmailRoute.tsx` (consumes `?token=`) with `data-testid` hooks
- [X] T039 [P] [US2] Backend test: signup creates unverified + token; duplicate email non-enumerating; unverified login blocked; verify activates; expired/used token rejected — `backend/src/test/java/com/stocktracker/api/SignUpVerifyTest.java`
- [X] T040 [P] [US2] Backend test: dev token endpoint returns latest token in dev and is absent/404 in cognito mode — `backend/src/test/java/com/stocktracker/api/DevAuthTokenTest.java`
- [X] T041 [P] [US2] Frontend test: signup flow + verify route behavior — `frontend/src/routes/SignupRoute.test.tsx`

**Checkpoint**: New users can register, verify, and sign in to an empty dataset.

---

## Phase 5: User Story 3 - Reset a Forgotten Password (Priority: P2)

**Goal**: Request reset → time-limited single-use token → set new password →
existing sessions invalidated.

**Independent Test**: Request reset for verified email → token issued → set new
password → old password rejected, new accepted; expired/used token rejected.

- [X] T042 [P] [US3] Create reset DTO records (`ForgotPasswordRequest`, `ResetPasswordRequest`, status responses) in `backend/src/main/java/com/stocktracker/dto/`
- [X] T043 [US3] Implement `AuthService.forgotPassword` (issue PASSWORD_RESET token, identical response regardless of existence) and `resetPassword` (validate token, policy, update hash, set `sessions_invalid_before=now` for FR-018) in `backend/src/main/java/com/stocktracker/service/AuthService.java`
- [X] T044 [US3] Add `POST /api/auth/forgot-password` and `/reset-password` to `AuthResource` per contracts/auth-api.md
- [X] T045 [P] [US3] Create `frontend/src/routes/ForgotPasswordRoute.tsx` (neutral confirmation) and `frontend/src/routes/ResetPasswordRoute.tsx` (consumes `?token=`, policy feedback) with `data-testid` hooks; add authApi reset calls
- [X] T046 [P] [US3] Backend test: forgot-password non-enumerating; reset updates password, invalidates prior sessions, old password rejected; expired/used token rejected — `backend/src/test/java/com/stocktracker/api/PasswordResetTest.java`
- [X] T047 [P] [US3] Frontend test: forgot + reset flows — `frontend/src/routes/ResetPasswordRoute.test.tsx`

**Checkpoint**: Forgotten-password recovery works end-to-end.

---

## Phase 6: User Story 4 - Sign In with a Social Account (Priority: P2)

**Goal**: Google/Facebook login via Cognito federation in production; verified-
email auto-linking; covered by mocked-provider integration tests (no browser e2e).

**Independent Test**: Mocked verified Google identity with new email → new account;
mocked verified identity whose email matches an existing account → linked, single
account, existing data; unverified provider email → not linked.

- [X] T048 [P] [US4] Implement `AccountLinkingService` (auto-link on provider `email_verified=true` matching existing account per FR-S03; refuse link on unverified/absent email per FR-S04; create `SocialIdentity`) in `backend/src/main/java/com/stocktracker/service/AccountLinkingService.java`
- [X] T049 [US4] Extend `CurrentUser` to resolve/link federated Cognito tokens (provider + subject from `identities` claim) via `AccountLinkingService` (JIT) in `backend/src/main/java/com/stocktracker/security/CurrentUser.java`
- [X] T050 [P] [US4] Add cognito-mode JWT validation env wiring (`COGNITO_ISSUER`, `COGNITO_JWKS_URL`) docs/config in `backend/src/main/resources/application.properties` per contracts/cognito.md
- [X] T051 [P] [US4] Create Terraform `infra/modules/cognito/` (user pool, app client, hosted-UI domain, Google + Facebook `aws_cognito_identity_provider`, verified-email linking, outputs issuer/JWKS) with `variables.tf`/`outputs.tf`
- [X] T052 [US4] Instantiate the cognito module and pass `STOCKTRACKER_AUTH_MODE=cognito` + `COGNITO_*` env vars to `lambda_backend` in `infra/envs/production/main.tf` (and `production-persistent` as applicable)
- [X] T053 [US4] Implement frontend cognito `AuthProvider` strategy (Hosted UI redirect + auth-code callback) and wire "Continue with Google/Facebook" buttons in `frontend/src/auth/AuthProvider.tsx` + `frontend/src/routes/LoginRoute.tsx`
- [X] T054 [P] [US4] Backend integration test with mocked provider claims: new federated account; auto-link on verified matching email (no duplicate); refuse link on unverified email — `backend/src/test/java/com/stocktracker/service/AccountLinkingTest.java` (FR-T04)
- [X] T055 [US4] Validate Terraform (`terraform -chdir=infra/envs/production validate`/`fmt`) and document plan output in PR notes

**Checkpoint**: Social login works via Cognito with correct linking; logic covered
by deterministic mocked tests.

---

## Phase 7: User Story 5 - Authentication Regression Journey (Priority: P2)

**Goal**: Headless Selenium auth journey (dev mode) guarding sign-up→verify→sign-in
→sign-out, reset, invalid credentials, protected-route redirect, and isolation.

**Independent Test**: Run the regression suite against a fresh stack; the auth
journey passes all scenarios in CI within the time budget.

- [X] T056 [US5] Enable two verified seed users (one with data, one empty) for the isolation scenario in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java` and seed data; ensure `STOCKTRACKER_AUTH_MODE=dev` in `docker-compose.yml` backend env
- [X] T057 [P] [US5] Create `DevTokenClient` (java.net.http) hitting `/api/dev/auth/latest-token` in `e2e/src/test/java/com/stocktracker/e2e/support/DevTokenClient.java`
- [X] T058 [P] [US5] Create page objects `LoginPage`, `SignupPage`, `ForgotPasswordPage` in `e2e/src/test/java/com/stocktracker/e2e/pages/` using the `data-testid` hooks
- [X] T059 [US5] Create `AuthJourneyTest` covering all 5 scenarios from contracts/e2e-journey.md in `e2e/src/test/java/com/stocktracker/e2e/journeys/AuthJourneyTest.java`
- [X] T060 [P] [US5] Verify the auth journey runs in `.github/workflows/regression.yml` (no structural change expected) and produces failure screenshots; adjust only if env/seed wiring requires it

**Checkpoint**: Authentication regressions are caught on every PR (FR-T05/SC-010).

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T061 [P] Add structured auth event logging (sign-in success/failure, sign-up, verify, reset) without secrets in `AuthService`/`AuthResource` (FR-021)
- [X] T062 [P] Verify rate limiting covers signup/resend/forgot/login uniformly and add a backend test — `backend/src/test/java/com/stocktracker/api/AuthRateLimitTest.java` (FR-020/SC-006)
- [X] T063 [P] Update `frontend/README.md` and `e2e/README.md` for the auth flows and dev token endpoint; confirm `quickstart.md` curl steps work end-to-end
- [X] T064 [P] Run all quality gates: `backend ./mvnw -B verify spotless:check`, `frontend npm run lint && npm run test && npx tsc --noEmit`, `e2e mvn -B -f e2e/pom.xml test spotless:check` (Constitution gates)
- [X] T065 Security check: confirm `/api/dev/auth/*` and the dev keypair are absent in cognito/prod packaging; confirm passwords never logged or returned (FR-019/FR-T02)

---

## Dependencies & Execution Order

- **Setup (Phase 1)** → blocks everything.
- **Foundational (Phase 2)** → blocks all user stories. T006 (migration) blocks
  T007–T014; T007 blocks T011; T015–T017 block T019's usefulness and all stories.
- **US1 (P1)** depends only on Foundational → **MVP**.
- **US2 (P1)** depends on Foundational; independent of US1 (shares `AuthService`/
  `AuthResource` files — coordinate edits).
- **US3 (P2)** depends on Foundational + US2's token helpers (T033).
- **US4 (P2)** depends on Foundational + `CurrentUser` (T016); extends it (T049).
- **US5 (P2)** depends on US1+US2+US3 UI/endpoints and the dev token endpoint (T036).
- **Polish (Phase 8)** depends on the stories it touches.

## Parallel Execution Examples

- **Foundational entities**: T007, T008, T009, T010 in parallel; then T011, then
  repositories T012, T013, T014 in parallel.
- **US1**: T021, T022, T026 in parallel; tests T028, T029, T030 in parallel after
  their targets exist.
- **US2**: T031, T032, T038 in parallel; tests T039, T040, T041 in parallel.
- **US4**: T048, T050, T051 in parallel; T054 after T048/T049.
- **US5**: T057, T058 in parallel before T059.
- Backend `AuthService.java` / `AuthResource.java` are edited by US1–US3 — those
  same-file tasks are **not** [P] across stories; sequence them.

## Implementation Strategy

- **MVP**: Phase 1 → Phase 2 → Phase 3 (US1). Delivers gated, isolated sign-in.
- **Increment 2**: US2 (sign-up + verification) — completes the P1 entry path.
- **Increment 3**: US3 (reset), then US4 (social via Cognito), then US5 (e2e
  regression guard).
- Each story ends at a green checkpoint with its own tests passing before moving on.
