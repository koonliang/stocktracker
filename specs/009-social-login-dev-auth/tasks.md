# Tasks: Social Login Dev Auth Profile

**Input**: Design documents from `/specs/009-social-login-dev-auth/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Automated test tasks are included for backend, frontend, and e2e coverage because the plan and quickstart require repeatable verification for auth, demo-user, and telemetry behavior.

**Organization**: Tasks are grouped by user story so each story can be implemented, tested, and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Every task includes exact file paths

## Path Conventions

- Backend: `backend/src/main/java/com/stocktracker/`, `backend/src/main/resources/`, `backend/src/test/java/com/stocktracker/`
- Frontend: `frontend/src/`, `frontend/tests/`
- E2E: `e2e/src/test/java/com/stocktracker/e2e/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared dependencies and documentation scaffolding needed by the feature.

- [X] T001 Add Vercel Analytics and Speed Insights packages to `frontend/package.json`
- [X] T002 [P] Document non-production auth and telemetry environment variables in `frontend/.env.example`
- [X] T003 [P] Add backend non-production auth defaults and config placeholders in `backend/src/main/resources/application.properties`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before any user story implementation starts.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create the non-production auth and demo-user schema migration in `backend/src/main/resources/db/migration/V6__nonprod_social_and_demo_auth.sql`
- [X] T005 [P] Extend `backend/src/main/java/com/stocktracker/domain/AppUser.java` for demo-account metadata and non-production account state rules
- [X] T006 [P] Extend `backend/src/main/java/com/stocktracker/persistence/AppUserRepository.java` and `backend/src/main/java/com/stocktracker/persistence/SocialIdentityRepository.java` with demo-slot and provider lookup helpers
- [X] T007 [P] Add non-production auth configuration support in `backend/src/main/java/com/stocktracker/config/NonProdAuthConfig.java` and `backend/src/main/java/com/stocktracker/security/AuthMode.java`
- [X] T008 [P] Add shared request/response DTOs for social exchange and demo-user flows in `backend/src/main/java/com/stocktracker/dto/NonProdAuthDtos.java`
- [X] T009 [P] Add provider exchange/profile clients in `backend/src/main/java/com/stocktracker/client/GoogleAuthClient.java` and `backend/src/main/java/com/stocktracker/client/FacebookAuthClient.java`
- [X] T010 [P] Add frontend non-production auth API contracts in `frontend/src/api/authApi.ts` and `frontend/src/api/types.ts`
- [X] T011 [P] Add frontend non-production auth configuration helpers in `frontend/src/auth/authConfig.ts`
- [X] T012 Update seed/bootstrap support for demo-user provisioning in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java`

**Checkpoint**: Foundation ready. User story work can begin.

---

## Phase 3: User Story 1 - Social Sign-In in Non-Production (Priority: P1) 🎯 MVP

**Goal**: Let a developer or tester sign in with Google or Facebook in non-production, create or reuse the correct local account, and skip standalone email verification.

**Independent Test**: In `dev` auth mode, complete first-time and returning social sign-in through `/auth/callback`, confirm protected pages load immediately, and confirm cancelled or failed provider flows show a clear error without creating a session.

### Tests for User Story 1

- [X] T013 [P] [US1] Add backend social exchange endpoint coverage in `backend/src/test/java/com/stocktracker/api/AuthSocialExchangeTest.java`
- [ ] T014 [P] [US1] Add backend account-linking and non-production provisioning coverage in `backend/src/test/java/com/stocktracker/service/AccountLinkingTest.java` and `backend/src/test/java/com/stocktracker/service/NonProdSocialAuthServiceTest.java`
- [X] T015 [P] [US1] Add frontend dev-mode auth hub coverage in `frontend/tests/routes/LoginRoute.dev-auth.test.tsx` and `frontend/tests/auth/AuthProvider.dev-auth.test.tsx`
- [X] T016 [P] [US1] Add frontend social callback route coverage in `frontend/tests/routes/AuthCallbackRoute.dev-auth.test.tsx`
- [ ] T017 [US1] Extend social and auto-verified signup regression coverage in `e2e/src/test/java/com/stocktracker/e2e/journeys/AuthJourneyTest.java`

### Implementation for User Story 1

- [X] T018 [P] [US1] Implement non-production social exchange service in `backend/src/main/java/com/stocktracker/service/NonProdSocialAuthService.java`
- [X] T019 [US1] Update `backend/src/main/java/com/stocktracker/service/AccountLinkingService.java` to reuse verified accounts and create immediate-access non-production users
- [X] T020 [US1] Update `backend/src/main/java/com/stocktracker/service/AuthService.java` to auto-activate and auto-verify standard dev-mode sign-up
- [X] T021 [US1] Extend `backend/src/main/java/com/stocktracker/api/AuthResource.java` with `/api/auth/social/{provider}/exchange`
- [X] T022 [US1] Add dev-mode provider login and callback completion to `frontend/src/auth/AuthProvider.tsx`
- [X] T023 [P] [US1] Add non-production social exchange helpers to `frontend/src/routes/AuthCallbackRoute.tsx` and `frontend/src/api/authApi.ts`
- [X] T024 [US1] Rework the non-production auth hub UI in `frontend/src/routes/LoginRoute.tsx` and `frontend/src/routes/SignupRoute.tsx`
- [X] T025 [P] [US1] Add social auth UI components and test hooks in `frontend/src/components/auth/SocialLoginButtons.tsx` and `frontend/src/components/auth/NonProdAuthBanner.tsx`
- [ ] T026 [US1] Extend `e2e/src/test/java/com/stocktracker/e2e/pages/LoginPage.java` for social entry, callback failure, and non-production banner assertions

**Checkpoint**: User Story 1 is independently functional as the MVP.

---

## Phase 4: User Story 2 - Demo User Quick Access (Priority: P1)

**Goal**: Let a tester create up to three demo users, sign in automatically after creation, and re-enter through existing demo accounts without passwords.

**Independent Test**: In `dev` auth mode, create three demo users from the login page, confirm each one signs in immediately with seeded data, then verify that a fourth creation attempt is blocked with a clear message while existing demo users remain selectable.

### Tests for User Story 2

- [X] T027 [P] [US2] Add backend demo-user API coverage in `backend/src/test/java/com/stocktracker/api/DemoUserAuthResourceTest.java`
- [ ] T028 [P] [US2] Add backend demo-user service coverage in `backend/src/test/java/com/stocktracker/service/DemoUserServiceTest.java`
- [ ] T029 [P] [US2] Add frontend demo-user auth hub coverage in `frontend/tests/routes/LoginRoute.demo-users.test.tsx`
- [ ] T030 [US2] Extend demo-user quick-access journey coverage in `e2e/src/test/java/com/stocktracker/e2e/journeys/AuthJourneyTest.java`

### Implementation for User Story 2

- [X] T031 [P] [US2] Implement demo-user catalog and slot-allocation logic in `backend/src/main/java/com/stocktracker/service/DemoUserService.java`
- [X] T032 [US2] Extend `backend/src/main/java/com/stocktracker/api/AuthResource.java` with `/api/auth/demo-users`, `/api/auth/demo-users/{slot}/login`, and limit handling
- [X] T033 [US2] Add demo-user creation, listing, and login requests to `frontend/src/api/authApi.ts`
- [X] T034 [US2] Extend `frontend/src/auth/AuthProvider.tsx` with demo-user session flows
- [X] T035 [P] [US2] Add demo-user list and create controls in `frontend/src/components/auth/DemoUserPanel.tsx`
- [X] T036 [US2] Integrate demo-user quick access, limit messaging, and seeded-data labels into `frontend/src/routes/LoginRoute.tsx`
- [X] T037 [US2] Seed and refresh demo-user portfolio data in `backend/src/main/java/com/stocktracker/bootstrap/DevDataBootstrap.java` and `backend/src/main/resources/seed/demo-transactions.json`
- [ ] T038 [US2] Extend `e2e/src/test/java/com/stocktracker/e2e/pages/LoginPage.java` for demo-user creation and selection actions

**Checkpoint**: User Stories 1 and 2 both work independently in non-production.

---

## Phase 5: User Story 3 - Observe Non-Production Usage and Performance (Priority: P2)

**Goal**: Capture non-production page usage and performance signals through Vercel Analytics and Speed Insights without blocking auth or navigation.

**Independent Test**: In a non-production build, confirm login and authenticated routes mount Vercel Analytics and Speed Insights when enabled, and confirm the app still renders and signs in when telemetry is disabled or blocked.

### Tests for User Story 3

- [ ] T039 [P] [US3] Add frontend telemetry gating coverage in `frontend/tests/app/App.telemetry.test.tsx`
- [ ] T040 [P] [US3] Add frontend runtime config coverage for telemetry flags in `frontend/tests/auth/authConfig.telemetry.test.ts`

### Implementation for User Story 3

- [ ] T041 [US3] Mount Vercel Analytics and Speed Insights in `frontend/src/App.tsx`
- [X] T042 [US3] Add non-production telemetry guards in `frontend/src/auth/authConfig.ts` and `frontend/src/main.tsx`
- [ ] T043 [US3] Update non-production runtime documentation in `frontend/.env.example` and `specs/009-social-login-dev-auth/quickstart.md`

**Checkpoint**: All user stories are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish documentation, validation, and cross-story cleanup.

- [ ] T044 [P] Update non-production auth contracts and mockup notes in `specs/009-social-login-dev-auth/contracts/frontend-auth-experience.md` and `specs/009-social-login-dev-auth/contracts/user-story-1-mockup.md`
- [ ] T045 Align backend and frontend README guidance in `frontend/README.md` and `e2e/README.md`
- [ ] T046 Run the full quickstart validation and record final adjustments in `specs/009-social-login-dev-auth/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational completion
- **User Story 2 (Phase 4)**: Depends on Foundational completion; can start after US1 backend auth primitives exist, but should remain independently testable
- **User Story 3 (Phase 5)**: Depends on Foundational completion and can proceed in parallel with late-stage US1/US2 UI work
- **Polish (Phase 6)**: Depends on completion of all targeted user stories

