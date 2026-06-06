# Feature Specification: User Authentication & Account Management

**Feature Branch**: `005-user-authentication`
**Created**: 2026-06-06
**Status**: Draft
**Input**: User description: "- for dev mode, user id and password will be sufficient; - for production mode in aws, suggest if it should integrate with Cognito or use DynamoDB (include a cost analysis); - include an account sign up feature and reset password"

## Clarifications

### Session 2026-06-06

- Q: Once login is added, how should existing shared portfolio/watchlist/transaction data be scoped? → A: **Per-user isolation** — each authenticated user sees only their own data; pre-existing shared data is migrated to a default seed account.
- Q: Should new accounts require email verification before first login? → A: **Yes** — accounts are inactive until the user confirms ownership of their email.
- Q: Which social identity providers should v1 support? → A: **Google and Facebook**.
- Q: When a social sign-in's email matches an existing email/password account, what happens? → A: **Auto-link** when the provider asserts a verified matching email, so both methods reach one account.
- Q: Which authentication mode should the new regression journey exercise in the headless e2e stack? → A: **Dev mode only** — local email+password against the app's own store via docker-compose; no AWS/Cognito in e2e.
- Q: How should email-dependent flows (verification, reset) get their token in e2e with no real inbox? → A: A **dev-only token-retrieval endpoint** exposes the latest verification/reset token for a test email so the real flow runs end-to-end.
- Q: Should social login (Google/Facebook) be part of the browser regression journey? → A: **Excluded from browser e2e**; Google/Facebook are covered by mocked-provider integration tests instead.
- Q: What scenarios should the new authentication regression journey cover? → A: **Full happy + negative path** — sign-up→verify→sign-in→sign-out, password reset, invalid-credentials rejection, protected-route redirect, and per-user data isolation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign In to Access My Portfolio (Priority: P1)

A returning user opens StockTracker, enters their credentials, and is taken to their own portfolio dashboard. Without valid credentials they cannot view or change any portfolio, watchlist, or transaction data.

**Why this priority**: Authentication is the gate for every other capability in this feature. Per-user data isolation is meaningless without a reliable sign-in. This is the minimum viable slice — a single seeded account that can sign in and out delivers value and is independently demonstrable.

**Independent Test**: Seed one verified account; sign in with correct credentials and confirm the dashboard loads scoped to that account; sign in with wrong credentials and confirm access is denied; sign out and confirm protected pages are no longer reachable.

**Acceptance Scenarios**:

1. **Given** a verified account exists, **When** the user submits the correct email and password, **Then** they are signed in and see their own portfolio dashboard.
2. **Given** a verified account exists, **When** the user submits an incorrect password, **Then** sign-in is rejected with a generic error that does not reveal whether the email exists.
3. **Given** a signed-in user, **When** they sign out, **Then** their session ends and protected pages redirect to the sign-in screen.
4. **Given** an unauthenticated visitor, **When** they navigate directly to a protected page, **Then** they are redirected to sign in rather than seeing any data.

---

### User Story 2 - Create a New Account (Priority: P1)

A new visitor signs up by providing an email and password. The system sends a verification email; the account becomes usable only after the user confirms their email. On first sign-in the user has an empty, private portfolio.

**Why this priority**: Without self-service sign-up the product cannot onboard real users beyond seeded accounts. Combined with Story 1 it forms the complete entry path. It is independently testable end to end.

**Independent Test**: Submit the sign-up form with a new email; confirm a verification email is sent and the account cannot sign in until verified; complete verification and confirm sign-in succeeds with an empty private dataset.

**Acceptance Scenarios**:

1. **Given** an email not already registered, **When** the user completes sign-up with a valid password, **Then** an account is created in an unverified state and a verification message is sent to that email.
2. **Given** an unverified account, **When** the user attempts to sign in, **Then** sign-in is refused and the user is prompted to verify their email (with an option to resend verification).
3. **Given** an unverified account, **When** the user completes the email verification step, **Then** the account becomes active and the user can sign in.
4. **Given** an email already registered, **When** someone attempts to sign up with it, **Then** the system does not create a duplicate and does not disclose whether the email is already in use beyond a neutral message / standard verification flow.
5. **Given** a password that does not meet the strength policy, **When** the user submits sign-up, **Then** the account is not created and the policy requirements are shown.

