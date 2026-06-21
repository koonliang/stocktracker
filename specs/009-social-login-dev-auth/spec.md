# Feature Specification: Social Login Dev Auth Profile

**Feature Branch**: `009-social-login-dev-auth`  
**Created**: 2026-06-20  
**Status**: Draft  
**Input**: User description: "social identity login for dev auth profile; for non-production profile, auto verify email address (skip email verification flow); allow creation of demo account and auto-login using demo user (up to 3 demo users); enable vercel analytics and speed-insights"

## Clarifications

### Session 2026-06-20

- Q: Which web analytics and performance instrumentation should User Story 3 use in the non-production environment? → A: Use Vercel's built-in Analytics component for web analytics and Vercel's built-in Speed Insights component for performance usage tracking.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Social Sign-In in Non-Production (Priority: P1)

A developer or tester opens the non-production StockTracker environment, signs in with a supported social identity, and reaches the app immediately without being blocked by a separate email verification step.

**Why this priority**: The main purpose of this feature is to make non-production access faster and closer to real sign-in behavior while removing unnecessary friction from the current verification flow.

**Independent Test**: In a non-production environment, complete sign-in with a supported social identity using a new email and confirm the user is signed in immediately and can access protected pages without completing any separate verification action.

**Acceptance Scenarios**:

1. **Given** a visitor is using a non-production profile, **When** they complete first-time sign-in with a supported social identity, **Then** an account is created, marked ready for immediate use, and the user is signed in.
2. **Given** a returning non-production user already has a linked social identity, **When** they choose that provider again, **Then** they are signed in to the same account and data set.
3. **Given** a non-production user attempts social sign-in and the provider flow is cancelled or denied, **When** they return to the app, **Then** no session is created and a clear non-technical error message is shown.

---

### User Story 2 - Demo User Quick Access (Priority: P1)

A developer or tester who does not want to use a personal social account creates or selects a demo account and is automatically signed in so they can start testing immediately.

**Why this priority**: Demo access provides the fastest path into the app for testing, demos, and environment checks, and it reduces setup overhead for repeated non-production use.

**Independent Test**: In a non-production environment, create demo users until the limit is reached, confirm each created demo user signs in automatically, and confirm the system blocks creation of a fourth demo user with a clear explanation.

**Acceptance Scenarios**:

1. **Given** fewer than three demo users exist in the non-production environment, **When** a user requests a new demo account, **Then** the system creates one, signs the user in automatically, and presents the demo account as clearly identifiable test data.
2. **Given** one or more demo users already exist, **When** a user selects an available demo account, **Then** the system signs them in without requiring a separate credential step.
3. **Given** three demo users already exist, **When** another demo account creation is requested, **Then** the request is refused with a clear message and existing demo users remain available for sign-in.

---

### User Story 3 - Observe Non-Production Usage and Performance (Priority: P2)

A maintainer uses the non-production environment and wants page usage and performance activity to be captured through Vercel's built-in Analytics and Speed Insights components so they can confirm the environment is being exercised and spot obvious speed issues during testing.

**Why this priority**: Analytics and speed visibility support environment validation and regression detection, but they are secondary to getting users signed in successfully.

**Independent Test**: Visit key non-production pages and confirm Vercel Analytics records page visits and Vercel Speed Insights records performance signals for that environment; temporarily make the telemetry service unavailable and confirm the app still loads and sign-in still works.

**Acceptance Scenarios**:

1. **Given** a user accesses the non-production environment, **When** they navigate through the sign-in and authenticated pages, **Then** Vercel Analytics records page-usage activity for that environment.
2. **Given** a user loads pages in the non-production environment, **When** Vercel Speed Insights is active, **Then** page speed and user-experience signals are collected for those visits.
3. **Given** analytics or performance collection is temporarily unavailable, **When** the user accesses the app, **Then** the app remains usable and authentication flows continue without user-facing failure.

### Edge Cases