### User Story Dependencies

- **US1**: No dependency on other user stories; this is the MVP
- **US2**: Reuses shared auth/session primitives from Foundational work and should preserve US1 session behavior
- **US3**: Reuses frontend runtime configuration and app root composition but does not depend on demo-user behavior

### Within Each User Story

- Write or extend automated tests before or alongside implementation
- Backend domain/config work before endpoint wiring
- Endpoint wiring before frontend integration
- Frontend auth state wiring before e2e coverage updates

### Parallel Opportunities

- T002 and T003 can run in parallel after T001
- T005 through T011 can run in parallel after T004
- In US1, T013 through T016 can run in parallel, then T018 and T023 through T025 can run in parallel once shared foundations land
- In US2, T027 through T029 can run in parallel, then T031, T033, and T035 can run in parallel
- In US3, T039 and T040 can run in parallel, followed by T041 and T042

---

## Parallel Example: User Story 1

```bash
# Launch backend and frontend automated coverage for US1 together:
Task: "Add backend social exchange endpoint coverage in backend/src/test/java/com/stocktracker/api/AuthSocialExchangeTest.java"
Task: "Add backend account-linking and non-production provisioning coverage in backend/src/test/java/com/stocktracker/service/AccountLinkingTest.java and backend/src/test/java/com/stocktracker/service/NonProdSocialAuthServiceTest.java"
Task: "Add frontend dev-mode auth hub coverage in frontend/tests/routes/LoginRoute.dev-auth.test.tsx and frontend/tests/auth/AuthProvider.dev-auth.test.tsx"
Task: "Add frontend social callback route coverage in frontend/tests/routes/AuthCallbackRoute.dev-auth.test.tsx"

# Launch independent UI/service work after the shared auth DTO/config layer is ready:
Task: "Implement non-production social exchange service in backend/src/main/java/com/stocktracker/service/NonProdSocialAuthService.java"
Task: "Add non-production social exchange helpers to frontend/src/routes/AuthCallbackRoute.tsx and frontend/src/api/authApi.ts"
Task: "Add social auth UI components and test hooks in frontend/src/components/auth/SocialLoginButtons.tsx and frontend/src/components/auth/NonProdAuthBanner.tsx"
```