---

### User Story 3 - Reset a Forgotten Password (Priority: P2)

A user who cannot remember their password requests a reset, receives a time-limited reset link or code by email, sets a new password, and can then sign in with it.

**Why this priority**: Password reset is essential for retention and support-load reduction, but the product is still usable for new and remembering users without it, so it ranks below sign-in and sign-up.

**Independent Test**: Request a reset for a known verified email; confirm a time-limited reset message is sent; use it to set a new password; confirm the old password no longer works and the new one does; confirm an expired/used reset link is rejected.

**Acceptance Scenarios**:

1. **Given** a verified account, **When** the user requests a password reset for their email, **Then** a time-limited reset link/code is sent, and the response is identical whether or not the email is registered (no account enumeration).
2. **Given** a valid, unexpired reset link/code, **When** the user submits a new compliant password, **Then** the password is updated and any active sessions for that account are invalidated.
3. **Given** a reset link/code that is expired or already used, **When** the user attempts to use it, **Then** the reset is rejected and the user is prompted to request a new one.
4. **Given** a successful reset, **When** the user signs in with the new password, **Then** sign-in succeeds and the old password is rejected.

---

### User Story 4 - Sign In with a Social Account (Priority: P2)

A user chooses "Continue with Google" or "Continue with Facebook" instead of typing a password. On first use this creates (or links to) their StockTracker account; on later visits it signs them straight in. If the social provider's verified email matches an existing email/password account, the two are joined into one account.

**Why this priority**: Social login lowers onboarding friction and removes password management for many users, improving sign-up conversion. It is valuable but not required for the product to function — email/password (Stories 1–3) already deliver a complete auth path — so it ranks alongside password reset rather than above sign-in/sign-up.

**Independent Test**: From the sign-in screen, choose a social provider; complete the provider's consent flow with a brand-new email and confirm a new StockTracker account with an empty private dataset is created and signed in; repeat with a provider email matching an existing verified email/password account and confirm both methods now reach the same single account and data.

**Acceptance Scenarios**:

1. **Given** a user with no StockTracker account, **When** they complete sign-in with a supported social provider, **Then** a new account is created, signed in, and given an empty private dataset.
2. **Given** a returning user who previously used a social provider, **When** they choose that provider again, **Then** they are signed straight in to the same account without entering a password.
3. **Given** an existing email/password account with a verified email, **When** the user signs in with a social provider that asserts the same verified email, **Then** the social identity is linked to that existing account and they see their existing data (no duplicate account).
4. **Given** a user cancels or is denied at the provider's consent screen, **When** they return to StockTracker, **Then** no account or session is created and a clear, non-technical message is shown.
5. **Given** a social provider does not supply a verified email, **When** the user attempts social sign-in, **Then** the system does not silently link to an existing account and handles the case safely (per the linking policy).

---

### User Story 5 - Authentication Regression Journey (Priority: P2)

The existing automated browser regression suite (feature 004) gains a new authentication journey so that changes to sign-up, verification, sign-in, password reset, and per-user data isolation are guarded against regression on every pull request. The journey runs headlessly against the ephemeral docker-compose stack in **dev mode** (local email+password), using a dev-only endpoint to obtain verification/reset tokens. Social login is not driven through the browser here.

**Why this priority**: Authentication is now the gate for all functionality, so an undetected regression is high-severity. Automated coverage protects every later change. It depends on Stories 1–3 existing first, so it ships alongside them rather than ahead.

**Independent Test**: Run the regression suite against a fresh stack; confirm the authentication journey signs up a new user, verifies via the dev-only token, signs in, exercises a protected page, signs out, performs a password reset, rejects invalid credentials, and confirms one user cannot see another user's data — all passing in CI within the suite's time budget.

**Acceptance Scenarios**:

