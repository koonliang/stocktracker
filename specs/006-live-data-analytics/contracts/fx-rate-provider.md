# Contract: FxRateProvider SPI & FX cache

Internal backend abstraction for foreign-exchange rates (FR-030), decoupled from
the quote provider so either can change independently. Selected by
`stocktracker.fx.provider` (env `STOCKTRACKER_FX_PROVIDER`), values `stub`
(default — dev + all tests) and `frankfurter` (prod).

## Interface

```java
public interface FxRateProvider {
    /** Daily rates for each quote currency against `base`, for `onDate`. */
    List<ProviderFxRate> dailyRates(String base, Collection<String> quotes,
                                    LocalDate onDate);
}

record ProviderFxRate(String base, String quote, LocalDate date,
                      BigDecimal rate) {} // units of quote per 1 base
```

## `stub` implementation (dev + all tests)

- Returns deterministic, fixed rates for the currency pairs used by seeds (at
  least `USD↔SGD`) so multi-currency conversion is reproducible offline.

## `frankfurter` implementation (prod)

- `@RegisterRestClient` against Frankfurter (`api.frankfurter.app`), ECB daily
  reference rates. **No API key.**
  - latest → `GET /latest?base={base}&symbols={quotes}`.
  - historical/backfill → `GET /{date}?base={base}&symbols={quotes}`.
- Daily granularity is sufficient (FR-030); no intraday FX.
- Errors are caught by `FxRefreshJob`; the last known rate is retained.

## `FxRefreshJob` (internal)

- `@Scheduled` daily: for the set of currencies in use (distinct instrument
  currencies + every user `base_currency`), fetch the day's rates and upsert
  `fx_rate`.
- On failure, prior rows remain; `CurrencyService` serves the most recent rate
  marked `stale` (FX-unavailable edge case).

## `CurrencyService`

```java
BigDecimal convert(BigDecimal amount, String from, String to, LocalDate onDate);
```
- Returns `amount` unchanged when `from == to`.
- Looks up `fx_rate` for `onDate` (or the most recent prior date); cross-converts
  via the base if a direct pair is absent.
- Used by `PortfolioService` and `PerformanceService` to express combined totals,
  P&L, and the return series in the user's base currency (FR-031), while native
  values remain available for per-holding display (FR-032).
