# Upcoming Feature Suggestions

Forward-looking ideas that build on the shipped Portfolio Dashboard, Watchlist,
Stock Analysis, and CSV Import/Export features (specs 001 + 002). Ordered
roughly by user value vs. implementation cost.

## Near-term (next 1–2 iterations)

### 1. Live Market Data Integration
Replace the static seed price snapshots with a real quote provider (e.g., Alpha
Vantage, Finnhub, or Yahoo Finance via a backend proxy).
- Backend: scheduled refresh job + cache table; expose `/quotes?symbols=...`.
- Frontend: poll or SSE stream so dashboard tiles and watchlist rows tick.
- Includes rate-limit handling and a "last updated" indicator per quote.

### 2. User Accounts & Authentication
Move from single-implicit-user to per-user portfolios.
- Email/password + OAuth (Google) via Quarkus OIDC.
- Migrate localStorage state to server-side per user; add migration-on-login.
- Enables multi-device access and unlocks all collaboration features below.

### 3. Dividends, Splits & Cash Transactions
Extend the transaction model beyond buy/sell.
- New types: `dividend`, `split`, `deposit`, `withdrawal`, `fee`.
- Update cost-basis and P&L calculations to handle splits retroactively.
- Update CSV schema (with backwards-compatible parsing of the v1 schema).

### 4. Realized vs. Unrealized P&L Reporting
Today the dashboard only shows unrealized P&L. Add:
- Realized P&L per closed lot (FIFO by default, optional LIFO/specific-lot).
- A "Performance" page with cumulative return chart, time-weighted return,
  and a per-holding contribution breakdown.

### 5. Price Alerts & Notifications
Let users set thresholds on a ticker (price >, price <, % change).
- Backend: alert evaluation on each quote refresh; persist in MySQL.
- Delivery: in-app toast first; email/push notifications later.

## Mid-term

### 6. Broker CSV Import Adapters
Adapter layer over the canonical CSV schema for popular brokers (Fidelity,
Schwab, Interactive Brokers, Robinhood, Tiger, Moomoo). User picks the broker;
adapter normalizes columns before the existing preview/validation flow.

### 7. Multi-Currency Support
- Tag each transaction with a currency.
- FX rate table + daily snapshot via a free FX feed.
- Dashboard reports in a user-chosen base currency with an FX-impact line item.

### 8. Tax Lot Tracking & Year-End Reports
- Per-lot tracking with acquisition dates.
- Generate IRS Form 8949 / Singapore IRAS-style summary CSV/PDF.
- Wash-sale flagging (US) as an optional toggle.

### 9. Benchmark Comparison
On the Performance page, overlay user's portfolio return vs. a chosen
benchmark (S&P 500, STI, custom ticker). Time-weighted, normalized to 100 at
start of selected window.

### 10. News Feed per Ticker
On the Stock Analysis view, render headlines from a news API (e.g., Finnhub,
Marketaux). Filter by source and recency; cache aggressively to control cost.

## Longer-term

### 11. Mobile-First PWA / Native Wrapper
Make the app installable, offline-capable for the dashboard, and push-capable
for alerts. Wrap with Capacitor for App Store/Play presence if needed.

### 12. Shared / Read-Only Portfolio Links
Generate a signed read-only URL for a portfolio snapshot — useful for
showing returns without exposing transaction detail. Configurable visibility
per field (hide quantities, show % only, etc.).

### 13. Options & Crypto Asset Classes
Extend the instrument model: option contracts (with strike/expiry/Greeks),
crypto (24/7 pricing, fractional units, on-chain wallet import as future
extension).

### 14. AI Insights Panel
Use an LLM (Claude) to summarize: "Why did MY portfolio move today?" Inputs
are the day's holdings movements + headline news per ticker. Emphasize
attribution to specific positions, not generic market commentary.

### 15. Backtesting & What-If Scenarios
Let a user define a hypothetical allocation or rule (e.g., "rebalance to
60/40 quarterly since 2020") and see the resulting equity curve vs. their
actual portfolio.

## Cross-cutting / Platform

- **Observability**: structured logging, request tracing, Grafana dashboard
  for quote-fetch latency and error rates.
- **CI/CD**: GitHub Actions pipeline — backend tests, frontend Vitest +
  Playwright, container build, deploy to AWS Lambda + S3/CloudFront.
- **Test coverage**: contract tests between frontend and Quarkus endpoints
  (Pact or OpenAPI-driven) so the two specs stay in sync.
- **Theming**: persist user theme preference server-side; add a high-contrast
  theme to push WCAG AA → AAA on key views.
