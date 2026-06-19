# Tasks: SIT Homelab Profile

**Input**: Design documents from `/specs/008-sit-homelab-profile/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Minimum automated test tasks are included only where required by the constitution. Homelab deployment and regression verification are otherwise manual.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend code: `backend/src/main/java/com/stocktracker/`
- Backend resources: `backend/src/main/resources/`
- Backend tests: `backend/src/test/java/com/stocktracker/`
- Deployment scripts: `scripts/`
- Feature docs: `specs/008-sit-homelab-profile/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare repo-level script/config scaffolding for the homelab deployment path

- [ ] T001 Add homelab deployment script and env sample placeholders in `scripts/deploy-homelab-sit.sh` and `scripts/.env.example`
- [ ] T002 [P] Update git ignore rules for local homelab env files in `.gitignore`
- [ ] T003 [P] Add a homelab deployment usage section stub to `backend/README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core runtime and script infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Add `sit` profile defaults and environment-variable hooks in `backend/src/main/resources/application.properties`
- [ ] T005 [P] Implement backend runtime config helper(s) for SIT deployment values in `backend/src/main/java/com/stocktracker/config/`
- [ ] T006 [P] Create minimal backend unit tests for SIT profile resolution in `backend/src/test/java/com/stocktracker/config/`
- [ ] T007 Implement shared shell helpers for env loading, validation, and logging in `scripts/deploy-homelab-sit.sh`
- [ ] T008 [P] Reuse or adapt HTTP verification flow from `scripts/smoke-check.sh` inside `scripts/deploy-homelab-sit.sh`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Deploy SIT Backend to Homelab (Priority: P1) 🎯 MVP

**Goal**: Deliver a working manual homelab deployment path that packages the backend, deploys it to the app host, and verifies the public health endpoint

**Independent Test**: Run `scripts/deploy-homelab-sit.sh` from a homelab machine with valid SIT inputs and verify the backend is deployed successfully and `PUBLIC_BASE_URL/q/health` returns success.

### Tests for User Story 1 (Minimum Required) ⚠️

- [ ] T009 [P] [US1] Add minimal script validation and dry-run coverage for deployment entry paths in `scripts/deploy-homelab-sit.sh`
- [ ] T010 [P] [US1] Add minimal backend unit coverage for SIT startup-related config wiring in `backend/src/test/java/com/stocktracker/config/`

### Implementation for User Story 1

- [ ] T011 [US1] Implement backend JVM packaging flow for homelab deployment in `scripts/deploy-homelab-sit.sh`
- [ ] T012 [US1] Implement remote artifact copy and restart flow in `scripts/deploy-homelab-sit.sh`
- [ ] T013 [US1] Implement post-deploy public health verification in `scripts/deploy-homelab-sit.sh`
- [ ] T014 [US1] Document the full SIT deployment runbook in `specs/008-sit-homelab-profile/quickstart.md`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Configure Homelab Hosts Per Deployment (Priority: P1)

**Goal**: Allow operators to maintain and override homelab host inputs safely per deployment without changing application definitions

**Independent Test**: Update `scripts/.env` with different valid host values, rerun the deployment flow, and verify the effective deployment uses the overridden hosts; run again with invalid or missing values and confirm it fails before rollout.

### Tests for User Story 2 (Minimum Required) ⚠️

- [ ] T015 [P] [US2] Add minimal script coverage for env-file loading and override precedence in `scripts/deploy-homelab-sit.sh`
- [ ] T016 [P] [US2] Add minimal backend unit tests for JDBC/host composition in `backend/src/test/java/com/stocktracker/config/`

### Implementation for User Story 2

- [ ] T017 [US2] Implement `scripts/.env` loading and explicit override precedence in `scripts/deploy-homelab-sit.sh`
- [ ] T018 [US2] Implement required-input and private-network preflight validation in `scripts/deploy-homelab-sit.sh`
- [ ] T019 [US2] Create documented sample deployment inputs in `scripts/.env.example`
- [ ] T020 [US2] Update runtime config contract and operator guidance for configurable hosts in `specs/008-sit-homelab-profile/contracts/runtime-config-contract.md` and `specs/008-sit-homelab-profile/contracts/deployment-script-contract.md`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Keep Frontend Scope Stable (Priority: P2)

**Goal**: Ensure the SIT backend path does not require frontend deployment changes and continues to verify against the existing public backend URL

**Independent Test**: Complete a SIT backend deployment without modifying frontend code or Vercel configuration, then manually confirm the existing frontend can still load data from the deployed SIT backend.

### Tests for User Story 3 (Minimum Required) ⚠️

- [ ] T021 [US3] Add manual non-regression verification steps for the existing frontend/API contract in `specs/008-sit-homelab-profile/quickstart.md`
- [ ] T022 [P] [US3] Add automated coverage ensuring SIT profile changes do not alter default non-SIT runtime behavior in `backend/src/test/java/com/stocktracker/config/`

### Implementation for User Story 3

- [ ] T023 [US3] Confirm frontend-facing backend URL guidance remains externalized and unchanged in `specs/008-sit-homelab-profile/quickstart.md` and `backend/README.md`
- [ ] T024 [US3] Ensure SIT-specific config is isolated from existing AWS production behavior in `backend/src/main/resources/application.properties`
- [ ] T025 [US3] Add manual verification steps for frontend-after-backend deployment in `specs/008-sit-homelab-profile/quickstart.md`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, consistency, and verification across stories

- [ ] T026 [P] Add final script usage/help text and failure messaging cleanup in `scripts/deploy-homelab-sit.sh`
- [ ] T027 [P] Add final documentation cross-links in `backend/README.md` and `specs/008-sit-homelab-profile/plan.md`
- [ ] T028 Run full backend verification notes and record manual homelab validation steps in `specs/008-sit-homelab-profile/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - US1 is the MVP and should land first
  - US2 depends functionally on the US1 deployment path existing
  - US3 validates non-regression once SIT-specific behavior exists
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Starts after Foundational - establishes the deployment path
- **User Story 2 (P1)**: Starts after Foundational and builds on the deployment path from US1
- **User Story 3 (P2)**: Starts after Foundational and should be completed after US1 to verify frontend-scope preservation

