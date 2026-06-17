# Phase 1 Data Model: Alert Notifications and Currency Views

This feature refines the feature-006 live-data model. If feature 006's migration has already introduced `alert`, `notification`, `portfolio_transaction.currency`, `fx_rate`, and user base currency, this feature adds a follow-up migration only for missing columns, indexes, or data backfill.

## Modified: `notification`

Backs **Triggered Alert Notification**.

| Field | Type | Rules |
|-------|------|-------|
| `id` | PK | immutable |
| `user_id` | FK `app_user.id` | required; all reads/writes scoped to current user |
| `alert_id` | FK `alert.id` | required while alert exists; notifications are deleted when the alert is deleted |
| `instrument_symbol` | VARCHAR | denormalized from alert for history display |
| `condition_type` | VARCHAR | `price_above`, `price_below`, or `pct_change` |
| `threshold` | DECIMAL | copied from alert at trigger time |
| `observed_value` | DECIMAL | quote price or percent observed at trigger time |
| `observed_currency` | CHAR(3) | required for price alerts; blank/not applicable for percent-only display |
| `triggered_at` | TIMESTAMP | when crossing fired |
| `read` | BOOLEAN | false on create, true after mark-read/mark-all-read |
| `crossing_key` | VARCHAR | unique event identity for one alert crossing |
| `created_at` / `updated_at` | TIMESTAMP | audit fields |

Validation and uniqueness:
- `user_id` and `alert_id` must belong to the same owner.
- Unique constraint on `alert_id, crossing_key` prevents duplicate notifications for the same crossing.
- Default ordering is `triggered_at DESC, instrument_symbol ASC, id DESC`.
- Deleting an alert deletes its notification history.

State transitions:

```text
unread -> read        user marks one or all as read
read/unread -> deleted user deletes notification or associated alert
```

## Modified: `alert`

Backs the alert re-arm state.

| Field | Type | Rules |
|-------|------|-------|
| `armed` | BOOLEAN | true means eligible to create notification on next crossing |
| `last_condition_met` | BOOLEAN | records prior evaluation state; used to detect clear/cross transitions |
| `last_triggered_at` | TIMESTAMP NULL | latest notification trigger time |
| `last_cleared_at` | TIMESTAMP NULL | latest time condition was observed false after a trigger |

State transitions:

```text
armed=true, condition=false -> armed=true, last_condition_met=false
armed=true, condition=true  -> create notification, armed=false, last_condition_met=true
armed=false, condition=true -> no notification
armed=false, condition=false -> armed=true, last_condition_met=false, last_cleared_at=now
```

Editing an alert resets `armed=true`, `last_condition_met=false`, and clears crossing state for the new rule.

## Modified: `portfolio_transaction`

Backs **Portfolio Transaction** currency enforcement.

| Field | Type | Rules |
|-------|------|-------|
| `currency` | CHAR(3) | required for all non-split monetary rows after backfill |
| `currency_source` | VARCHAR | `instrument`, `user_base_backfill`, `import`, or `manual` |
| `currency_backfilled_at` | TIMESTAMP NULL | set when legacy rows are migrated |

Backfill rules:
- Buy, sell, dividend, and fee rows tied to an instrument infer currency from `instrument.currency` when missing.
- Deposit, withdrawal, and cash-only fee rows without an instrument infer currency from the user's current base currency.
- Split rows do not require monetary currency unless fees or monetary amounts are present.

Validation rules:
- New monetary transactions cannot be saved without a supported currency.
- Imported cash-only rows must include currency; legacy database rows are the only records eligible for base-currency backfill.
- Security transaction currency defaults to instrument currency and must match it unless a future spec explicitly allows cross-currency settlement.

## Modified: `fx_rate`

Backs **Converted Portfolio Metric**.

| Field | Type | Rules |
|-------|------|-------|
| `rate_date` | DATE | exact date for known rate |
| `rate` | DECIMAL(19,8) | conversion rate |
| `source` | VARCHAR | stub or real provider |

Lookup rules:
- `convertTransaction(amount, from, to, transactionDate)` uses the transaction date.
- `convertHolding(amount, from, to, valuationDate)` uses the dashboard/performance valuation date.
- If exact date is missing, lookup the most recent prior rate and return conversion status `stale`.
- If no prior rate exists for the pair, return conversion status `unavailable` and let the caller show an unavailable state rather than an incorrect value.

## Modified: User Base Currency Preference

Backs **Base Currency Preference**.

| Field | Type | Rules |
|-------|------|-------|
| `base_currency` | CHAR(3) | required; default from feature 006 remains valid |

Rules:
- Preference is scoped to the authenticated user.
- Changing base currency invalidates/refetches dashboard and performance views.
- Legacy cash-only backfill uses the user's current base currency at migration/backfill time.

## Derived: Notification Dialog View Model

No separate table. Built from `notification` rows.

Fields:
- `unreadCount`
- `items[]`: `id`, `alertId`, `symbol`, `conditionLabel`, `thresholdLabel`, `observedLabel`, `triggeredAt`, `read`, `canDelete`
- `hasMore` and pagination cursor if history exceeds the dialog page size

Validation:
- Empty list produces explicit empty state.
- Read/unread counts are scoped to the current user.
- Mark-all-read applies only to visible/current-user notifications.
