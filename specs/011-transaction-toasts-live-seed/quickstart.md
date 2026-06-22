# Quickstart: CRUD Toast Feedback and Live Seed Accuracy

How to run and verify this feature locally.

## Prerequisites

- Docker + docker-compose for MySQL
- JDK 21
- Node 18+
- A valid authenticated dev or demo account

## Run

Stub mode:

```bash
cd backend
./mvnw quarkus:dev

cd ../frontend
npm install
npm run dev
```

Live-provider mode:

```bash
cd backend
STOCKTRACKER_MARKETDATA_PROVIDER=yahoo ./mvnw quarkus:dev

cd ../frontend
npm install
npm run dev
```

## Manual Smoke Flow

1. Sign in to the authenticated app shell.
2. Create and then delete a transaction; confirm exactly one toast appears after each action.
3. Create, rename, and delete a watchlist; confirm each action shows a success or failure toast.
4. Add and remove a ticker from a watchlist; confirm each mutation shows a toast.
5. Create and delete an alert; if alert editing is exposed in the current UI, update it and confirm a toast appears.
6. Import transactions and confirm the completion toast summarizes the result.
7. Export transactions and confirm a completion toast appears.
8. Restart backend in stub mode and load the seeded/demo account; note the seeded portfolio loads normally.
9. Restart backend in live-provider mode and load the same seeded/demo account; confirm seeded holdings use current or latest-available live-backed values rather than obviously stale bundled values.
10. If one live symbol cannot refresh, confirm the seeded account still loads and values degrade gracefully instead of failing startup.

## Verification Gates

```bash
cd backend
./mvnw -B verify

cd ../frontend
npm run verify
```

## Acceptance Mapping

| Spec item | Verification |
|-----------|--------------|
| US1 / SC-001 / SC-002 | Frontend component/store tests and manual CRUD mutation walkthrough |
| US2 / SC-003 | Transaction import/export tests plus manual import/export confirmation |
| US3 / SC-004..SC-006 | Backend provider-mode/bootstrap tests plus manual stub-vs-live startup comparison |

## Notes

- The plan assumes live-provider mode is opt-in and stub mode remains the default for reproducibility.
- Seed transaction fixtures remain deterministic; only quote-backed values change in live mode.
- Alert-notification toasts are separate from CRUD feedback and are not replaced by this feature.
