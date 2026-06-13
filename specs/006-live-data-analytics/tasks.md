---
description: "Task list for Live Market Data & Portfolio Analytics"
---

# Tasks: Live Market Data & Portfolio Analytics

**Input**: Design documents from `/specs/006-live-data-analytics/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: INCLUDED. The project Constitution (Principle I — Test Verification,
NON-NEGOTIABLE) requires automated tests for every feature, so each user story
carries backend integration tests, targeted unit tests for the calculation
engines, and Selenium e2e journeys. All tests run against the deterministic
`stub` provider (no network).

**Organization**: Tasks are grouped by user story (US1–US4) for independent
implementation and testing.

## Path Conventions

- Backend: `backend/src/main/java/com/stocktracker/`, tests `backend/src/test/java/com/stocktracker/`
- Backend resources: `backend/src/main/resources/`
- Frontend: `frontend/src/`, tests co-located / `frontend/src/test/`
- e2e (Selenium, Java): `e2e/src/test/java/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencies, configuration, and stub fixtures shared by all stories

- [X] T001 Add `quarkus-rest-client-jackson` to `backend/pom.xml` (for the Yahoo and Frankfurter REST clients)
- [X] T002 [P] Add provider config keys with `stub` defaults to `backend/src/main/resources/application.properties`: `stocktracker.marketdata.provider=${STOCKTRACKER_MARKETDATA_PROVIDER:stub}`, `stocktracker.marketdata.refresh-interval=60s`, `stocktracker.fx.provider=${STOCKTRACKER_FX_PROVIDER:stub}`, base-currency default `USD`
- [X] T003 [P] Create stub fixtures `backend/src/main/resources/provider-stub/quotes.json` and `fx-rates.json` — include at least one SGX `.SI` symbol (SGD) and a fixed `USD↔SGD` rate (research §2, SC-012/013)
- [X] T004 [P] Set provider env vars in `docker-compose.yml` (backend service stays on `stub`) and document `yahoo`/`frankfurter` opt-in

**Checkpoint**: Build resolves new dependency; config + fixtures present

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The single additive migration, the entity changes it forces, and the
provider seams every story builds on. With `hibernate-orm.database.generation=validate`,
the three existing entities MUST be updated in lockstep with `V4` or the app won't boot.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create Flyway migration `backend/src/main/resources/db/migration/V4__live_data_analytics.sql` covering ALL of: `instrument.currency CHAR(3) NOT NULL DEFAULT 'USD'`; `app_user.base_currency CHAR(3) NOT NULL DEFAULT 'USD'`; `portfolio_transaction` — make `instrument_symbol` nullable, add `amount DECIMAL(19,4) NULL`, `currency CHAR(3) NULL`, widen `transaction_type` to {buy,sell,dividend,split,deposit,withdrawal,fee}; new tables `instrument_quote`, `fx_rate`, `alert`, `notification` (per data-model.md)
- [X] T006 [P] Update `domain/Instrument.java` — add `currency` field (validate against V4)
- [X] T007 [P] Update `domain/AppUser.java` — add `baseCurrency` field
- [X] T008 [P] Update `domain/PortfolioTransaction.java` — nullable `instrumentSymbol`, add `amount`, `currency`, extend `transactionType` enum to the seven types
- [X] T009 [P] Define `service/provider/MarketDataProvider.java` interface + `ProviderQuote`/`ProviderDailyBar`/`ProviderSymbol` records (contracts/market-data-provider.md)
- [X] T010 [P] Define `service/provider/FxRateProvider.java` interface + `ProviderFxRate` record (contracts/fx-rate-provider.md)
- [X] T011 Implement provider-selection CDI producer `service/provider/ProviderConfig.java` choosing `stub`/`yahoo` and `stub`/`frankfurter` from config (runtime selection, one build for dev+prod — research §2)
- [X] T012 [P] Implement `service/provider/StubMarketDataProvider.java` — deterministic quotes/history/search from `provider-stub/*` + seeded bars, movement = bounded fn of symbol + injectable clock (contracts/market-data-provider.md)
- [X] T013 [P] Implement `service/provider/StubFxRateProvider.java` — fixed seeded daily rates incl. `USD↔SGD`
- [X] T014 [P] Extend `TransactionValidationService` with per-type rules from data-model.md (symbol-required vs cash types, positive quantity/amount, currency rules) in `service/`

