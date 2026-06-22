# Contract: Authenticated Action Feedback UI

This contract defines which authenticated user actions must emit in-app toast feedback and how that feedback behaves.

## Covered Actions

The following completed actions MUST emit exactly one toast:

| Surface | Action | Success example | Failure example |
|---------|--------|-----------------|-----------------|
| Transactions | Create transaction | "Transaction created" | "Transaction could not be created" |
| Transactions | Delete transaction | "Transaction deleted" | "Transaction could not be deleted" |
| Transactions | Import commit | "12 transactions imported" | "Import needs attention" |
| Transactions | Export | "Transactions exported" | "Export failed" |
| Watchlists | Create watchlist | "Watchlist created" | "Watchlist could not be created" |
| Watchlists | Rename watchlist | "Watchlist updated" | "Watchlist could not be updated" |
| Watchlists | Delete watchlist | "Watchlist deleted" | "Watchlist could not be deleted" |
| Watchlists | Add ticker | "Ticker added to watchlist" | "Ticker could not be added" |
| Watchlists | Remove ticker | "Ticker removed from watchlist" | "Ticker could not be removed" |
| Alerts | Create alert | "Alert created" | "Alert could not be created" |
| Alerts | Update alert | "Alert updated" | "Alert could not be updated" |
| Alerts | Delete alert | "Alert deleted" | "Alert could not be deleted" |

## Behavior Rules

- Success toasts use the existing success tone.
- Failure toasts use the existing error tone.
- Each completed action emits one toast event only, even if the surrounding view rerenders.
- Failure toast detail should use the normalized API/client error message when one is available.
- Toasts appear in the shared app-level toast stack so they are visible from any authenticated page.

## Out of Scope

These actions do not require toast feedback in this feature:

- Notification mark-read / mark-all-read / delete history
- Watchlist reorder
- Background quote refreshes
- Page loads, reads, and navigation

## Import/Export Specifics

- Import success toast summarizes the number of committed rows.
- Import failure or partial-result toast indicates that one or more rows need attention.
- Export success toast confirms the export completed.
- Export failure toast indicates the export did not complete.

## Testing Contract

- Component/store tests assert exactly one toast for each covered action.
- Failure tests assert error tone plus user-visible context.
- Manual verification covers at least one action from each surface: transactions, watchlists, and alerts.
