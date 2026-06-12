# Contract: Instrument Search & Add-on-Demand API

Requires a valid bearer JWT. Lets users find any provider-recognized symbol
(including non-US, e.g. SGX `.SI`) and add it to their universe with an immediate
quote (FR-026/027, SC-010/012).

## GET /api/instruments/search

Search symbols by company name or partial/exact ticker.

**Query params**: `q` (required, min 1 char).

**200 Response**
```json
{
  "results": [
    { "symbol": "D05.SI", "name": "DBS Group Holdings Ltd", "exchange": "SGX", "currency": "SGD" },
    { "symbol": "AAPL",   "name": "Apple Inc",              "exchange": "NASDAQ", "currency": "USD" }
  ]
}
```
- Proxies `MarketDataProvider.searchSymbols`. Empty result set → `results: []`
  (no error) for a query with no matches (edge case).

## POST /api/instruments

Add a searched symbol to the tracked universe. Idempotent if it already exists.

**Request**
```json
{ "symbol": "D05.SI" }
```

**Behavior**
1. If the instrument row does not exist, validate the symbol via the provider; if
   unrecognized → **422** with a clear message and **no** row created (edge case).
2. Create the `instrument` row capturing `name`, `exchange`, `currency` from the
   provider.
3. **Immediately** fetch its current quote (upsert `instrument_quote`) and trigger
   `HistoricalBackfillService` so price/value appear at once (FR-027/SC-010).
4. If that immediate fetch fails, the instrument is still created and returned
   with `stale: true` (edge case) — the next refresh cycle will fill it.

**201 Response**
```json
{
  "symbol": "D05.SI",
  "name": "DBS Group Holdings Ltd",
  "exchange": "SGX",
  "currency": "SGD",
  "quote": { "price": 45.10, "asOf": "2026-06-12T08:00:00Z", "stale": false }
}
```

The frontend then includes the new symbol in its quote-polling set and it joins
the recurring server-side refresh.
