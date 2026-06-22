# Phase 0 Research: CRUD Toast Feedback and Live Seed Accuracy

## 1. Toast Ownership and Dispatch

**Decision**: Emit action-feedback toasts from the frontend at the mutation boundary, using the existing shared toast store, rather than introducing backend-persisted toast events or per-feature local toast implementations.

**Rationale**: The repo already has a generic `toastStore` and a single mounted `NotificationToaster`. Most CRUD flows already terminate in frontend stores or route handlers that know whether an action succeeded or failed and have access to user-friendly context. Reusing that seam keeps feedback immediate, avoids extra persistence or polling, and preserves a single rendering surface for all user-action toasts.

**Alternatives considered**:
- Backend-generated toast events: rejected because the app has no persisted action-feedback queue and this would add unnecessary infrastructure for transient UI feedback.
- Route-local toast logic only: rejected because it would duplicate success/failure wording and drift across watchlists, alerts, and transactions.
- Browser-native alerts/snackbars outside the shared store: rejected because they would fragment the app experience.

## 2. Action Scope for Guaranteed Toasts

**Decision**: Guaranteed feedback applies to authenticated add, update, and delete actions across existing CRUD surfaces, plus transaction import/export. Concretely, that includes transaction create/delete and import/export, watchlist create/rename/delete, watchlist ticker add/remove, and alert create/update/delete. Non-CRUD actions such as notification mark-read, list refresh, navigation, and drag-reorder remain out of scope unless later specified.

**Rationale**: This matches the clarified spec while staying testable against the current app surface. The repo already exposes these operations through existing routes, stores, and API clients. Limiting mandatory toast coverage to user-meaningful mutations avoids noisy feedback for every interaction.

**Alternatives considered**:
- Transaction-only scope: rejected by the user's clarification.
- Toast every authenticated click/action: rejected because it creates noise and does not map cleanly to meaningful outcomes.
- Include read-only or utility actions like notification mark-read and watchlist reorder now: rejected because the spec does not require them and the UX benefit is lower.

## 3. Toast Content Strategy

**Decision**: Standardize toast content as action + subject + outcome, with success toasts using concise confirmation copy and failure toasts reusing the existing API/client error message when available, falling back to a safe generic message.

**Rationale**: `ApiError` already carries normalized `code`, `message`, and optional `details`. Combining those with a consistent subject/action label yields feedback that is understandable, testable, and aligned across flows without inventing a new backend response contract.

**Alternatives considered**:
- Hard-code unique strings in every component: rejected because copy would drift and tests would become brittle.
- Show only generic “Success”/“Failed” messages: rejected because users need enough context to know which mutation completed.
- Push all copy generation to backend responses: rejected because several flows already normalize domain errors in frontend stores.

## 4. Live Seed Value Source in Provider Modes

**Decision**: Keep demo/seeded transaction history deterministic, but when the active market-data provider is the live provider, refresh the seeded instruments’ quote-backed values through the existing quote cache and market-data services so demo portfolios reflect live or latest-available provider prices.

**Rationale**: The user complaint is about inaccurate seed data, not about transaction history. Seed transactions are useful because they are reproducible and coherent. The inaccurate part is the displayed valuation derived from stale bundled quotes. Rebinding quote-backed values to the active provider solves the mismatch without rewriting seed transaction fixtures.

**Alternatives considered**:
- Replace seed transactions with live-generated buys/sells: rejected because that would make the demo portfolio nondeterministic and harder to test.
- Leave seed valuations static even in live-provider mode: rejected because it contradicts the feature goal.
- Use live quotes only for newly added symbols, not seeded ones: rejected because the existing seed portfolio is the main demo surface.

## 5. Fallback Behavior for Live Seed Mode

**Decision**: If live-provider mode is enabled but one or more seeded symbols cannot be refreshed, the app continues loading the seeded account using existing quote-cache/stub behavior and marks values according to current stale/unavailable rules rather than failing startup or account refresh.

**Rationale**: The app already tolerates provider failures in live quote flows by serving stale or cached values. Seed mode should not be more fragile than normal runtime behavior. Users need a working demo account even if external data is temporarily incomplete.

**Alternatives considered**:
- Fail dev bootstrap when any symbol lacks a live quote: rejected because it would make demos and local dev brittle.
- Silently swap the whole app back to stub mode whenever a single symbol fails: rejected because it hides which provider mode is actually active.
- Remove unresolved holdings from the seeded portfolio: rejected because that would corrupt the demo dataset.
