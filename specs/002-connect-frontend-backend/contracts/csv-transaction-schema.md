# CSV Transaction Schema Contract

This contract defines the canonical CSV format accepted by transaction import
preview/commit and emitted by transaction export. The file format stays stable
across the move from browser-only storage to backend persistence.

## File-level rules

- Encoding: UTF-8, with or without BOM
- Delimiter: comma (`,`)
- Line endings: LF or CRLF accepted
- One header row is required
- Blank lines are ignored
- Quoting follows standard CSV escaping rules for commas, quotes, and newlines

## Canonical header

```csv
date,ticker,type,quantity,price,fees
```

## Column contract

| Column | Required | Format | Validation |
|--------|----------|--------|------------|
| date | yes | `YYYY-MM-DD` | Real calendar date, not in the future |
| ticker | yes | uppercase symbol | Must match `^[A-Z]{1,5}$` after normalization and exist in supported instruments |
| type | yes | `buy` or `sell` | Case-insensitive on input, normalized to lowercase |
| quantity | yes | decimal | Must parse and be `> 0` |
| price | yes | decimal | Must parse and be `> 0` |
| fees | no | decimal | Defaults to `0` when blank, must be `>= 0` when provided |

## Import workflow contract

1. Upload CSV file for preview.
2. Service validates every row and returns:
   - `validRows`
   - `invalidRows`
   - `headerErrors`
3. Only normalized valid rows are eligible for commit.
4. Commit writes valid rows as persisted transactions.

## Normalization rules

- `ticker` is uppercased
- `type` is lowercased
- Cells are trimmed before validation
- Server-generated transaction IDs are assigned only at commit time, not in the
  CSV itself

## Export contract

- Export always emits the canonical header in the order shown above
- One row is produced per committed transaction
- Output uses normalized ticker/type casing
- Exported files must be accepted by the import preview endpoint without schema
  changes

## Round-trip guarantee

Exporting committed transactions and re-importing the resulting file must
reproduce the same derived holdings and portfolio summary values.
