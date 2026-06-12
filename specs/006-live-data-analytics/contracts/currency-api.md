# Contract: Base Currency Setting

Requires a valid bearer JWT; scoped to the current user (FR-031). Lets the user
choose the base reporting currency used for all combined totals/P&L/performance.

## GET /api/me/base-currency

**200 Response**
```json
{ "baseCurrency": "USD", "supported": ["USD", "SGD", "EUR", "GBP", "JPY", "..."] }
```
- `supported` lists currencies the FX provider can convert (derived from currently
  reachable pairs). Default base currency is `USD`.

## PUT /api/me/base-currency

**Request**
```json
{ "baseCurrency": "SGD" }
```
- Validates against `supported`; **422** if unsupported.
- Persists `app_user.base_currency`. Subsequent dashboard/performance responses
  report totals in the new base currency (FR-031). **200** with the updated value.

## How conversion surfaces elsewhere

- Dashboard `Holding` rows expose both native (`nativePrice`, `nativeMarketValue`,
  `currency`) and base-converted (`currentPrice`, `marketValue`) values (FR-032).
- `GET /api/performance` and the portfolio summary include `baseCurrency` and
  report all monetary figures in it.
- Conversion is performed server-side by `CurrencyService` using `fx_rate`
  (daily). FX is never computed in the frontend, ensuring consistent totals.