**Checkpoint**: App boots on `stub`, schema validates, provider seams injectable. Stories can begin.

---

## Phase 3: User Story 1 - See Live, Auto-Updating Prices (Priority: P1) 🎯 MVP

**Goal**: Replace static seed prices with live cached quotes that auto-update on the
dashboard/watchlist with a last-updated/stale indicator; let users search and add any
provider symbol (incl. global/SGX); show values in a user-chosen base currency alongside
native currency.

**Independent Test**: Open dashboard/watchlist for held symbols → prices match the
(stub) source, each shows "last updated", values tick on poll without reload; stop the
backend → rows go stale not blank; search "DBS"/`D05.SI`, add it → SGD price appears
immediately and contributes to the base-currency total.

### Tests for User Story 1

- [X] T015 [P] [US1] Integration test `backend/src/test/java/com/stocktracker/api/QuotesResourceTest.java` — `/api/quotes` returns cached values, stale fallback, unknown symbol → `price:null,stale:true` (FR-002/006)
- [X] T016 [P] [US1] Integration test `backend/.../api/InstrumentSearchResourceTest.java` — search returns matches incl. `.SI`; add-on-demand creates instrument + immediate quote; unrecognized symbol → 422, no row (FR-026/027, SC-010/011/012)
- [X] T017 [P] [US1] Integration test `backend/.../service/QuoteRefreshJobTest.java` — refresh upserts cache, partial-failure keeps prior values + flips stale (FR-006)
- [X] T018 [P] [US1] Unit test `backend/.../service/CurrencyServiceTest.java` — native→base conversion, mixed-currency total reconciles, missing pair → last-known stale (SC-013, edge case)
- [X] T019 [P] [US1] e2e `e2e/src/test/java/.../LiveQuotesJourneyTest.java` — last-updated indicator + base-currency controls; search + add global SGX symbol (SC-001/010/012). Runs against the running stack (docker-compose).

### Implementation for User Story 1

- [X] T020 [P] [US1] `domain/InstrumentQuote.java` entity + `persistence/QuoteRepository.java` (upsert/find by symbols)
- [X] T021 [P] [US1] `domain/FxRate.java` entity + `persistence/FxRateRepository.java`
- [X] T022 [US1] `service/CurrencyService.java` — `convert(amount, from, to, onDate)` over `fx_rate`, last-known-rate fallback marked stale (contracts/currency-api.md)
- [X] T023 [US1] `service/FxRefreshJob.java` `@Scheduled` daily — fetch rates for in-use currencies + every user `base_currency`, upsert `fx_rate`, retain last on failure
- [X] T024 [US1] `service/QuoteCacheService.java` + `service/QuoteRefreshJob.java` `@Scheduled(every=refresh-interval)` — tracked-symbol union, batch fetch via `MarketDataProvider`, upsert cache, set `fetched_at`; staleness by `fetched_at` not market hours (FR-028, research §4)
- [X] T025 [US1] `service/HistoricalBackfillService.java` — on-demand daily-close backfill into `instrument_price_bar` (FR-025/027)
- [X] T026 [US1] `service/MarketDataService.java` — search proxy + add-on-demand (create instrument from provider symbol, immediate quote + backfill, 422 on unrecognized) (instruments-search-api.md)
- [X] T027 [US1] On-demand refresh fallback in `QuoteCacheService`: when `/api/quotes` reads a stale entry, trigger a fetch (mitigates Lambda cold-scheduler — plan "nuance")
- [X] T028 [P] [US1] `service/provider/YahooMarketDataProvider.java` `@RegisterRestClient` — `/v7/finance/quote`, `/v8/finance/chart`, `/v1/finance/search`, no key, errors caught not thrown (contracts/market-data-provider.md)
- [X] T029 [P] [US1] `service/provider/FrankfurterFxRateProvider.java` `@RegisterRestClient` — `api.frankfurter.app` `/latest` + `/{date}`, no key
- [X] T030 [US1] `api/QuotesResource.java` — `GET /api/quotes?symbols=` reads cache only (contracts/quotes-api.md)
- [X] T031 [US1] `api/InstrumentSearchResource.java` — `GET /api/instruments/search`, `POST /api/instruments` (instruments-search-api.md)
- [X] T032 [US1] `api/SettingsResource.java` — `GET`/`PUT /api/me/base-currency` with `supported` list, 422 unsupported, persist `app_user.base_currency` (currency-api.md)
- [X] T033 [US1] Extend `PortfolioService` + dashboard/watchlist DTOs: source current price from `instrument_quote` (fallback price bar), add native (`currency`,`nativePrice`,`nativeMarketValue`) + base-converted fields + `asOf`/`fetchedAt`/`stale`; totals + `baseCurrency` in base (contracts/quotes-api.md §Dashboard, FR-031/032)
- [X] T034 [P] [US1] Frontend `src/api/quotesApi.ts`, `searchApi.ts`, `settingsApi.ts` (fetch wrappers per existing `client.ts` pattern)
- [X] T035 [US1] Frontend `src/stores/quotesStore.ts` — visibility-aware ~30s polling of `/api/quotes`, per-symbol price/asOf/stale; merge into `portfolioStore`/watchlist store (frontend-routes.md)
- [X] T036 [US1] Frontend live-price UI: last-updated + stale badge on `SummaryTiles`/`HoldingsTable`/watchlist rows; native+base value display; `data-testid`s `quote-last-updated`,`quote-stale`,`holding-native-value`,`holding-base-value` (FR-004/005/032)
- [X] T037 [P] [US1] Frontend `src/features/.../SymbolSearch.tsx` — debounced search + add, empty/422 states; `data-testid`s `symbol-search`,`symbol-search-result`,`symbol-add`
- [X] T038 [P] [US1] Frontend `src/components/.../BaseCurrencySelect.tsx` — read/PUT base currency, re-fetch on change; `data-testid` `base-currency-select`
- [X] T039 [P] [US1] Frontend component tests (Vitest+MSW) for quote polling/stale rendering and symbol-search add flow in `frontend/src/...`

