# Feature Specification: CRUD Toast Feedback and Live Seed Accuracy

**Feature Branch**: `011-transaction-toasts-live-seed`  
**Created**: 2026-06-22  
**Status**: Draft  
**Input**: User description: "every transaction should have a toast message; seed data is showing inaccurate data. if live provider is enabled, seed data should use from live provider" + clarification: "toast message is not just for transaction action, but all add/update/delete actions."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Receive Immediate Feedback for Add, Update, and Delete Actions (Priority: P1)

A signed-in user adds, updates, or deletes data anywhere in the authenticated app and immediately sees a toast message confirming whether the action succeeded or failed. The user does not need to inspect tables or refresh the page to know what happened.

**Why this priority**: Add, update, and delete actions change important portfolio and watchlist data. Immediate, explicit feedback is critical to user trust and reduces uncertainty after high-impact actions.

**Independent Test**: Perform representative add, update, and delete actions from the authenticated experience and confirm a toast appears once per completed action with a clear success or failure outcome and enough context for the user to understand the result.

**Acceptance Scenarios**:

1. **Given** a user submits a valid add action such as creating a transaction, watchlist, or alert, **When** the save completes, **Then** the user sees a success toast indicating the item was created.
2. **Given** a user edits an existing item such as a transaction, watchlist, or alert, **When** the update completes, **Then** the user sees a success toast indicating the item was updated.
3. **Given** a user deletes an existing item such as a transaction, watchlist, or alert, **When** the delete completes, **Then** the user sees a success toast indicating the item was removed.
4. **Given** an add, update, or delete action fails validation or cannot be completed, **When** the failure is returned to the user, **Then** the user sees an error toast indicating the action failed and what needs attention.

---

### User Story 2 - Understand Transaction Import and Export Results (Priority: P2)

A user imports a transaction file or exports their transaction history and receives a toast summarizing the outcome so they can tell at a glance whether the operation completed successfully and whether any rows need follow-up.

**Why this priority**: Import and export operations are high-value transaction workflows that can affect many records at once. They need explicit result feedback and should follow the same toast pattern established for all CRUD actions in Story 1.

**Independent Test**: Import a file with valid rows, import a file with partial validation issues, and export current transactions. Confirm a toast appears for each completed action with an accurate summary of the result.

**Acceptance Scenarios**:

1. **Given** a user imports a valid transaction file, **When** the import completes, **Then** the user sees a success toast summarizing how many transactions were added or updated.
2. **Given** a user imports a file containing rejected rows, **When** the validation result is shown, **Then** the user sees an error or warning toast summarizing that some rows need correction.
3. **Given** a user exports their transaction history, **When** the export file is generated, **Then** the user sees a success toast confirming the export completed.

---

### User Story 3 - See Demo Portfolio Data Match the Live Provider (Priority: P2)

A developer or demo user runs the application with the live market-data provider enabled and sees seed portfolio values that are based on the live provider rather than stale hard-coded numbers. The demo experience better reflects current market reality while remaining coherent with the seeded transactions.

**Why this priority**: Inaccurate demo data undermines confidence in the live-data capability and makes it harder to validate the product visually. This is important, but it depends on the existing live-provider integration and is less critical than transaction feedback.

**Independent Test**: Start the app with live-provider mode enabled, load the seeded demo account, and confirm the dashboard, holdings, and related seeded portfolio views use quotes sourced from the live provider instead of bundled static seed values.

**Acceptance Scenarios**:

1. **Given** the application is running with the live market-data provider enabled, **When** demo seed data is loaded, **Then** seeded holdings use quote values derived from the live provider rather than bundled static quote snapshots.
2. **Given** the live provider is enabled and a seeded instrument has a current quote available, **When** the demo portfolio is displayed, **Then** the shown market value aligns with the live provider's current or latest available price.
3. **Given** the live provider is not enabled, **When** demo seed data is loaded, **Then** the application continues to use deterministic seeded data so local development and automated tests remain reproducible.
4. **Given** the live provider is enabled but a seeded symbol cannot be resolved or quoted, **When** seed data is prepared or displayed, **Then** the application falls back gracefully without preventing the demo account from loading.

### Edge Cases

