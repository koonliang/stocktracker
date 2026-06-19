# Contract: Frontend Notification Dialog

## Entry Point

The app shell/top bar exposes a notification control with:
- unread count badge
- accessible name "Triggered alerts"
- opens the notification dialog without navigating away

## Dialog Content

The dialog displays:
- title: "Triggered Alerts"
- unread count
- scrollable newest-first notification list
- empty state when no notifications exist
- individual mark-read action for unread rows
- mark-all-read action
- delete action for history cleanup
- close action

Each row displays:
- read/unread state
- symbol
- trigger condition and threshold
- observed value at trigger time
- trigger timestamp

## States

- Loading: skeleton or compact loading state while history loads.
- Empty: "No triggered alerts" style empty state with no error styling.
- Error: user can retry fetching history.
- Overflow: list scrolls while header/actions remain usable.

## Accessibility and Test Hooks

Requirements:
- Dialog uses focus trap and returns focus to the opener when closed.
- Close and mark-read actions are keyboard accessible.
- Unread state is represented visually and textually.
- No in-dialog text overlaps at mobile or desktop widths.

Suggested `data-testid` values:
- `notification-dialog-trigger`
- `notification-dialog`
- `notification-unread-count`
- `notification-row`
- `notification-row-read-state`
- `notification-mark-read`
- `notification-mark-all-read`
- `notification-delete`
- `notification-empty`
