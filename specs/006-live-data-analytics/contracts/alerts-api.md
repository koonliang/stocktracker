# Contract: Alerts & Notifications API

All endpoints require a valid bearer JWT; every row is scoped to the current user
(FR-024). A user may only read/modify their own alerts and notifications.

## Alerts

### GET /api/alerts
List the current user's alerts.
```json
{
  "alerts": [
    {
      "id": "a1",
      "symbol": "AAPL",
      "conditionType": "price_above",
      "threshold": 200.00,
      "armed": true,
      "lastTriggeredAt": null,
      "createdAt": "2026-06-12T10:00:00Z"
    }
  ]
}
```

### POST /api/alerts
Create an alert.
```json
// request
{ "symbol": "AAPL", "conditionType": "price_below", "threshold": 180.00 }
```
- `conditionType` ∈ `price_above` | `price_below` | `pct_change`.
- For `pct_change`, `threshold` is a percent measured vs the symbol's **previous
  close**, resetting each trading day (FR-019).
- New alerts start `armed: true`. **201** with the created alert.

### PATCH /api/alerts/{id}
Update `conditionType` / `threshold`. Editing **re-arms** the alert
(`armed: true`) so the new condition can fire. **200** with the updated alert;
**404** if not owned by the user.

### DELETE /api/alerts/{id}
Delete the alert; it no longer fires (FR-022). **204**; **404** if not owned.

## Evaluation (internal)

`AlertService.evaluate(quote)` runs inside `QuoteRefreshJob` after each upsert:
- Fire only when `armed == true` and condition holds against the fresh quote
  (`pct_change` uses `quote.changePct` vs previous close).
- On fire: create a `notification`, set `armed=false`, `last_triggered_at=now`.
- Re-arm (`armed=true`) on a later refresh once the condition no longer holds.
- Guarantees exactly one notification per crossing (FR-021, SC-008); two distinct
  thresholds on one symbol each fire at most once per crossing (edge case).

## Notifications

### GET /api/notifications
List the current user's notifications, newest first; supports `?unread=true`.
```json
{
  "notifications": [
    {
      "id": "n1",
      "alertId": "a1",
      "message": "AAPL crossed above 200.00",
      "read": false,
      "createdAt": "2026-06-12T15:42:00Z"
    }
  ]
}
```

### POST /api/notifications/{id}/read
Mark a notification read. **204**; **404** if not owned.

The frontend polls `GET /api/notifications?unread=true` and surfaces new ones as
in-app toasts (in-app delivery only; email/push deferred).