- A user performs the same add, update, or delete action repeatedly in quick succession; each completed action produces one toast, without duplicate toasts for a single completion event.
- A bulk workflow partially succeeds; the toast clearly distinguishes full success from partial success that requires review.
- A user navigates away immediately after submitting an add, update, or delete action; the toast still appears if the action completes in the current session.
- The live provider returns a delayed, stale, or market-closed quote for a seeded instrument; the seed-backed demo still loads and shows the latest available value with the same freshness rules used elsewhere in the app.
- The live provider is enabled in development but external network access is temporarily unavailable; the app degrades to the existing deterministic seed behavior or last known values rather than failing startup.
- A seeded transaction references an instrument that exists in seed metadata but is unavailable from the live provider; the system preserves the transaction history and surfaces an understandable portfolio state rather than crashing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a toast message after every user-initiated add, update, or delete action completes within the authenticated application experience.
- **FR-002**: CRUD toasts MUST clearly communicate whether the action succeeded or failed.
- **FR-003**: Success toasts for add, update, and delete actions MUST identify the completed action in user language.
- **FR-004**: Failure toasts for add, update, and delete actions MUST explain that the action did not complete and provide enough context for the user to know what to correct or retry.
- **FR-005**: System MUST display a toast message after transaction import and export actions complete.
- **FR-006**: Import toasts MUST summarize the result of the import, including whether all rows succeeded or whether some rows require user attention.
- **FR-007**: Export toasts MUST confirm that the export action completed successfully or failed.
- **FR-008**: System MUST emit at most one toast per completed action event.
- **FR-009**: CRUD and transaction-operation toasts MUST be visible within the existing signed-in application experience without requiring a page reload or manual refresh.
- **FR-010**: When the live market-data provider is enabled, seeded portfolio quote values MUST be sourced from the live provider instead of bundled static quote snapshots.
- **FR-011**: When live-provider-backed seed values are used, the seeded portfolio MUST remain consistent with the existing seeded transactions, holdings, and user isolation model.
- **FR-012**: When the live market-data provider is disabled, unavailable, or intentionally stubbed, the system MUST continue using deterministic seed data suitable for tests and offline development.
- **FR-013**: If a live quote cannot be obtained for one or more seeded instruments, the system MUST degrade gracefully without blocking demo account creation or seeded portfolio display.
- **FR-014**: The rule for whether seed data uses live-provider values or deterministic seeded values MUST be environment-driven and aligned with the existing provider-selection behavior.

### Key Entities *(include if feature involves data)*

- **CRUD Action Result**: The outcome of a user-initiated add, update, or delete workflow for an authenticated app resource, including action type, outcome state, and summary details shown to the user.
- **Toast Message**: A transient in-app message tied to a completed CRUD, import, or export action result, including tone, title, and concise outcome summary.
- **Seeded Demo Portfolio**: The preloaded portfolio, holdings, and transaction set used for local development, demos, and seeded accounts.
- **Seed Quote Source Mode**: The runtime rule that determines whether seeded portfolio values are derived from deterministic seed data or from the live provider when that provider is enabled.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of completed add, update, delete, import, and export actions in tested authenticated flows produce exactly one toast message visible to the user in the current session.
- **SC-002**: In usability checks, users can tell whether an add, update, or delete action succeeded or failed within 5 seconds of the action completing, without inspecting backend logs or refreshing the page.
- **SC-003**: Import-result toasts accurately reflect whether the import fully succeeded or requires follow-up for rejected rows in 100% of tested import scenarios.
- **SC-004**: When live-provider mode is enabled, seeded portfolio prices for resolvable instruments match the live provider's latest available values closely enough that no seeded holding displays an obviously stale hard-coded quote during the same session.
- **SC-005**: When live-provider mode is disabled or unavailable, the application still loads seeded demo data successfully in 100% of automated and offline development runs.
- **SC-006**: Switching between deterministic seed mode and live-provider-backed seed mode does not change the seeded transaction history or break seeded account access.

## Assumptions

- This feature refines existing CRUD workflows and live-data behavior already introduced in specs 006 and 007 rather than redefining those broader capabilities.
- The toast scope covers user-initiated add, update, and delete actions across authenticated app features, plus transaction import and export actions.
- Existing alert-notification toasts remain separate from action-feedback toasts; this feature adds user-action feedback, not alert semantics.
- Deterministic seeded data remains the default for automated tests and offline/local workflows unless the live provider is explicitly enabled.
- Live-provider-backed seed values affect displayed seeded portfolio valuations and related quote-derived metrics, not the underlying seeded transaction history itself.
