# Contract: Dashboard and Performance Currency Conversion

All endpoints require a valid bearer JWT and use the current user's base currency.

## Conversion Metadata

Converted monetary values include conversion status when stale or unavailable.

```json
{
  "amount": 1234.56,
  "currency": "SGD",
  "conversion": {
    "baseCurrency": "USD",
    "amountBase": 915.32,
    "fxDate": "2026-06-14",
    "fxStatus": "stale"
  }
}
```

`fxStatus` values:
- `current`: exact-date FX rate was used.
- `stale`: latest prior FX rate was used because exact-date FX was unavailable.
- `unavailable`: no prior FX rate exists for the pair; caller shows unavailable state.

## Date Basis

- Transaction-based values use transaction-date FX.
- Current holding values use the dashboard or performance view valuation date.
- If exact-date FX is unavailable, latest prior FX is used and marked `stale`.

## Dashboard Response Additions

Dashboard summary and holding rows expose:
- `baseCurrency`
- native amount/currency where relevant
- converted base amount
- `fxStatus` and `fxDate` when converted

Dashboard totals must not silently include unavailable conversions. If a component value is unavailable, the total response includes a warning item identifying the affected symbol or transaction.

## Performance Response Additions

Performance summaries expose:
- base currency for all top-level monetary values
- native and base values for realized lot rows and income rows
- FX date/status for transaction-based conversions
- valuation date/status for current holding contribution values

Percentage returns must remain consistent with the converted monetary values used in the same response.
