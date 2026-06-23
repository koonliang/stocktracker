# Contract: Live Seed Runtime Behavior

This contract defines how seeded/demo portfolio values behave under different market-data provider modes.

## Provider Modes

| Active provider mode | Transaction source | Quote-backed seeded values |
|----------------------|--------------------|----------------------------|
| `stub` | deterministic seeded transactions | deterministic stub/cached values |
| `live` | deterministic seeded transactions | live-provider-backed current or latest-available cached values |

## Runtime Rules

- Seeded transaction history remains deterministic in all modes.
- When live provider mode is enabled, seeded symbols referenced by the active seed profile are refreshed through the existing quote-refresh/provider path.
- If a seeded symbol already exists, quote refresh is idempotent and must not duplicate instruments or transactions.
- If one or more live quote refreshes fail, the seeded account still loads and uses stale or existing cached values according to current quote-cache behavior.
- User isolation is unchanged: seeded/demo portfolio refresh affects only the owning seeded or demo user.

## Startup / Refresh Expectations

1. Resolve the seed profile for the seeded or demo user.
2. Insert or refresh deterministic seed transactions for that profile.
3. Collect all seeded symbols referenced by those transactions.
4. If provider mode is `live`, attempt quote refresh for those symbols through the active provider path.
5. If provider mode is `stub`, keep existing deterministic behavior.
6. Return a usable seeded account regardless of individual symbol refresh failures.

## Failure Handling

- Unknown or missing seed instrument metadata is still a bootstrap error.
- Live quote unavailability for a valid seeded symbol is not a bootstrap error by itself.
- When live-provider mode degrades, values follow the app’s existing stale/unavailable semantics instead of inventing a separate seed-only state.

## Verification Contract

- Backend tests cover stub mode, live mode, and live-mode partial-failure fallback.
- Manual verification checks that seeded dashboard values change when live provider mode is enabled and remain deterministic when stub mode is used.
