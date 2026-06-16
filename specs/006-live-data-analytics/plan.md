# Implementation Plan: Live Market Data & Portfolio Analytics

**Branch**: `006-live-data-analytics` | **Date**: 2026-06-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-live-data-analytics/spec.md`

## Summary

Replace static seed prices with live quotes from a pluggable market-data provider
(concrete impl: **Yahoo Finance** via a backend HTTP proxy; a **deterministic stub**
is the default in tests and local dev), add dividend/split/cash transaction types
with split-aware cost-basis and a FIFO/LIFO/specific-lot realized-P&L engine, a
Performance page (cumulative return, time-weighted return, per-holding contribution),
multi-currency support with daily FX from **Frankfurter (ECB)** converted to a
user-chosen base currency, and in-app price alerts evaluated on each quote refresh.

Technical approach: a single `MarketDataProvider` seam (search, current quote, daily
history) and a single `FxRateProvider` seam, each with a `stub` and a real
implementation selected by env config. A `@Scheduled` job refreshes cached quotes
~every 60s and evaluates alerts; the frontend polls a batch `/api/quotes` endpoint.
New Flyway migration `V4` adds quote cache, historical prices, FX rates, instrument
currency, extended transaction types, alerts, notifications, and a per-user base
currency setting. Reuses the existing `CurrentUser` per-user scoping model.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.6 / React 18.3 (frontend)
**Primary Dependencies**: Quarkus 3.15.2 (quarkus-rest, hibernate-orm-panache, jdbc-mysql, flyway, scheduler, smallrye-jwt, rest-client-jackson for provider HTTP, scheduler); React + Vite 5.4, zustand 4.5, recharts 2.13, papaparse 5.4, react-hook-form + zod
**Storage**: MySQL 8.4 via Hibernate ORM + Panache; Flyway migrations (latest applied = V3, this feature adds V4)
**Testing**: Backend JUnit 5 + QuarkusTest + REST-Assured + Testcontainers MySQL (`IntegrationTestSupport`, `MySqlTestResource`, `@TestSecurity`); Frontend Vitest + Testing Library + MSW; e2e Selenium (Java) — all use the deterministic stub provider
**Target Platform**: AWS Lambda (quarkus-amazon-lambda-http) behind API Gateway + RDS MySQL; frontend S3 + CloudFront; local via docker-compose
**Project Type**: web (frontend + backend)
**Performance Goals**: quote freshness ≤ 60s during market hours (SC-001/002); `/api/quotes` served from cache, no live provider call per request (FR-002); just-added symbol shows a price within a few seconds (SC-010)
**Constraints**: provider rate limits handled by serving last cached value + retry next cycle (FR-006); daily FX granularity sufficient (no intraday); per-exchange staleness, no US-only calendar assumption (FR-028); all data per-user scoped (FR-024)
**Scale/Scope**: single-user-per-account personal tracker; tracked symbol universe = union of all users' held/watched symbols (tens–low hundreds); refresh batched per provider call

### Provider selection (resolved in clarify)

- **MarketDataProvider** = Yahoo Finance. Backend calls Yahoo's JSON endpoints directly via a Quarkus REST client (`query1.finance.yahoo.com` chart/quote/search) — the same endpoints `yfinance`/`yahooquery` wrap; no Java SDK needed. Endpoints are unofficial; reliability risk accepted.
- **FxRateProvider** = Frankfurter (`api.frankfurter.app`, ECB daily rates), separate REST client.
- Selection by config: `stocktracker.marketdata.provider` (`stub` | `yahoo`) and `stocktracker.fx.provider` (`stub` | `frankfurter`), env-overridable via `STOCKTRACKER_MARKETDATA_PROVIDER` / `STOCKTRACKER_FX_PROVIDER`. Default = `stub` for tests and local dev; `yahoo`/`frankfurter` in prod. Local dev opts in via env override. Neither real provider needs an API key.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment | Status |
|-----------|------------|--------|
| I. Test Verification (NON-NEGOTIABLE) | Deterministic stub provider makes quotes/history/FX reproducible offline; every FR maps to backend integration tests + frontend component tests; cost-basis/lot/TWR math validated against fixed fixtures (SC-005/006/007/013). | PASS |
| II. Lint & Style Compliance (NON-NEGOTIABLE) | Reuses existing ESLint/Prettier (frontend) and build-time checks (backend); no new style config. | PASS |
| III. Compilation Integrity (NON-NEGOTIABLE) | Backend `mvn` build + frontend `tsc --noEmit`/`vite build` must stay green; Flyway `validate` keeps schema/entity in sync. | PASS |
| IV. Simplicity & YAGNI | Two provider seams justified by the explicit stub-vs-real clarification; one new migration; no speculative provider abstraction beyond Yahoo+stub / Frankfurter+stub. Email/push deferred (in-app only). Specific-lot selection UX is the only deferred sub-decision. | PASS |
| V. Specification-Driven Development | Plan traces to spec FR-001..FR-032 and SC-001..SC-013; clarifications recorded in spec. | PASS |

No violations — Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/006-live-data-analytics/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (REST contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/com/stocktracker/
├── domain/              # + Quote, HistoricalPrice, FxRate, Alert, Notification,
│                        #   UserSetting; Instrument gains `currency`; PortfolioTransaction
│                        #   transactionType enum extended (DIVIDEND/SPLIT/DEPOSIT/WITHDRAWAL/FEE)
├── api/                 # + QuotesResource, InstrumentSearchResource, PerformanceResource,
│                        #   AlertsResource, NotificationsResource, SettingsResource
├── service/             # + QuoteRefreshJob (@Scheduled), QuoteCacheService, MarketDataService,
│                        #   FxRateService, CostBasisEngine (FIFO/LIFO/specific-lot), SplitService,
│                        #   PerformanceService, AlertEvaluationService, CurrencyConversionService;
│                        #   extend TransactionImportService/ExportService for CSV v2
├── service/provider/    # MarketDataProvider + FxRateProvider interfaces (seams),
│                        #   YahooMarketDataProvider, FrankfurterFxRateProvider,
│                        #   StubMarketDataProvider, StubFxRateProvider (CDI @IfBuildProperty/@Lookup by config)
├── persistence/         # + QuoteRepository, HistoricalPriceRepository, FxRateRepository,
│                        #   AlertRepository, NotificationRepository, UserSettingRepository
├── dto/                 # + quote/search/performance/alert/notification/settings DTOs
└── resources/db/migration/V4__live_data_analytics.sql

backend/src/test/java/com/stocktracker/   # integration tests per resource + unit tests for engines, all on stub provider
backend/src/main/resources/provider-stub/ # seeded stub fixtures (quotes, history, fx)

frontend/src/
├── api/                 # + quotesApi, searchApi, performanceApi, alertsApi, notificationsApi, settingsApi
├── routes/              # + PerformanceRoute, AlertsRoute (and a base-currency setting surface)
├── features/            # + performance/, alerts/, quotes/ (live-price hooks, last-updated/stale UI), search add-symbol
├── stores/              # + quotesStore (polling), alertsStore, notificationsStore; extend portfolioStore (multi-currency, new txn types)
└── lib/                 # extend types.ts, format.ts (currency), csv.ts (v2 schema + v1 compat)

infra/                   # provider config + (optional) Secrets Manager entry if a keyed provider is later used; Yahoo/Frankfurter need no key
```

**Structure Decision**: Existing **web** layout (separate `backend/` Quarkus + `frontend/` React) is reused unchanged. New code follows the established `domain/`/`api/`/`service/`/`persistence/`/`dto/` packaging; the only structural addition is a `service/provider/` sub-package to hold the two provider seams and their stub/real implementations — the single new abstraction this feature introduces, justified by the stub-vs-real clarification.

## Complexity Tracking

> No Constitution Check violations — section intentionally empty.
