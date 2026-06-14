# Contract: CSV Schema v2 (with v1 backwards compatibility)

Extends the existing transaction CSV to carry the new transaction types while
guaranteeing every valid v1 file still imports unchanged (FR-011/012, SC-004).

## v1 schema (unchanged, still accepted)

```
date,ticker,type,quantity,price,fees
2024-01-15,AAPL,buy,10,185.00,1.00
2024-03-02,AAPL,sell,4,190.00,1.00
```
- `type ∈ {buy, sell}`; `fees` optional (defaults 0).

## v2 schema

Adds trailing optional `amount` and `currency` columns:
```
date,ticker,type,quantity,price,fees,amount,currency
2024-01-15,AAPL,buy,10,185.00,1.00,,
2024-05-10,AAPL,dividend,,,,12.50,
2024-06-01,AAPL,split,2,,,,
2024-02-01,,deposit,,,,1000.00,USD
2024-07-01,D05.SI,buy,100,38.00,2.00,,SGD
```

Column usage by `type` (blank where not applicable):

| type | ticker | quantity | price | fees | amount | currency |
|------|--------|----------|-------|------|--------|----------|
| buy / sell | required | shares | per-share | optional | — | optional (defaults to instrument ccy) |
| dividend | required | — | — | — | cash (required) | optional (defaults to instrument ccy) |
| split | required | ratio (new per old) | — | — | — | — |
| deposit / withdrawal / fee | blank | — | — | — | cash (required) | required |

- `currency` for a security row, if provided, must match the instrument's
  currency; if blank it defaults to it. Cash rows require an explicit `currency`.

## Version detection (import)

The importer inspects the header and rows:
- **v1** when there is no `amount`/`currency` column **and** every `type` is
  `buy`/`sell` → parsed exactly as today (`amount`/`currency` null → defaults).
- **v2** when an `amount` or `currency` column is present **or** any row uses a
  new type → parsed with the v2 rules above.

Because v2 is a strict superset (extra optional trailing column), a v1 file is a
valid v2 file; no separate parser or upload path is needed. The detected version
is reported in the import preview response so the UI can label it (FR-012).

## Export

Export always writes the v2 header
`date,ticker,type,quantity,price,fees,amount,currency`. A portfolio containing
only buy/sell rows in one currency round-trips with empty `amount`/`currency`
columns and re-imports cleanly under either detection branch.

## Validation (preview + commit)

Reuses `TransactionValidationService` per-row rules from data-model.md:
required-symbol vs cash-type rules, positive quantity/amount, allowed type set.
Invalid rows are surfaced in the existing preview flow (valid/invalid split) with
no change to that UX.
