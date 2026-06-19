# Quickstart: Alert Notifications and Currency Views

How to run and verify this feature locally. Assumes feature-006 live-data stub providers are available.

## Prerequisites

- Docker + docker-compose for MySQL
- JDK 21
- Node 18+
- Existing auth/dev seed account from prior features

## Run

```bash
cd backend
./mvnw quarkus:dev

cd ../frontend
npm install
npm run dev
```

## Manual Smoke Flow

1. Sign in and open the app shell.
2. Create an alert for AAPL where the stub quote crosses the threshold.
3. Open the triggered-alert notification dialog from the app shell.
4. Confirm unread count, row content, mark-read, mark-all-read, close, empty, and history behavior.
5. Keep the quote above the threshold and confirm only one notification exists for that crossing.
6. Move the stub quote below the threshold, then above again, and confirm a second notification is created.
7. Delete the alert and confirm associated notification history is removed.
8. Create or edit transactions and confirm every monetary transaction shows a currency.
9. Import v1 and v2 CSV files and confirm currency defaults/validation match the contracts.
10. Change base currency and verify dashboard/performance values refresh with FX status visible when stale.

## Verification Gates

```bash
cd backend
./mvnw -B verify

cd ../frontend
npm run verify

cd ../e2e
./mvnw -B test
```

## Acceptance Mapping

| Spec item | Verification |
|-----------|--------------|
| US1 / SC-001 | Review notification dialog mockup/components in Storybook-equivalent test route or component test |
| US2 / SC-002..SC-004 | Notification dialog tests with unread/history/mark-read/re-arm fixtures |
| US3 / SC-005..SC-006 | Transaction form/import/export tests require and preserve currency |
| US4 / SC-007..SC-009 | Dashboard/performance tests switch base currency and assert stale FX indicators |

## Notes

- Email, SMS, and push notification delivery are out of scope.
- Real providers are not required for tests; use deterministic stub quotes and FX rates.
- If exact-date FX is missing, the UI should show converted values with a stale indicator, not fail the whole view.