### Within Each User Story

- Minimum automated tests MUST be created for feature behavior before or alongside implementation
- Runtime config before deployment rollout steps
- Validation before remote restart logic
- Documentation updated before final manual verification sign-off

### Parallel Opportunities

- T002 and T003 can run in parallel after T001
- T005, T006, and T008 can run in parallel after T004
- T009 and T010 can run in parallel within US1
- T015 and T016 can run in parallel within US2
- T021 and T022 can run in parallel within US3
- T026 and T027 can run in parallel during polish

---

## Parallel Example: User Story 1

```bash
# Launch User Story 1 automated coverage together:
Task: "Add minimal script validation and dry-run coverage for deployment entry paths in scripts/deploy-homelab-sit.sh"
Task: "Add minimal backend unit coverage for SIT startup-related config wiring in backend/src/test/java/com/stocktracker/config/"
```

---

## Parallel Example: User Story 2

```bash
# Launch User Story 2 validation work together:
Task: "Add minimal script coverage for env-file loading and override precedence in scripts/deploy-homelab-sit.sh"
Task: "Add minimal backend unit tests for JDBC/host composition in backend/src/test/java/com/stocktracker/config/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run the homelab deployment script and verify the public health endpoint

### Incremental Delivery

1. Complete Setup + Foundational → shared runtime and script base ready
2. Add User Story 1 → validate manual homelab deployment end-to-end
3. Add User Story 2 → validate configurable hosts and `.env` workflow
4. Add User Story 3 → validate frontend-scope non-regression manually plus minimal config safeguards
5. Finish polish tasks and final documentation pass

### Parallel Team Strategy

With multiple developers:

1. Complete Setup + Foundational together
2. Developer A implements US1 rollout flow
3. Developer B implements US2 config/env handling after foundational config lands
4. Developer C prepares US3 non-regression documentation/tests after US1 health verification path is stable

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify each story manually against acceptance scenarios using relevant test evidence
- Commit after each task or logical group
- Stop at each checkpoint to validate the story independently