1. **Given** a fresh ephemeral stack in dev mode, **When** the regression journey runs, **Then** it completes sign-up → email verification (via the dev-only token endpoint) → sign-in → protected-page access → sign-out without manual steps.
2. **Given** the journey runs, **When** it submits invalid credentials and accesses a protected route while signed out, **Then** sign-in is rejected and the protected route redirects to sign-in.
3. **Given** two seeded users with distinct data, **When** the journey signs in as each, **Then** each sees only their own portfolio/watchlist/transactions.
4. **Given** the journey performs a password reset using a dev-only reset token, **When** it signs in with the new password, **Then** sign-in succeeds and the old password is rejected.
5. **Given** the suite runs in CI, **When** any authentication scenario fails, **Then** the pull request is blocked and a failure screenshot/report is produced.

---

### Edge Cases

- A user submits sign-in, sign-up, or reset forms repeatedly in a short window — the system throttles/rate-limits to resist brute-force and email-flooding without locking out legitimate use indefinitely.
- A verification or reset email is delayed or not received — the user can request a resend, and previously issued links/codes for the same purpose are superseded.
- A session expires or a token becomes invalid while the user is active — the user is returned to sign-in without exposing protected data.
- A user changes their password (via reset) while signed in elsewhere — other sessions are invalidated.
- Email casing/whitespace differences ("User@x.com" vs "user@x.com ") resolve to the same account.
- Concurrent sign-up attempts with the same email do not create duplicate accounts.
- A verification or reset link is opened more than once — only the first use takes effect.
- A social provider returns an email already tied to an account but marked unverified by the provider — the system does not auto-link (avoids account takeover via unverified email).
- A user attempts to sign in with a password to an account that was created via social login only — they are guided to use the social provider (or to set a password via reset) rather than shown a confusing failure.
- A social provider is temporarily unavailable — email/password sign-in remains usable and the failure is surfaced clearly.

## Requirements *(mandatory)*

### Functional Requirements

**Authentication & sessions**

- **FR-001**: System MUST allow a user to sign in with an email (user identifier) and password and establish an authenticated session.
- **FR-002**: System MUST reject sign-in for invalid credentials with a generic, non-enumerating error message.
- **FR-003**: System MUST allow a signed-in user to sign out, ending the session so protected resources are no longer accessible with it.
- **FR-004**: System MUST protect all portfolio, watchlist, transaction, and analysis functionality behind authentication; unauthenticated requests MUST be denied or redirected to sign-in.
- **FR-005**: System MUST expire sessions after a period of inactivity and on explicit sign-out.

**Per-user data isolation**

- **FR-006**: System MUST scope all portfolio, watchlist, and transaction data to the owning user; a user MUST NOT be able to read or modify another user's data.
- **FR-007**: System MUST migrate pre-existing shared data into a single default/seed account so no data is lost when isolation is introduced.
- **FR-008**: System MUST provision a new user with an empty, private dataset on first successful sign-in.

**Account sign-up & verification**

- **FR-009**: Users MUST be able to self-register an account using an email and password.
- **FR-010**: System MUST validate email format and enforce a documented password-strength policy at sign-up.
- **FR-011**: System MUST create new accounts in an unverified state and send a verification message to the supplied email.
- **FR-012**: System MUST refuse sign-in for unverified accounts and offer a way to resend the verification message.
- **FR-013**: System MUST activate an account only after successful email verification, and verification links/codes MUST be time-limited and single-use.
- **FR-014**: System MUST NOT create duplicate accounts for the same normalized email and MUST avoid disclosing existing-account status in a way that enables enumeration.

**Password reset**

- **FR-015**: Users MUST be able to request a password reset by supplying their email.
- **FR-016**: System MUST send a time-limited, single-use reset link/code and MUST return an identical response regardless of whether the email is registered.
- **FR-017**: System MUST allow setting a new policy-compliant password via a valid reset link/code, and MUST reject expired or already-used links/codes.
- **FR-018**: System MUST invalidate active sessions for an account when its password is reset.

**Social login**

- **FR-S01**: Users MUST be able to sign up and sign in using a supported social identity provider (Google and Facebook in v1) from the sign-in and sign-up screens.
- **FR-S02**: System MUST create a new StockTracker account with an empty private dataset on a user's first successful social sign-in when no matching account exists.
- **FR-S03**: System MUST link a social identity to an existing email/password account when the provider asserts a verified email that matches that account, so both methods resolve to one account and one dataset.
- **FR-S04**: System MUST NOT auto-link a social identity to an existing account when the provider's email is unverified or absent; such cases MUST be handled without exposing or merging existing data.
- **FR-S05**: System MUST treat an account reached via social login as fully authenticated for all protected functionality, identical to an email/password session.
- **FR-S06**: System MUST handle provider cancellation, denial, or error without creating an account or session, and surface a clear non-technical message.
- **FR-S07**: A single account MAY have multiple linked identities (password and/or one or more social providers); removing or lacking a password MUST NOT lock a user out if a working social identity remains.

