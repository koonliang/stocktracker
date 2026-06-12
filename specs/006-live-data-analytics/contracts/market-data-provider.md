# Contract: MarketDataProvider SPI

Internal backend abstraction isolating the external quote source. Selected by
`stocktracker.marketdata.provider` (env `STOCKTRACKER_MARKETDATA_PROVIDER`),
values `stub` (default — dev + all tests) and `yahoo` (prod). Mirrors feature
005's mode-agnostic design so tests stay hermetic.

## Interface

```java
public interface MarketDataProvider {
    /** Latest quote per symbol, batched. Missing/unknown symbols omitted. */
    List<ProviderQuote> latestQuotes(Collection<String> symbols);

    /** Daily closing prices for a symbol from `from` (inclusive) to today. */
    List<ProviderDailyBar> dailyHistory(String symbol, LocalDate from);

    /** Search symbols by company name or partial/exact ticker (FR-026). */
    List<ProviderSymbol> searchSymbols(String query);
}

record ProviderQuote(String symbol, BigDecimal price,
                     BigDecimal previousClose, Instant asOf) {}
record ProviderDailyBar(String symbol, LocalDate date, BigDecimal close) {}
record ProviderSymbol(String symbol, String name, String exchange,
                      String currency) {}
```

## `stub` implementation (dev + all tests)

- Sources base prices from seeded `instrument_price_bar` / `instrument_stat`.
- Produces deterministic intra-day movement (a bounded function of symbol + a
  test-controllable clock) so quotes "move" reproducibly without any network call
  — required for deterministic tests (Constitution I) and for driving the e2e
  alert journey across a threshold.
- Seed includes **at least one non-US symbol** (e.g. an SGX `.SI` ticker in `SGD`)
  so global + multi-currency behavior is covered offline (SC-012).
- `searchSymbols` matches against the seeded universe (US + the `.SI` example).
- `dailyHistory` returns the seeded daily closes.

## `yahoo` implementation (prod)

- `@RegisterRestClient` against Yahoo Finance's public JSON endpoints (same
  endpoints the `yfinance`/`yahooquery` Python libraries wrap — called directly
  from Java, no Python dependency):
  - `latestQuotes` → `GET /v7/finance/quote?symbols=A,B,C` (batch).
  - `dailyHistory` → `GET /v8/finance/chart/{symbol}?interval=1d&range=…`.
  - `searchSymbols` → `GET /v1/finance/search?q=…` (returns name, ticker,
    exchange, currency).
- **No API key.** Free and global — covers SGX `.SI` and other exchanges (FR-028),
  satisfying the global-coverage requirement without a paid plan (Finnhub's free
  tier, US-only, was rejected for this reason).
- **Unofficial-endpoint risk** (accepted for a personal tracker): send a sane
  `User-Agent`, batch symbols per call, and never throw into a request path —
  transient/rate-limit errors are caught in `QuoteRefreshJob`, the prior cached
  value is retained (FR-006), and the next cycle retries.

## Staleness & degradation rules (FR-006, global-market edge case)

- Read endpoints never invoke the provider; they read the `instrument_quote` cache.
- A quote is **stale** when our `fetched_at` is older than N refresh intervals
  (our fetch is failing) — **not** because a market is closed. A closed-exchange
  quote with an old `as_of` but a recent `fetched_at` is fresh.
- The scheduled job tolerates partial batch failure: fetched symbols update; the
  rest keep their last value and flip to `stale` once `fetched_at` ages out.
- No provider error is ever returned to the user as a blank or error price.

## Market hours

The refresh job runs continuously at the 60s cadence regardless of any single
market's hours (FR-028). An optional `ExchangeCalendar` may skip fetching symbols
on a known-closed exchange as a rate-limit optimization only; it never affects
staleness.
