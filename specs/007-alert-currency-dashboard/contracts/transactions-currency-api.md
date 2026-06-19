# Contract: Transaction Currency Behavior

This contract refines the transaction API/import/export behavior from feature 006.

## Transaction DTO Fields

Every transaction response that carries monetary values includes:

```json
{
  "id": "t1",
  "type": "buy",
  "symbol": "AAPL",
  "tradeDate": "2026-06-17",
  "quantity": 10,
  "price": 185.00,
  "amount": null,
  "fees": 1.00,
  "currency": "USD",
  "currencySource": "instrument"
}
```

`currencySource` values:
- `instrument`: inferred/defaulted from instrument currency.
- `manual`: selected directly by the user.
- `import`: supplied by imported CSV data.
- `user_base_backfill`: assigned to older cash-only records from user base currency.

## Create or Update Transaction

Rules:
- Buy/sell/dividend rows default to the instrument currency when omitted.
- If a security row supplies currency, it must match the instrument currency.
- Deposit, withdrawal, and cash-only fee rows require a supported currency.
- Split rows require no currency unless a monetary amount or fee exists.

Validation errors use row/field-level messages in existing transaction forms and import preview.

## Legacy Backfill

Before converted dashboard/performance views include legacy transactions:
- Instrument-linked rows with missing currency are assigned `instrument.currency`.
- Cash-only rows with missing currency are assigned the user's current `baseCurrency`.
- Backfilled rows set `currencySource` and `currencyBackfilledAt`.

## CSV Import/Export

The v2 CSV schema includes a `currency` column. Import behavior:
- Valid cash-only v2 rows must include currency.
- Valid security v2 rows may omit currency and default to instrument currency.
- Legacy v1 buy/sell files remain accepted and infer instrument currency.

Export behavior:
- Always writes the v2 header including `currency`.
- Exports the resolved transaction currency for every monetary transaction.