**Checkpoint**: Live, auto-updating, multi-currency dashboard with global-symbol add — independently shippable MVP.

---

## Phase 4: User Story 2 - Record Dividends, Splits & Cash Movements (Priority: P2)

**Goal**: Support dividend/split/deposit/withdrawal/fee transactions with split-aware
cost basis (retroactive), a cash balance, and CSV v2 import/export that still accepts v1.

**Independent Test**: Add a position, record a dividend, a 2-for-1 split, a deposit; share
count + per-share basis adjust (total basis unchanged), cash reflects movements; a v1 CSV
imports unchanged; a v2 CSV round-trips with version labelled.

### Tests for User Story 2

- [X] T040 [P] [US2] Unit test `backend/.../service/CostBasisEngineTest.java` — 2-for-1 and reverse(1-for-10)/non-integer(3-for-2) splits preserve total basis, apply retroactively, fractional shares (SC-005, edge cases)
- [X] T041 [P] [US2] Integration test `backend/.../service/CashBalanceTest.java` — deposit/withdrawal/fee/dividend/buy/sell update per-currency cash balance correctly
- [X] T042 [P] [US2] Integration test `backend/.../service/CsvImportV2Test.java` — v1 file imports 100% unchanged; v2 new types round-trip; mixed v1/v2 in one session no cross-contamination (SC-004, FR-011/012, edge case)
- [X] T043 [P] [US2] e2e `e2e/src/test/java/.../TransactionTypesTest.java` — record split via UI, verify adjusted holding; import v1 CSV succeeds

### Implementation for User Story 2

- [X] T044 [US2] `service/CostBasisEngine.java` — replay transactions in trade-date order into per-symbol lots; `split` rescales open-lot qty + per-share basis (total preserved, retroactive); dividend = income (no basis change); reject sell exceeding available lots (research §5, FR-008/009, edge case)
- [X] T045 [US2] `service/CashBalanceService.java` — per-currency running balance from cash-affecting transactions; expose base-converted total via `CurrencyService` (FR-010)
- [X] T046 [US2] Extend transaction create/update flow + DTOs in `api/TransactionsResource.java`/`service` for the new types (symbol/amount/ratio/currency per type) using `TransactionValidationService` (FR-007)
- [X] T047 [US2] Extend `service/TransactionImportService.java` — header/row-based v1-vs-v2 detection, parse new types, report detected version in preview (csv-schema-v2.md)
- [X] T048 [US2] Extend `service/TransactionExportService.java` — always write v2 header `date,ticker,type,quantity,price,fees,amount,currency`
- [X] T049 [US2] Frontend transaction form: `type` selector with new options, fields show/hide by type (incl. currency for cash); update `src/features/transactions/` + `portfolioStore` (frontend-routes.md)
- [X] T050 [US2] Frontend import preview labels detected CSV version + renders amount/currency columns; extend `src/lib/csv.ts` (papaparse) for v2 superset

