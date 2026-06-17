# Contract: Notification Dialog and History API

All endpoints require a valid bearer JWT. Every notification is scoped to the current user.

## GET /api/notifications

Returns triggered-alert notification history, newest first.

**Query params**
- `unread` optional boolean: when true, return unread notifications only.
- `limit` optional integer, default `25`, max `100`.
- `cursor` optional opaque cursor for pagination.

**200 Response**

```json
{
  "unreadCount": 3,
  "notifications": [
    {
      "id": "n1",
      "alertId": "a1",
      "symbol": "AAPL",
      "conditionType": "price_above",
      "threshold": 300.00,
      "thresholdCurrency": "USD",
      "observedValue": 301.25,
      "observedCurrency": "USD",
      "triggeredAt": "2026-06-17T10:42:00Z",
      "read": false,
      "message": "AAPL crossed above 300.00 USD"
    }
  ],
  "nextCursor": null
}
```

Behavior:
- Sort by `triggeredAt DESC`, then symbol, then id.
- Empty history returns `notifications: []` and `unreadCount: 0`.
- Read notifications remain in history until deleted or until the associated alert is deleted.

## POST /api/notifications/{id}/read

Marks one notification as read.

Responses:
- `204` on success.
- `404` if the notification is not owned by the current user or does not exist.

## POST /api/notifications/read-all

Marks all current-user unread notifications as read.

**Request**

```json
{
  "ids": ["n1", "n2"]
}
```

Rules:
- If `ids` is present, only those current-user notifications are marked read.
- If `ids` is omitted or empty, all current-user unread notifications are marked read.

**200 Response**

```json
{ "updated": 2, "unreadCount": 0 }
```

## DELETE /api/notifications/{id}

Deletes one notification from history.

Responses:
- `204` on success.
- `404` if the notification is not owned by the current user or does not exist.

## Alert Evaluation Contract

`AlertEvaluationService.evaluate(quote)`:
- Creates at most one notification per alert threshold crossing.
- Sets the alert disarmed after a notification is created.
- Re-arms only after a later quote refresh shows the condition is false.
- Creates another notification only when a re-armed alert crosses again.
- Deleting an alert deletes all associated notifications.
