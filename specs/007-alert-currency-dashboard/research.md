# Phase 0 Research: Alert Notifications and Currency Views

## 1. Notification Re-Arm Semantics

**Decision**: Alert notifications fire once per threshold crossing. After firing, an alert is disarmed until a later quote refresh shows the condition is no longer met; the next crossing creates a new notification.

**Rationale**: This matches the clarified spec and feature-006 alert model, prevents notification spam while still reporting meaningful repeated threshold events, and gives tests a precise state transition.

**Alternatives considered**:
- Fire on every refresh while satisfied: rejected as noisy and hard to trust.
- Fire once until manual reset: rejected because it hides future crossings.
- Daily fire limit: rejected as arbitrary and less aligned with trading thresholds.

## 2. Notification History Retention

**Decision**: Retain triggered-alert notification history until the user deletes the notification or deletes the associated alert. Deleting an alert removes its notification history.

**Rationale**: Personal portfolio users benefit from a durable audit trail. The clarified retention rule is easier to explain and test than time-based purges, and the expected data volume is small.

**Alternatives considered**:
- Fixed 30/90-day retention: rejected because no business requirement needs automatic expiry.
- Cap by most recent N notifications: rejected because it can silently remove history.
- Keep read notifications briefly: rejected because read state should not imply archival deletion.

## 3. Transaction Currency Backfill

**Decision**: Existing security transactions infer currency from the related instrument. Existing cash-only transactions without an instrument currency are backfilled with the user's current base currency.

**Rationale**: This preserves existing data, avoids blocking converted views, and reflects the user's chosen reporting currency for historical account-level cash movements when no instrument context exists.

**Alternatives considered**:
- Require manual review before converted views work: rejected as disruptive for legacy users.
- Exclude unresolved cash rows from totals: rejected because it makes totals incomplete.
- Default all legacy transactions to USD: rejected because user base currency is more context-aware.

## 4. FX Date Selection

**Decision**: Convert transaction-based monetary values using the transaction date's FX rate. Convert current holdings using the valuation date of the dashboard or performance view.

**Rationale**: Transaction-date FX makes historical cash flows and realized results reproducible. Valuation-date FX keeps current holdings aligned with the date of the displayed portfolio value.

**Alternatives considered**:
- Latest FX for all values: rejected because it changes historical realized results over time.
- Transaction-date FX for current holdings: rejected because holdings need current valuation.

## 5. Missing Exact-Date FX Rates

**Decision**: If an exact-date FX rate is unavailable, use the latest prior available rate and mark the converted value as stale.

**Rationale**: This keeps dashboard and performance views usable during weekends, holidays, or provider gaps while making conversion uncertainty visible.

**Alternatives considered**:
- Hide converted values: rejected because it makes dashboards brittle.
- Exclude from totals: rejected because totals become misleadingly incomplete.
- Silently use fallback rates: rejected because users must know when conversion is stale.

## 6. Frontend Notification Dialog Pattern

**Decision**: Implement a modal/dialog launched from the app shell notification control, using existing UI primitives and showing unread count, empty state, scrollable history, individual mark-read, mark-all-read, and delete actions.

**Rationale**: Existing `Dialog`, `Button`, `Badge`, and layout components keep UX consistent. A dialog is appropriate because users are reviewing a bounded list and acting without leaving their current workflow.

**Alternatives considered**:
- Dedicated notifications page only: rejected because the spec asks for a dialog.
- Toast-only notification review: rejected because toasts do not preserve history.
- Browser/push notifications: out of scope per assumptions.

## 7. Contract Delta Strategy

**Decision**: Treat this feature as a delta over feature 006 contracts, adding missing notification history/delete/read-all behavior, transaction currency migration/backfill, and conversion metadata.

**Rationale**: Feature 006 already defines provider seams, alert basics, base currency, and performance surfaces. A delta avoids duplicating provider decisions and keeps implementation tasks focused.

**Alternatives considered**:
- Rewrite all live-data contracts in this feature: rejected as duplicative.
- Defer contracts to implementation: rejected by Spec Kit plan requirements and test-first constitution.