**Checkpoint**: Full transaction model + split-correct cost basis + CSV v1/v2; US1 still works.

---

## Phase 5: User Story 3 - Understand Realized vs. Unrealized Performance (Priority: P2)

**Goal**: A Performance page with realized P&L per closed lot (FIFO default, LIFO toggle),
unrealized P&L, cumulative-return chart, time-weighted return, and per-holding contribution
— all in the base currency.

**Independent Test**: For an account with ≥1 closed + several open positions, the page shows
FIFO realized P&L per lot, recomputes on LIFO toggle, a cumulative-return chart + TWR over a
selected window, and contributions that sum to the total.

**Dependency note**: consumes US2's `CostBasisEngine` (lot replay + splits).

### Tests for User Story 3

- [ ] T051 [P] [US3] Unit test `backend/.../service/LotMatchingTest.java` — realized P&L per closed lot matches independent FIFO calc; LIFO/specific differ correctly; with a split in history (SC-006)
- [ ] T052 [P] [US3] Unit test `backend/.../service/PerformanceServiceTest.java` — TWR neutralizes cash flows; contributions reconcile to total within tolerance; multi-currency converted per-day (SC-007, FR-016/017)
- [ ] T053 [P] [US3] Integration test `backend/.../api/PerformanceResourceTest.java` — `/api/performance` windows/methods; no-closed-positions → realizedPnL 0, empty lots, flat series, not error (edge case); backfill triggered on missing history (FR-025)
- [ ] T054 [P] [US3] e2e `e2e/src/test/java/.../PerformanceTest.java` — window + FIFO/LIFO toggle change figures; chart + contribution render

### Implementation for User Story 3

- [ ] T055 [US3] `service/LotMatchingService.java` — realized P&L per closed lot under fifo/lifo/specific via `CostBasisEngine`, native + base (research §7)
- [ ] T056 [US3] `service/PerformanceService.java` — daily portfolio-value series (split-adjusted holdings × daily closes, each day base-converted), TWR by chaining sub-period returns, per-holding contribution; trigger `HistoricalBackfillService` on gaps (research §8, performance-api.md)
- [ ] T057 [US3] `api/PerformanceResource.java` — `GET /api/performance?window=&method=` returning realized/unrealized/TWR/series/contributions in base currency (performance-api.md)
- [ ] T058 [P] [US3] Frontend `src/api/performanceApi.ts` + `src/routes/PerformanceRoute.tsx` route wired in `App.tsx`/nav
- [ ] T059 [US3] Frontend Performance UI: `ReturnChart` (recharts), realized-lot table, unrealized summary, `ContributionTable`, window selector + FIFO/LIFO toggle; `data-testid`s `performance-page`,`return-chart`,`realized-table`,`contribution-table`,`lot-method-toggle`,`perf-window-select`

**Checkpoint**: Performance analytics complete; US1+US2 still work.

---

## Phase 6: User Story 4 - Get Alerted When a Price Crosses a Threshold (Priority: P3)

**Goal**: Users create price-above/below/%-change alerts; evaluated on each quote refresh;
fire exactly once per crossing as an in-app toast; editable/deletable.

**Independent Test**: Create a "price above X" alert, drive the (stub) quote past it → one
in-app toast; further refreshes above threshold don't re-fire; edit/delete works.

**Dependency note**: consumes US1's `QuoteRefreshJob` (evaluation hook) and cached quotes.

### Tests for User Story 4

- [ ] T060 [P] [US4] Integration test `backend/.../service/AlertEvaluationTest.java` — armed→fire→disarm→re-arm; exactly one notification per crossing; two thresholds on one symbol each fire once; `pct_change` vs previous close (SC-008, FR-021, edge case)
- [ ] T061 [P] [US4] Integration test `backend/.../api/AlertsResourceTest.java` — CRUD scoped per user; edit re-arms; delete stops firing; cross-user 404 (FR-022/024)
- [ ] T062 [P] [US4] e2e `e2e/src/test/java/.../AlertsTest.java` — create alert, drive crossing, exactly one toast appears

### Implementation for User Story 4

