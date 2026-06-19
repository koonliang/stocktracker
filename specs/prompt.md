# 001-frontend-prototype
This is a frontend and backend project that allows user to track and monitor stocks.
Tech stack to be used:
- Frontend: React Vite
- Backend: Java Quarkus on AWS Lambda

Initial Features:
- Portfolio Dashboard
- Creation of Watchlist
- Import/Export Transaction
- Stock Analysis

For this initial feature, I just need the frontend prototype without backend implementation.
The frontend design must not look like it's vibe-coded. Use the frontend-design skill when creating the prototype.

# 002-connect-frontend-backend
Link the frontend features with the backend. Backend stack will be Java Quarkus with MySQL db.
For local development, include docker compose with frontend/backend app and MySQL db.

# 003-ci-cd-aws
- CI feature: github action for PR creation and merge to main
- CD feature to AWS via terraform: deploy backend to AWS lambda, provision MySQL RDS, frontend to S3 bucket. 
Frontend flow: Cloudflare CDN -> S3
Backend flow: Lambda -> MySQL

# 004-selenium-regression-tests
- for regression testing, i want to use selenium with java for automated web testing
- this should be triggered as part of ci pipeline (run in headless mode)

# 005-user-authentication
- for dev mode, user id and password will be sufficient
- for production mode in aws, suggest if it should integrate with Cognito or use DynamoDB (include a cost analysis)
- include an account sign up feature and reset password

# 006-live-data-analytics
### 1. Live Market Data Integration
Replace the static seed price snapshots with a real quote provider (e.g., Alpha
Vantage, Finnhub, or Yahoo Finance via a backend proxy).
- Backend: scheduled refresh job + cache table; expose `/quotes?symbols=...`.
- Frontend: poll or SSE stream so dashboard tiles and watchlist rows tick.
- Includes rate-limit handling and a "last updated" indicator per quote.
### 2. Dividends, Splits & Cash Transactions
Extend the transaction model beyond buy/sell.
- New types: `dividend`, `split`, `deposit`, `withdrawal`, `fee`.
- Update cost-basis and P&L calculations to handle splits retroactively.
- Update CSV schema (with backwards-compatible parsing of the v1 schema).
### 3. Realized vs. Unrealized P&L Reporting
Today the dashboard only shows unrealized P&L. Add:
- Realized P&L per closed lot (FIFO by default, optional LIFO/specific-lot).
- A "Performance" page with cumulative return chart, time-weighted return,
  and a per-holding contribution breakdown.
### 4. Price Alerts & Notifications
Let users set thresholds on a ticker (price >, price <, % change).
- Backend: alert evaluation on each quote refresh; persist in MySQL.
- Delivery: in-app toast first; email/push notifications later.

# 007-alert-currency-dashboard
- notification dialog with the list of alerts triggered
- Tag each transaction with a currency.
- User can choose to view dashboard and performance using user-chosen base currency

# 008-
- On the Stock Analysis view, render headlines from a news API. Filter by source and recency; cache aggressively to control cost.
- On the Stock Analysis view, show a comprehensive analysis of the stock with the following sections (for this iteration, the data can be mocked. next iteration will implement the data pipeline)
    - General Info
        - Industry
        - Sector
        - Went public on
        - Method of going public
        - Full time employees 
    - CEO Info 
        - Name and Picture of CEO
        - Compensation Summary
            - Salary
            - Stock Awards
            - Incentive
            - Others
    - Upcoming Earnings
    - Competition Analysis
    - Income Statement
    - Sector Peers and Price