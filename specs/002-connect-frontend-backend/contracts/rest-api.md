# REST API Contract

This contract defines the backend surface that the frontend will integrate with
for dashboard, watchlists, analysis, and transaction import/export workflows.

Business endpoint base URL: `/api`

## Shared conventions

- Request and response bodies use JSON unless explicitly noted otherwise.
- Timestamps use ISO-8601 strings.
- Dates use `YYYY-MM-DD`.
- Monetary and quantity values are serialized as JSON numbers.
- Error shape:

```json
{
  "code": "validation_error",
  "message": "Ticker is unknown",
  "details": {
    "field": "ticker"
  }
}
```

- Standard error codes:
  - `400` invalid request payload
  - `404` missing resource
  - `409` duplicate name or conflicting mutation
  - `422` semantically invalid import/transaction content

## Dashboard

### `GET /dashboard`

Returns the current portfolio summary and holdings projection.

```json
{
  "summary": {
    "totalMarketValue": 18250.14,
    "totalCostBasis": 15940.00,
    "totalUnrealizedPnL": 2310.14,
    "totalUnrealizedPnLPct": 0.1449,
    "totalDayChange": 215.44,
    "totalDayChangePct": 0.0119
  },
  "holdings": [
    {
      "ticker": "AAPL",
      "name": "Apple Inc.",
      "shares": 16,
      "averageCost": 181.11,
      "costBasis": 2897.76,
      "currentPrice": 198.42,
      "marketValue": 3174.72,
      "unrealizedPnL": 276.96,
      "unrealizedPnLPct": 0.0956,
      "dayChange": 28.80,
      "dayChangePct": 0.0092,
      "weight": 0.1739
    }
  ]
}
```

## Watchlists

### `GET /watchlists`

Returns all watchlists with ordered ticker rows and display metadata needed by
the watchlist UI.

### `POST /watchlists`

Creates a watchlist.

Request:

```json
{
  "name": "Tech Majors"
}
```

### `PATCH /watchlists/{watchlistId}`

Renames a watchlist.

Request:

```json
{
  "name": "Large Cap Tech"
}
```

### `DELETE /watchlists/{watchlistId}`

Deletes a watchlist and its items.

### `POST /watchlists/{watchlistId}/tickers`

Adds a ticker to the end of a watchlist.

Request:

```json
{
  "ticker": "MSFT"
}
```

### `DELETE /watchlists/{watchlistId}/tickers/{ticker}`

Removes a ticker from the watchlist.

### `PUT /watchlists/{watchlistId}/ticker-order`

Replaces the ticker order for a watchlist.

Request:

```json
{
  "tickers": ["AAPL", "MSFT", "NVDA"]
}
```

## Instruments / Analysis

### `GET /instruments/{ticker}`

Returns the analysis payload for a supported ticker, including instrument
metadata, current stats, price history, and position summary when held.

```json
{
  "ticker": {
    "symbol": "AAPL",
    "name": "Apple Inc.",
    "sector": "Technology",
    "exchange": "NASDAQ"
  },
  "stats": {
    "open": 196.2,
    "high": 199.1,
    "low": 195.4,
    "previousClose": 196.6,
    "volume": 41234567,
    "week52High": 205.3,
    "week52Low": 164.8,
    "marketCap": 3045000000000,
    "peRatio": 30.6
  },
  "priceHistory": [
    {
      "date": "2026-04-24",
      "open": 196.2,
      "high": 199.1,
      "low": 195.4,
      "close": 198.4,
      "volume": 41234567
    }
  ],
  "positionSummary": {
    "shares": 16,
    "averageCost": 181.11,
    "marketValue": 3174.72,
    "unrealizedPnL": 276.96
  }
}
```

If the ticker exists but is not held, `positionSummary` is `null`.

## Transactions

### `GET /transactions`

Returns committed transactions in reverse chronological order.

### `DELETE /transactions/{transactionId}`

Deletes a committed transaction and returns the updated dashboard summary.

### `POST /transactions/import/preview`

Accepts CSV file content and returns validation results without persisting rows.

Request:
- `multipart/form-data`
- Part name: `file`

Response:

```json
{
  "validRows": [
    {
      "row": 2,
      "normalized": {
        "date": "2026-01-03",
        "ticker": "AAPL",
        "type": "buy",
        "quantity": 10,
        "price": 188.5,
        "fees": 0
      }
    }
  ],
  "invalidRows": [
    {
      "row": 5,
      "reason": "unknown ticker: ZZZZ",
      "raw": {
        "date": "2026-01-03",
        "ticker": "ZZZZ",
        "type": "buy",
        "quantity": "5",
        "price": "10",
        "fees": "0"
      }
    }
  ],
  "headerErrors": []
}
```

### `POST /transactions/import/commit`

Commits the normalized valid rows from a preview request.

Request:

```json
{
  "rows": [
    {
      "date": "2026-01-03",
      "ticker": "AAPL",
      "type": "buy",
      "quantity": 10,
      "price": 188.5,
      "fees": 0
    }
  ]
}
```

Response:

```json
{
  "importedCount": 1,
  "dashboard": {
    "summary": {
      "totalMarketValue": 18250.14,
      "totalCostBasis": 15940.00,
      "totalUnrealizedPnL": 2310.14,
      "totalUnrealizedPnLPct": 0.1449,
      "totalDayChange": 215.44,
      "totalDayChangePct": 0.0119
    },
    "holdings": []
  }
}
```

### `GET /transactions/export`

Returns the committed transaction history as `text/csv` using the canonical
schema defined in [csv-transaction-schema.md](./csv-transaction-schema.md).

## Operational endpoints

Operational endpoints are outside the `/api` base path.

### `GET /q/health`

Returns backend health for local development verification. Minimal success
shape:

```json
{
  "status": "UP"
}
```