---

## Parallel Example: User Story 2

```bash
# Launch backend, frontend, and e2e coverage preparation together:
Task: "Add backend demo-user API coverage in backend/src/test/java/com/stocktracker/api/DemoUserAuthResourceTest.java"
Task: "Add backend demo-user service coverage in backend/src/test/java/com/stocktracker/service/DemoUserServiceTest.java"
Task: "Add frontend demo-user auth hub coverage in frontend/tests/routes/LoginRoute.demo-users.test.tsx"

# Launch implementation work that touches separate files:
Task: "Implement demo-user catalog and slot-allocation logic in backend/src/main/java/com/stocktracker/service/DemoUserService.java"
Task: "Add demo-user creation, listing, and login requests to frontend/src/api/authApi.ts"
Task: "Add demo-user list and create controls in frontend/src/components/auth/DemoUserPanel.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. Validate first-time social sign-in, returning sign-in, and failed callback handling
5. Demo or deploy the MVP auth hub before adding demo users

### Incremental Delivery

1. Complete Setup + Foundational to establish shared auth/config primitives
2. Deliver US1 social sign-in and auto-verified dev signup
3. Deliver US2 demo-user quick access and seeded demo data
4. Deliver US3 telemetry integration and graceful degradation
5. Finish with documentation and quickstart validation

### Parallel Team Strategy

1. One developer handles backend auth foundations and API endpoints
2. One developer handles frontend auth hub, callback, and telemetry integration
3. One developer handles automated coverage and e2e journey expansion

---

## Notes

- All tasks follow the required checklist format with sequential IDs, optional `[P]` markers, and `[US#]` labels only in user-story phases
- User Story 1 and User Story 2 are both P1, but US1 is the recommended MVP because it establishes the non-production auth-session path
- Demo-user work must preserve production isolation and must not expose passwordless shortcuts outside `dev` mode
- Telemetry work is frontend-only and must degrade safely when scripts or services are unavailable