- A social identity email matches an existing non-production account created through another path; the system resolves the user to one account rather than creating duplicates.
- A demo user is signed in on one browser while another tester selects the same demo user elsewhere; the system handles concurrent use without exposing another person's private session.
- A demo user has modified sample data from an earlier session; the next tester can still identify they are using demo data and does not mistake it for personal production-like data.
- The Vercel analytics or performance scripts are blocked by the browser or network; sign-in and page use still succeed.
- A non-production deployment accidentally uses production rules; non-production-only shortcuts such as auto-verified accounts and demo-user creation must not appear in production.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support social identity sign-in for the non-production authentication profile.
- **FR-002**: System MUST automatically treat newly created non-production accounts as verified and MUST NOT require a separate email verification step before first access.
- **FR-003**: System MUST preserve the existing production verification behavior and MUST restrict the auto-verified shortcut to non-production profiles only.
- **FR-004**: System MUST sign returning non-production users into their existing account when they use a previously linked social identity.
- **FR-005**: System MUST handle cancelled, denied, or failed social sign-in attempts without creating a session or duplicate account.
- **FR-006**: System MUST allow creation of demo accounts in non-production environments.
- **FR-007**: System MUST automatically sign the requester into a demo account immediately after that demo account is created.
- **FR-008**: System MUST allow users to choose from existing demo accounts for quick sign-in in non-production environments.
- **FR-009**: System MUST limit the total number of demo accounts in a non-production environment to three.
- **FR-010**: System MUST refuse creation requests beyond the three-demo-user limit with a clear, user-facing message.
- **FR-011**: System MUST clearly identify demo accounts as non-personal test accounts so users do not confuse them with normal accounts.
- **FR-012**: System MUST capture non-production page-usage analytics by using Vercel's built-in Analytics component.
- **FR-013**: System MUST capture non-production page-performance insights by using Vercel's built-in Speed Insights component.
- **FR-014**: System MUST ensure Vercel Analytics and Vercel Speed Insights collection do not block sign-in or normal page use when either telemetry service is unavailable.
- **FR-015**: System MUST make the non-production auth profile, demo-user shortcuts, and telemetry behavior testable through repeatable environment verification steps.

### Key Entities *(include if feature involves data)*

- **Non-Production Auth Profile**: The environment-specific sign-in mode that enables social sign-in, auto-verified account creation, and demo-user access shortcuts outside production.
- **Demo User**: A reusable non-production account reserved for testing and demonstrations. Key attributes include a display label, creation order, sign-in availability, and clear demo-only identification.
- **Linked Social Identity**: The external identity association used to recognize a returning non-production user and reconnect them to the correct account.
- **Telemetry Visit Record**: A record of page usage or performance activity captured through Vercel Analytics or Vercel Speed Insights while users exercise the non-production environment.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of first-time social sign-ins in non-production reach an authenticated session without requiring a separate email verification step.
- **SC-002**: A tester can enter the app through social sign-in or demo-user sign-in in under 60 seconds for at least 95% of attempts in non-production.
- **SC-003**: The system never allows more than three demo users to be created in a non-production environment.
- **SC-004**: 100% of blocked fourth-demo-user creation attempts return a clear explanatory message instead of a generic failure.
- **SC-005**: Vercel Analytics and Vercel Speed Insights record key non-production sign-in and authenticated page visits during environment verification.
- **SC-006**: If telemetry collection fails, 100% of core sign-in and page-navigation checks still pass during verification.
- **SC-007**: Production environments show 0 occurrences of non-production-only shortcuts such as auto-verified sign-in or demo-user creation.

## Assumptions

- The supported social identity providers for this non-production profile follow the same provider choices already defined for the broader authentication feature unless changed in a later specification.
- Demo users are environment-scoped test accounts and are not intended for long-term personal ownership or production migration.
- Existing account-linking behavior remains in place so that matching identities resolve to one user account rather than creating duplicates.
- Vercel Analytics and Vercel Speed Insights are intended for environment observability and validation, not for business reporting or billing decisions.
- The implementation will follow Vercel's current quickstart guidance for the React app by adding the built-in Analytics and Speed Insights components to the application surface used by non-production environments.
- This feature applies only to non-production profiles such as development, SIT, or preview-style environments; production behavior remains unchanged unless separately specified.