- [ ] T063 [P] [US4] `domain/Alert.java` + `domain/Notification.java` entities + `persistence/AlertRepository.java` + `persistence/NotificationRepository.java` (user-scoped)
- [ ] T064 [US4] `service/AlertEvaluationService.java` — `evaluate(quote)` hooked into `QuoteRefreshJob` after upsert; armed-flag fire-once + re-arm; write notification (alerts-api.md, research §9)
- [ ] T065 [US4] `api/AlertsResource.java` — `GET/POST/PATCH/DELETE /api/alerts`, edit re-arms (alerts-api.md)
- [ ] T066 [US4] `api/NotificationsResource.java` — `GET /api/notifications?unread=`, `POST /api/notifications/{id}/read`
- [ ] T067 [P] [US4] Frontend `src/api/alertsApi.ts`, `notificationsApi.ts` + `src/routes/AlertsRoute.tsx` (AlertList + AlertForm with react-hook-form/zod), wired in `App.tsx`/nav; `data-testid`s per frontend-routes.md
- [ ] T068 [US4] Frontend `NotificationToaster` mounted once in `App.tsx` — polls unread notifications, shows toasts, marks read; `data-testid` `notification-toast`

**Checkpoint**: All four user stories independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T069 [P] Update `quickstart.md` if any endpoint/config drifted during implementation
- [ ] T070 [P] Add `data-testid`s audit + Page Objects in `e2e/src/test/java/.../pages/` for the new routes
- [ ] T071 Verify Constitution gates: backend `./mvnw -B verify`, frontend `npm run verify`, e2e `./mvnw -B test` all green (quickstart.md)
- [ ] T072 [P] Run lint/format clean on all changed backend + frontend files (Principle II)
- [ ] T073 Run full `quickstart.md` acceptance-mapping smoke pass (SC-001..SC-013)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (P1)**: no dependencies
- **Foundational (P2)**: depends on Setup; **BLOCKS all stories** (migration + entity sync + provider seams)
- **US1 (P3)**: after Foundational — the MVP; introduces quote cache, FX/currency runtime, search/add
- **US2 (P4)**: after Foundational — independent of US1 (different files); introduces `CostBasisEngine`
- **US3 (P5)**: after Foundational; **consumes US2's `CostBasisEngine`** for realized P&L
- **US4 (P6)**: after Foundational; **consumes US1's `QuoteRefreshJob`** for evaluation
- **Polish (P7)**: after all desired stories

### Within Each User Story

- Tests written first and expected to fail → entities → repositories → services → endpoints → frontend
- Models before services; services before endpoints; backend before its frontend wiring

### Parallel Opportunities

- Setup T002/T003/T004 in parallel
- Foundational T006/T007/T008 (entities) and T009/T010 (interfaces) and T012/T013 (stubs) in parallel; T005 (migration) first, T011 after interfaces
- All `[P]` test tasks within a story in parallel
- After Foundational, **US1 and US2 can run in parallel** (different files); US3 starts once US2's engine lands; US4 starts once US1's refresh job lands
- Real-provider impls T028/T029 are `[P]` (independent files) and can lag behind the stub-based work

---

## Parallel Example: User Story 1

```bash
# Tests together:
Task: "QuotesResourceTest"  Task: "InstrumentSearchResourceTest"
Task: "QuoteRefreshJobTest"  Task: "CurrencyServiceTest"

# Entities together:
Task: "InstrumentQuote entity + QuoteRepository"
Task: "FxRate entity + FxRateRepository"

# Real providers (independent files) in parallel with cache/UI work:
Task: "YahooMarketDataProvider"  Task: "FrankfurterFxRateProvider"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 Setup → 2. Phase 2 Foundational (CRITICAL) → 3. Phase 3 US1 →
   **STOP & VALIDATE**: live, auto-updating, multi-currency dashboard with global-symbol add.

### Incremental Delivery

Foundation → US1 (MVP, deploy) → US2 (transactions/CSV, deploy) → US3 (performance, deploy)
→ US4 (alerts, deploy). Each story adds value without breaking the previous.

### Notes

- All tests use the `stub` provider; the real Yahoo/Frankfurter paths are exercised only in
  `prod`/opt-in dev and must never throw into a request path (last cached value served).
- `[P]` = different files, no incomplete-task dependency. `[Story]` maps to spec user stories.
- Constitution gates (test/lint/compile) must be green before each story is "done".
