# Phase 1 Data Model: CRUD Toast Feedback and Live Seed Accuracy

This feature is primarily behavioral. It does not require a new persisted table by default. The main design work is in derived frontend feedback state and runtime seed-value resolution.

## Derived: `ActionFeedbackEvent`

Backs a single visible toast for a completed user action.

| Field | Type | Rules |
|-------|------|-------|
| `id` | string | unique per completed action event; prevents duplicate toast rendering |
| `scope` | enum | `transaction`, `watchlist`, `watchlist_ticker`, `alert`, `transaction_import`, or `transaction_export` |
| `operation` | enum | `add`, `update`, `delete`, `import`, `export` |
| `outcome` | enum | `success` or `failure` |
| `title` | string | short action-oriented message visible at a glance |
| `message` | string/null | optional details or error context |
| `tone` | enum | `success` for successful completion, `error` for failure |
| `triggeredAt` | timestamp | when the action completed on the client |

Rules:
- Exactly one `ActionFeedbackEvent` is emitted per completed mutation/import/export event.
- Success and failure events share the same structure so the same renderer can display them.
- Events are transient UI state only; they are not stored in the backend.

## Derived: `CrudActionCatalog`

Defines which authenticated actions must produce `ActionFeedbackEvent`.

| Surface | Action | Required feedback |
|---------|--------|-------------------|
| Transactions | create transaction | success/failure toast |
| Transactions | delete transaction | success/failure toast |
| Transactions | import commit | success/failure or partial-result toast |
| Transactions | export | success/failure toast |
| Watchlists | create watchlist | success/failure toast |
| Watchlists | rename watchlist | success/failure toast |
| Watchlists | delete watchlist | success/failure toast |
| Watchlists | add ticker | success/failure toast |
| Watchlists | remove ticker | success/failure toast |
| Alerts | create alert | success/failure toast |
| Alerts | update alert | success/failure toast |
| Alerts | delete alert | success/failure toast |

Out of scope:
- Notification mark-read / delete history actions
- Pure reads, polling, and page navigation
- Watchlist reorder unless later specified

## Runtime: `SeedQuoteResolutionMode`

Determines how seeded/demo portfolio valuations obtain quote-backed values.

| Field | Type | Rules |
|-------|------|-------|
| `providerMode` | enum | `stub` or `live` based on existing provider configuration |
| `transactionSeedSource` | enum | always `deterministic_seed` in this feature |
| `quoteValueSource` | enum | `stub_quote_cache`, `live_quote_cache`, or `stale_cached_quote` |
| `refreshAttempted` | boolean | whether bootstrap/demo refresh attempted provider-backed quote refresh |
| `degraded` | boolean | true when live mode was requested but one or more seeded symbols used stale/fallback values |

Rules:
- `providerMode=stub` keeps existing deterministic seed behavior unchanged.
- `providerMode=live` keeps the same seed transactions but refreshes seeded symbol quotes via the active live provider path.
- If live refresh fails for a symbol, the portfolio still loads and falls back to stale or existing cached values.

## Runtime: `DemoPortfolioRefreshBatch`

Represents one seeded account refresh during bootstrap or demo-user refresh.

| Field | Type | Rules |
|-------|------|-------|
| `userId` | long | required; current seed or demo account owner |
| `seedProfile` | string | resolved profile from `seed/demo-transactions.json` |
| `symbols` | set<string> | all seeded instrument symbols referenced by that profile |
| `transactionsInserted` | integer | count of deterministic seeded transaction rows inserted |
| `quotesRefreshed` | integer | count of seeded symbols successfully refreshed through the active provider path |
| `quoteFailures` | integer | count of seeded symbols that fell back to stale/cached behavior |

Rules:
- Transaction insertion remains the source of truth for seeded holdings.
- Quote refresh is additive runtime hydration, not a replacement for seed transactions.
- Failure to refresh some or all quotes does not invalidate the inserted transactions.