**Testability & regression**

- **FR-T01**: System MUST support a dev mode in which email+password authentication runs entirely against the application's own store with no dependency on AWS/Cognito or live social providers, enabling deterministic headless e2e runs.
- **FR-T02**: Dev mode MUST expose a dev-only mechanism to retrieve the most recent verification or password-reset token for a given test email, so the real verify/reset flows can be driven end-to-end without a live inbox. This mechanism MUST NOT be available in production mode.
- **FR-T03**: The automated browser regression suite MUST include an authentication journey covering, at minimum: sign-up → email verification → sign-in → protected-page access → sign-out, password reset, invalid-credentials rejection, protected-route redirect while unauthenticated, and per-user data isolation between two users.
- **FR-T04**: Social login (Google/Facebook) is excluded from the browser regression journey and MUST instead be covered by integration tests using a mocked/stubbed provider.
- **FR-T05**: Authentication regression scenarios MUST run in CI on pull requests and block merge on failure, producing a failure report/screenshot consistent with the existing regression suite.

**Security & operational**

- **FR-019**: System MUST never store passwords in reversible form; credential storage MUST use an industry-standard one-way hashing scheme (handled by the chosen auth backend).
- **FR-020**: System MUST rate-limit sign-in, sign-up, verification-resend, and password-reset requests to mitigate brute-force and email-flooding abuse.
- **FR-021**: System MUST log authentication-relevant events (sign-in success/failure, sign-up, verification, reset) without logging secrets.
- **FR-022**: System MUST support two operating modes: a lightweight **dev mode** where email + password authentication runs locally without external identity services, and a **production mode** in AWS using a managed identity backend (see Assumptions for the recommended choice and cost analysis).

### Key Entities *(include if feature involves data)*

- **User Account**: Represents a person who can sign in. Key attributes: unique normalized email (identifier), optional credential reference (hashed, never plaintext — absent for social-only accounts), verification status, account state (active/unverified/locked), creation and last-sign-in timestamps. Owns all of that user's portfolio/watchlist/transaction data. May have one or more linked identities.
- **Linked Social Identity**: An association between a User Account and an external provider (Google or Facebook). Attributes: provider, provider-side subject identifier, the email asserted by the provider and its verified flag, linked-at timestamp. One account may hold several; the provider subject identifier is unique per provider.
- **Session / Auth Token**: Represents an authenticated session established at sign-in (by any method). Attributes: associated user, issued-at/expiry, validity state. Invalidated on sign-out and on password reset.
- **Verification Challenge**: A time-limited, single-use token/code tied to a User Account and a purpose (email verification or password reset). Attributes: purpose, expiry, used/unused state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can complete sign-up and email verification and reach their empty dashboard in under 3 minutes (excluding email delivery latency).
- **SC-002**: A returning user can sign in and see their own data in under 10 seconds; another user's data is never visible (0 cross-account data exposures in testing).
- **SC-003**: A user can complete a forgotten-password reset and sign in with the new password in under 5 minutes (excluding email delivery latency).
- **SC-004**: 100% of protected resources are inaccessible without a valid session, verified by automated regression tests.
- **SC-005**: Sign-in, sign-up, and reset responses do not allow account enumeration — identical responses for existing vs non-existing emails on the reset and sign-up neutral paths.
- **SC-006**: Repeated failed sign-in / reset attempts are throttled within a defined threshold, preventing more than the configured number of attempts per account per window.
- **SC-007**: Verification and reset links/codes reliably expire and cannot be reused (0 successful reuses of an expired/used link in testing).
- **SC-008**: A new user can complete social sign-in and reach their empty dashboard in under 1 minute, with no password to create.
- **SC-009**: When a social provider asserts a verified email matching an existing account, the user reaches their existing data with 0 duplicate accounts created (verified in testing across both providers).
- **SC-010**: The automated authentication regression journey runs headlessly on every pull request, covers all scenarios in FR-T03, and blocks merge on any failure (0 auth regressions reaching main undetected).

