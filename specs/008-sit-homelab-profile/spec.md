# Feature Specification: SIT Homelab Profile

**Feature Branch**: `008-sit-homelab-profile`  
**Created**: 2026-06-19  
**Status**: Draft  
**Input**: User description: "new profile: sit => homelab environment; deploy backend services to homelab environment; frontend is deployed in Vercel (already done, integrated with gitlab main branch); for this feature, just need to configure backend with sit profile; backend app and database server ip are configurable in deployment"

## Clarifications

### Session 2026-06-19

- Q: How will deployment to the private homelab environment be triggered? → A: An operator runs a deployment script from inside the homelab.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Deploy SIT Backend to Homelab (Priority: P1)

As an operator, I can deploy the backend to the homelab SIT environment using a dedicated deployment profile so that the already-live frontend can use the SIT backend without manual server-side reconfiguration.

**Why this priority**: This is the core outcome of the feature. Without a working SIT backend profile, the frontend deployment cannot be exercised against the intended backend environment.

**Independent Test**: Run the homelab deployment script using the SIT profile and verify the backend starts successfully, is reachable at the expected public endpoint, and serves requests from the existing frontend.

**Acceptance Scenarios**:

1. **Given** the operator runs the deployment script with the SIT profile, **When** the backend is deployed, **Then** the deployment targets the homelab backend environment rather than the existing cloud environment.
2. **Given** the deployment completes, **When** the operator opens the existing frontend, **Then** the frontend can successfully communicate with the SIT backend at the configured public backend endpoint.
3. **Given** the backend deployment profile is used again for an update, **When** the operator redeploys, **Then** the deployment follows the same SIT environment settings without requiring manual host changes.

---

### User Story 2 - Configure Homelab Hosts Per Deployment (Priority: P1)

As an operator, I can set the backend server host and database server host as deployment configuration values so that the SIT environment can be moved or rebuilt without changing application behavior definitions.

**Why this priority**: The user explicitly requires the homelab server IPs to remain configurable. Hard-coding them would make the environment brittle and harder to maintain.

**Independent Test**: Update the configured backend and database host values to new valid addresses, perform a deployment, and verify the backend uses the updated hosts successfully.

**Acceptance Scenarios**:

1. **Given** the operator changes the configured backend server host, **When** the next SIT deployment runs, **Then** the deployment uses the updated backend host value.
2. **Given** the operator changes the configured database server host, **When** the next SIT deployment runs, **Then** the backend connects using the updated database host value.
3. **Given** a configured host value is missing or invalid, **When** deployment starts, **Then** the deployment fails with a clear error before the backend is promoted for use.

---

### User Story 3 - Keep Frontend Scope Stable (Priority: P2)

As an operator, I can enable the SIT backend without changing the already-deployed frontend release flow so that backend rollout to homelab stays isolated from the frontend delivery process.

**Why this priority**: The frontend deployment path is already in place and intentionally out of scope. Preserving that boundary avoids unnecessary change risk.

**Independent Test**: Complete the SIT backend deployment and verify no frontend deployment settings or release triggers are required to make the backend available.

**Acceptance Scenarios**:

1. **Given** the frontend remains deployed through its existing release flow, **When** the SIT backend profile is introduced, **Then** no new frontend deployment step is required for this feature.
2. **Given** the SIT backend is updated, **When** the frontend is unchanged, **Then** the frontend continues using the same public backend endpoint.

### Edge Cases

- The configured backend host is reachable but the backend process fails to start; deployment must report the failure and not mark the SIT environment ready.
- The configured database host is unreachable from the backend host; deployment must fail clearly rather than leaving a partially configured backend running.
- The public backend endpoint resolves correctly but serves an unhealthy backend; deployment verification must catch the unhealthy state before completion.
- The homelab backend host or database host changes after initial rollout; the operator must be able to update configuration and redeploy without redefining the SIT profile.
- The frontend remains pointed at the public SIT backend endpoint while the SIT backend is unavailable; operators need a clear signal that the issue is backend availability, not frontend deployment state.
- The deployment script is started from outside the homelab or from a host without the required private network access; the deployment must fail with a clear execution prerequisite error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a dedicated `sit` deployment profile representing the homelab environment for backend services.
- **FR-002**: The `sit` deployment profile MUST target the homelab backend environment independently from existing frontend deployment behavior.
- **FR-002a**: The system MUST support deployment to the `sit` profile through an operator-run deployment script executed from within the homelab network.
- **FR-003**: The system MUST allow the backend application server host for the `sit` profile to be configured at deployment time or through deployment configuration.
- **FR-004**: The system MUST allow the database server host for the `sit` profile to be configured at deployment time or through deployment configuration.
- **FR-005**: The system MUST use the configured backend application server host when deploying backend services for the `sit` profile.
- **FR-006**: The system MUST use the configured database server host when preparing backend runtime connectivity for the `sit` profile.
- **FR-007**: The system MUST validate that required `sit` deployment configuration values are present before starting deployment.
- **FR-008**: The system MUST stop the deployment and surface a clear error when required host configuration is missing or invalid.
- **FR-008a**: The deployment script MUST validate that it is being run from an environment with access to the private homelab targets before attempting rollout steps.
- **FR-009**: The system MUST verify that the deployed SIT backend is reachable through the public backend endpoint before marking deployment complete.
- **FR-010**: The system MUST preserve the current frontend release flow and MUST NOT require new frontend deployment work as part of this feature.
- **FR-011**: The system MUST support redeploying the `sit` backend profile after host configuration changes without redefining the profile itself.
- **FR-012**: The system MUST document the expected default homelab host values for SIT deployment so operators can provision the initial environment consistently.

### Key Entities *(include if feature involves data)*

- **SIT Deployment Profile**: The named environment definition used to deploy backend services to the homelab environment. Key attributes include profile name, target environment purpose, public backend endpoint, and required deployment configuration.
- **Deployment Host Configuration**: The operator-managed values that identify where the SIT backend application server and database server are located. Key attributes include backend host value, database host value, and configuration validity.
- **SIT Backend Deployment**: A single rollout of backend services to the homelab environment. Key attributes include deployment target, configuration used, deployment status, and verification outcome.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operators can complete a SIT backend deployment to the homelab environment in under 15 minutes using the documented profile steps.
- **SC-001a**: Operators can trigger a SIT backend deployment by running the documented homelab deployment script without requiring inbound access from GitHub into the private network.
- **SC-002**: 100% of SIT deployments fail before rollout completion when required backend or database host configuration is missing or invalid.
- **SC-003**: 100% of successful SIT deployments finish with the backend reachable through the public backend endpoint used by the existing frontend.
- **SC-004**: Operators can change the homelab backend host or database host and complete a follow-up redeployment without modifying the profile definition itself.
- **SC-005**: This feature introduces 0 required changes to the existing frontend deployment flow.

## Assumptions

- The frontend is already deployed separately and remains out of scope for this feature except for continuing to call the SIT backend's public endpoint.
- The homelab environment already has a backend application server and a MySQL database server available for the SIT profile.
- The operator has shell access to a machine inside the homelab network from which the deployment script can be executed.
- The initial default host values are the current homelab backend server address `192.168.100.101` and database server address `192.168.100.102`, but both remain operator-configurable.
- The public backend endpoint used by the frontend remains `https://example.com` for the SIT environment unless changed by a future feature.
- Authentication, frontend changes, and non-SIT environments are outside the scope of this feature.
