# Implementation Plan: Alert Notifications and Currency Views

**Branch**: `007-alert-currency-dashboard` | **Date**: 2026-06-17 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-alert-currency-dashboard/spec.md`

## Summary

Complete and tighten the user-facing alert notification and multi-currency behavior introduced by the live-data analytics work: add a reviewable notification dialog mockup and implementation target, expose triggered-alert history with read/delete behavior, enforce one notification per threshold crossing with clear re-arm rules, require transaction currency including import/export/backfill behavior, and make dashboard/performance base-currency conversion deterministic through transaction-date and valuation-date FX rules.

Technical approach: reuse the existing Quarkus + React structure and the feature-006 live quote, alert, notification, FX, transaction, and base-currency foundations. Add a backend notification history surface and deletion semantics, extend transaction migration/import/export validation for explicit currency backfill rules, make conversion metadata visible in dashboard/performance DTOs, and build a frontend notification dialog using existing dialog/button/table/toast patterns. Automated tests use deterministic stub quotes and FX rates.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.6 / React 18.3 (frontend)  
**Primary Dependencies**: Quarkus 3.15.2, Hibernate ORM Panache, Flyway, MySQL JDBC, scheduler, REST/Jackson DTOs; React Router 6, Zustand, React Hook Form + Zod, Recharts, Papaparse, lucide-react, existing UI primitives  
**Storage**: MySQL 8.4 via Flyway migrations; extends feature-006 `alert`, `notification`, `portfolio_transaction`, `fx_rate`, and user base-currency data  
**Testing**: Backend JUnit 5 + QuarkusTest + REST-Assured + Testcontainers MySQL; Frontend Vitest + Testing Library + MSW + axe checks where applicable; e2e Selenium Java for dialog and base-currency flows  
**Target Platform**: AWS Lambda backend behind API Gateway + RDS MySQL; frontend S3 + CloudFront; local docker-compose  
**Project Type**: web application (Quarkus backend + React frontend)  
**Performance Goals**: notification dialog opens and identifies triggered alerts in under 10 seconds; base-currency switch updates dashboard/performance in under 30 seconds; notification unread counts and mark-read actions complete within normal interactive web latency  
**Constraints**: build on feature-006 provider seams and stub fixtures; no email/SMS/push notification delivery; all user-owned data remains scoped to authenticated user; exact-date FX miss uses latest prior rate and stale indicator  
**Scale/Scope**: personal tracker accounts; tens to low hundreds of tracked symbols per account; notification history retained until user deletion or alert deletion

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment | Status |
|-----------|------------|--------|
| I. Test Verification (NON-NEGOTIABLE) | Plan includes backend integration tests for notification crossing/re-arm/history, transaction currency backfill/import/export, and FX date/stale conversion; frontend tests for dialog, empty state, mark-read, currency controls, and stale indicators. | PASS |
| II. Lint & Style Compliance (NON-NEGOTIABLE) | Uses existing backend build checks and frontend `npm run verify`; no new lint configuration. | PASS |
| III. Compilation Integrity (NON-NEGOTIABLE) | Backend `./mvnw -B verify`, frontend `npm run typecheck`/`npm run build`, and e2e compile/test remain required before completion. | PASS |
| IV. Simplicity & YAGNI | Reuses feature-006 tables/services/routes; adds only the UI dialog and contract refinements required by the clarified spec. Email/push and arbitrary retention windows remain out of scope. | PASS |
| V. Specification-Driven Development | Plan traces to specs/007 and its clarification log; scope changes must update spec before implementation. | PASS |

Post-design re-check: Phase 1 artifacts preserve the same boundaries; no Constitution violations introduced.

## Project Structure

### Documentation (this feature)

```text
specs/007-alert-currency-dashboard/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── notifications-api.md
│   ├── transactions-currency-api.md
│   ├── dashboard-performance-currency.md
│   └── frontend-dialog.md
└── tasks.md              # generated later by /speckit.tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/stocktracker/
├── api/                  # extend NotificationsResource, TransactionsResource, Settings/Performance/Dashboard DTO surfaces
├── domain/               # refine Alert, Notification, PortfolioTransaction, UserSetting/base currency model
├── dto/                  # notification dialog/history, transaction currency, conversion-status DTOs
├── persistence/          # notification, alert, transaction, FX repositories
├── scheduler/            # alert evaluation remains quote-refresh driven
├── service/              # AlertEvaluationService, NotificationService, TransactionImport/Export, CurrencyConversionService
└── resources/db/migration/ # add V5 for 007-only schema/data migration if V4 from feature 006 is already applied

backend/src/test/java/com/stocktracker/
├── api/                  # NotificationsResourceTest, TransactionsResource currency tests, dashboard/performance conversion tests
└── service/              # alert crossing/re-arm, FX date selection, transaction backfill tests

frontend/src/
├── api/                  # notificationsApi, transactionsApi, dashboardApi, performanceApi, settingsApi type refinements
├── components/layout/    # notification entry point in app shell/top bar
├── components/ui/        # reuse Dialog/Button/Badge/Table primitives
├── features/alerts/      # NotificationDialog, alert history/read/delete UI
├── features/transactions/# currency field/import/export validation surfaces
├── features/dashboard/   # conversion status/stale indicators
├── routes/               # AlertsRoute integration
├── stores/               # notificationsStore / base-currency refresh interactions
└── test/                 # MSW handlers and component tests
```

**Structure Decision**: Existing web layout is reused. This feature extends the live-data modules from `006-live-data-analytics` instead of introducing a separate project or provider abstraction. Contracts are documented as REST/UI contracts because the application exposes backend APIs and frontend interaction surfaces.

## Complexity Tracking

> No Constitution Check violations; section intentionally empty.