## Assumptions

- **Dev mode**: Local development authenticates with email + password against the application's own store (the existing relational database) — no external identity provider required, enabling offline development and the Selenium/e2e regression suite to seed and drive accounts deterministically. Dev mode also exposes a dev-only token-retrieval mechanism (FR-T02) so verification/reset flows can be driven without a live inbox; this mechanism is disabled in production.
- **Production identity backend (recommended)**: Use **Amazon Cognito User Pools** rather than a hand-rolled DynamoDB-based auth store. Rationale and cost analysis below.
- **Email delivery**: A transactional email channel (the identity provider's built-in email or Amazon SES) sends verification and reset messages; deliverability/latency is outside this feature's control.
- **Password policy**: Minimum 8 characters with a documented complexity rule (length + character variety), enforced consistently in dev and production.
- **Token model**: Sessions are represented by short-lived tokens with server-side or provider-side invalidation on sign-out/reset; long-lived "remember me"/refresh behavior is a reasonable default but not in v1 scope unless trivially provided by the backend.
- **Social login (v1)**: Google and Facebook sign-in/sign-up are in scope. Social sign-in auto-links to an existing email/password account only when the provider asserts a *verified* matching email; otherwise accounts stay separate. The chosen production identity backend (Cognito, below) federates these providers natively, so no bespoke OAuth handling is needed in application code.
- **Scope boundaries (v1)**: Email/password plus Google and Facebook only — no other social/SSO providers, no multi-factor authentication, no organization/role management beyond a single ordinary-user role. These are candidate follow-ups.
- **Data migration**: Existing shared portfolio/watchlist/transaction rows are reassigned to one seed account during rollout; this is a one-time migration.
- **Dependency**: Builds on the existing AWS deployment (Lambda backend, RDS MySQL, S3/CloudFront frontend) from feature 003.

### Production Auth Backend — Cognito vs DynamoDB Cost Analysis

The user asked whether production should integrate with **Amazon Cognito** or use **DynamoDB**. These are not equivalent options: Cognito is a managed identity service (handles password hashing, email verification, reset flows, token issuance, MFA, lockout), whereas DynamoDB is only a database — choosing it means **building and owning the entire auth system** (hashing, token signing/validation, verification/reset email flows, throttling, rotation) in application code, plus a separate email service (SES).

| Dimension | Amazon Cognito (User Pools) | DynamoDB-backed custom auth |
|-----------|-----------------------------|------------------------------|
| What you pay for | Monthly Active Users (MAU) | Read/write request units + storage; SES per-email; your own compute |
| Indicative price | Free tier covers a small user base; roughly low single-digit **cents per MAU** beyond it (verify current Cognito pricing tier at build time) | DynamoDB on-demand ~$1.25/M writes, ~$0.25/M reads, ~$0.25/GB-mo; SES ~$0.10/1,000 emails |
| Cost at small scale (≤ a few thousand users) | Effectively **$0–low** (within/near free tier) | Effectively **pennies** in infra |
| Hidden cost | Minimal — flows are built-in | **High engineering + security cost**: you must build/maintain hashing, token issuance, verification & reset flows, throttling, audit, and carry the breach risk |
| Security posture | Managed, audited, MFA-ready, automatic best practices | Entirely your responsibility; easy to get subtly wrong |
| Time to ship | Fast — flows provided | Slow — bespoke implementation + tests |

**Recommendation**: **Cognito User Pools.** At StockTracker's scale the raw infrastructure cost of either option is negligible, so the deciding factor is engineering and security burden. DynamoDB's apparent cheapness is misleading because it excludes the substantial, ongoing cost and risk of building auth correctly. Cognito issues standard tokens the existing Lambda backend can validate, provides built-in verification/reset flows, and keeps credential handling out of our code — directly satisfying FR-019 and shrinking the surface area we must secure. It also **federates Google and Facebook natively** (FR-S01–FR-S03), so social login adds essentially no new infrastructure cost or bespoke OAuth code. DynamoDB would only be preferable if a hard requirement Cognito cannot meet existed (none identified here).
