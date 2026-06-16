# Contract: Quotes API

All endpoints require a valid bearer JWT. Quote data is shared market data, but
the frontend only requests symbols the authenticated user tracks.

## GET /api/quotes

Return the latest cached quote for one or more symbols. Reads only from the
`instrument_quote` cache — never calls the external provider inline (FR-002/003).

**Query params**
- `symbols` (required): comma-separated symbols, e.g. `AAPL,D05.SI`.

**200 Response**
```json
{
  "quotes": [
    {
      "symbol": "D05.SI",
      "price": 45.10,
      "currency": "SGD",
      "changeAmount": 0.20,
      "changePct": 0.4454,
      "previousClose": 44.90,
      "asOf": "2026-06-12T08:00:00Z",
      "fetchedAt": "2026-06-12T15:42:00Z",
      "source": "stub",
      "stale": false
    }
  ]
}
```

**Behavior**
- `price` and `currency` are the instrument's **native** values.
- `stale` is derived from `fetchedAt` age (provider not responding), **not** from
  whether the symbol's exchange is currently closed (global-market edge case).
- A symbol with no cached quote yet falls back to the latest
  `instrument_price_bar` close with `stale: true` (FR-006). A symbol unknown to
  the provider returns `price: null, stale: true` rather than an error.
- Always returns 200 with whatever is cached; provider/rate-limit failures are
  absorbed by the refresh job.

## Dashboard / Watchlist integration

`GET /api/dashboard` (existing) and watchlist reads now source current price from
`instrument_quote` (fallback price bar). Each `Holding` / watchlist row gains:
- `currency`, `nativePrice`, `nativeMarketValue` — the instrument's own currency.
- `marketValue`, `currentPrice`, `dayChange*` — **converted to the user's base
  currency** via `CurrencyService` (FR-031/032).
- `asOf`, `fetchedAt`, `stale` — for the "last updated" indicator (FR-005).

Portfolio summary totals (`totalMarketValue`, `totalUnrealizedPnL`, …) are in the
user's base currency; the response includes `baseCurrency`.

## Refresh job (internal, not an endpoint)

`QuoteRefreshJob` `@Scheduled(every="60s")`:
1. Tracked symbols = distinct symbols in any `portfolio_transaction` (non-cash) ∪
   any `watchlist_item`.
2. Batch-call `MarketDataProvider.latestQuotes(symbols)` (continuously, not gated
   on US hours); on partial failure keep prior cached values, flip untouched
   symbols to `stale` once `fetched_at` ages out.
3. Upsert `instrument_quote` (set `fetched_at=now` on success).
4. Invoke `AlertService.evaluate(...)` for each refreshed quote.

Config: `stocktracker.marketdata.provider=stub|yahoo` (default `stub`);
`stocktracker.marketdata.refresh-interval=60s`.
