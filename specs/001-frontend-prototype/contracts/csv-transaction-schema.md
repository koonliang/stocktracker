# CSV Transaction Schema Contract

This contract defines the single canonical CSV format accepted by the Import
feature (FR-014, FR-015) and produced by the Export feature (FR-017). It is the
authoritative spec for round-trip equivalence (SC-008).

## File-level rules

- Encoding: UTF-8 (with or without BOM).
- Line endings: LF or CRLF, both accepted.
- Delimiter: comma (`,`).
- Quoting: RFC 4180 — fields containing commas, quotes, or newlines MUST be
  double-quoted; embedded double quotes are escaped as `""`.
- A single header row is REQUIRED and MUST appear as the first non-blank line.
- Blank lines are ignored.
- Column order in the header is fixed and case-insensitive on input; the
  exporter always emits the canonical casing shown below.

## Canonical header

```csv
date,ticker,type,quantity,price,fees
```

## Column specification

| Column   | Required | Type / Format                          | Validation                                                                 |
|----------|----------|----------------------------------------|----------------------------------------------------------------------------|
| date     | yes      | `YYYY-MM-DD` (ISO-8601)                | Parseable as calendar date; not in the future.                             |
| ticker   | yes      | string                                 | Matches `^[A-Z]{1,5}$` after uppercasing; MUST exist in seeded catalog.    |
| type     | yes      | `buy` \| `sell` (case-insensitive)     | Rejected if any other value.                                               |
| quantity | yes      | decimal                                | Parses as number; strictly > 0; up to 6 decimal places.                    |
| price    | yes      | decimal                                | Parses as number; strictly > 0; up to 4 decimal places.                    |
| fees     | no       | decimal                                | If column omitted entirely, defaults to 0. If column present but cell blank, defaults to 0. If present, >= 0. |

Numeric cells MUST NOT include currency symbols, thousands separators, or
parentheses for negatives. A leading `-` is the only accepted negative
indicator — and it is invalid for `quantity` and `price`.

## Normalization on import

- `ticker` is uppercased.
- `type` is lowercased.
- Whitespace is trimmed from every cell before validation.
- Each row that passes validation receives a client-generated `id` (UUID v4)
  not present in the CSV.

## Import validation outcomes (preview table)

For each parsed row, the importer produces one of:

- `valid` — all columns pass validation.
- `invalid: <reason>` — at least one validation rule failed; the row is shown
  in the preview with the specific reason and is excluded from commit.

Example reasons (non-exhaustive): `missing required column: date`,
`unknown ticker: ZZZZ`, `type must be buy or sell`, `quantity must be > 0`,
`date is in the future`, `malformed number in price`.

## Export format

Exports always emit:

- Canonical header in the order above, with canonical casing.
- One row per committed transaction, using the same normalization rules as
  import (uppercased ticker, lowercased type, ISO date, unformatted numbers).
- LF line endings.
- UTF-8, no BOM.

## Round-trip guarantee (SC-008)

`export(committed) → import → commit` MUST produce a portfolio byte-identical
to the pre-export state in terms of derived Holdings and Portfolio figures.

## Example

```csv
date,ticker,type,quantity,price,fees
2024-02-14,AAPL,buy,10,185.2500,0
2024-05-03,AAPL,buy,5,176.1000,0
2024-08-22,MSFT,buy,8,415.0000,0
2025-01-10,AAPL,sell,4,228.5000,1.00
```
