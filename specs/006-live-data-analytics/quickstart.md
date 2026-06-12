# Quickstart: Live Market Data & Portfolio Analytics

How to run, exercise, and verify this feature locally. Assumes the feature-005
auth dev setup works (sign in as `seed@stocktracker.local` / `DevPass123!`).

## Prerequisites

- Docker + docker-compose (MySQL), JDK 21, Node 18+.
- Defaults use the **`stub`** market-data and **`stub`** FX providers — no API
  key, no network. The stub seed includes a non-US symbol (an SGX `.SI` example)
  and a fixed `USD↔SGD` rate so global + multi-currency paths work offline.

## Run

```bash
# 1. Backend (applies Flyway V4 on start; stub providers; quote refresh every 60s)
cd backend && ./mvnw quarkus:dev

# 2. Frontend
cd frontend && npm install && npm run dev
```

Sign in, then:
- **Dashboard / Watchlist** — prices show a "last updated" time and tick on the
  ~30s poll; each holding shows base-currency value with native value alongside.
  Stop the backend → rows flip to **stale** (not error).
- **Symbol search** — search "DBS" or `D05.SI`, add it; its SGD price appears
  immediately (no wait for the next cycle) and contributes to the base-currency
  total.
- **Base currency** — switch base currency (e.g. USD → SGD); totals re-express.
- **Transactions** — add a `dividend`, a `2`-ratio `split`, a `deposit` (with a
  currency); confirm split adjusts shares/cost basis and cash updates.
- **Performance** (`/performance`) — pick a window, toggle FIFO/LIFO; see realized
  P&L per closed lot (native + base), the cumulative-return chart, TWR, and
  contributions, all in base currency.
- **Alerts** (`/alerts`) — create `price_above` on a held symbol below current
  price; within a refresh cycle an in-app toast appears once.

## Enable real providers (prod-like)

```properties
# application.properties (or env STOCKTRACKER_MARKETDATA_PROVIDER / STOCKTRACKER_FX_PROVIDER)
stocktracker.marketdata.provider=yahoo        # Yahoo Finance public endpoints — no API key, global incl. SGX .SI
stocktracker.fx.provider=frankfurter          # api.frankfurter.app, ECB daily rates — no API key
```
Neither provider needs a key. The refresh job runs continuously; per-symbol
freshness is judged by last successful fetch, not by any single market's hours.
Yahoo's endpoints are unofficial — accept the reliability risk or fall back to
`stub`.

## Verify the gates (Constitution I–III)

```bash
# Backend: unit + integration (quote refresh, global symbol add + immediate fetch,
# FIFO/LIFO realized P&L w/ split, currency conversion, CSV v1/v2, alert fire-once)
cd backend && ./mvnw -B verify

# Frontend: lint + typecheck + test + build
cd frontend && npm run verify

# e2e (stub providers; live-quote, global-symbol, performance, alert journeys)
cd e2e && ./mvnw -B test
```

## Acceptance mapping (smoke)

| Spec item | How to check |
|-----------|--------------|
| SC-001/002, US1 | Dashboard price + "last updated" ticks within 60s |
| SC-003, US1 | Backend down → stale badge, no blank/error |
| SC-010/011 | Search + add a symbol → live price within seconds; bad symbol rejected |
| SC-012, US1 | Add `D05.SI` → SGD price shows |
| SC-013 | Mixed SGD+USD portfolio total in base ccy reconciles to native×FX |
| SC-004, US2 | Existing v1 CSV imports 100% |
| SC-005, US2 | Record split → total cost basis unchanged |
| SC-006, US3 | FIFO vs LIFO toggle changes realized P&L |
| SC-007, US3 | Contribution rows sum to total return |
| SC-008, US4 | Alert crossing → exactly one toast |
