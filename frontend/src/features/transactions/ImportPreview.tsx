import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/Table';
import { Button } from '@/components/ui/Button';
import type { TransactionImportPreviewResponse } from '@/lib/types';
import { cn } from '@/lib/cn';

type Props = {
  result: TransactionImportPreviewResponse;
  pending?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ImportPreview({ result, pending = false, onConfirm, onCancel }: Props) {
  const validCount = result.validRows.length;
  const invalidCount = result.invalidRows.length;

  if (result.headerErrors.length > 0) {
    return (
      <div className="rounded-md border border-negative/30 bg-negative/5 p-4">
        <div className="font-medium text-negative">Cannot read this file</div>
        <ul className="mt-2 list-disc pl-5 text-small text-text-muted">
          {result.headerErrors.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
        <div className="mt-3">
          <Button variant="ghost" size="sm" onClick={onCancel}>
            Close
          </Button>
        </div>
      </div>
    );
  }

  // Build a unified ordered list: rows in the file order, regardless of valid/invalid.
  type Item = {
    row: number;
    status: 'valid' | 'invalid';
    reason?: string;
    raw: Record<string, string>;
  };
  const items: Item[] = [];
  result.validRows.forEach((row) => {
    items.push({
      row: row.row,
      status: 'valid',
      raw: {
        date: row.normalized.date,
        ticker: row.normalized.ticker,
        type: row.normalized.type,
        quantity: String(row.normalized.quantity),
        price: String(row.normalized.price),
        fees: String(row.normalized.fees),
      },
    });
  });
  result.invalidRows.forEach((row) => {
    items.push({ row: row.row, status: 'invalid', reason: row.reason, raw: row.raw });
  });
  items.sort((a, b) => a.row - b.row);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-3 text-small">
          <span className="rounded-full bg-positive/10 px-3 py-1 font-medium text-positive">
            {validCount} valid
          </span>
          <span className="rounded-full bg-negative/10 px-3 py-1 font-medium text-negative">
            {invalidCount} invalid
          </span>
        </div>
        <div className="flex gap-2">
          <Button variant="ghost" size="sm" onClick={onCancel}>
            Cancel
          </Button>
          <Button size="sm" onClick={onConfirm} disabled={validCount === 0} loading={pending}>
            Confirm import ({validCount})
          </Button>
        </div>
      </div>

      <div className="rounded-md border border-border bg-surface">
        <Table>
          <THead>
            <TR className="hover:bg-transparent">
              <TH>Status</TH>
              <TH>Date</TH>
              <TH>Ticker</TH>
              <TH>Type</TH>
              <TH align="right">Qty</TH>
              <TH align="right">Price</TH>
              <TH align="right">Fees</TH>
            </TR>
          </THead>
          <TBody>
            {items.map((item, i) => (
              <TR key={`${item.row}-${i}`} data-status={item.status}>
                <TD>
                  <span
                    className={cn(
                      'inline-flex items-center rounded px-2 py-0.5 text-small font-medium',
                      item.status === 'valid'
                        ? 'bg-positive/10 text-positive'
                        : 'bg-negative/10 text-negative',
                    )}
                  >
                    {item.status === 'valid' ? 'valid' : `invalid: ${item.reason}`}
                  </span>
                </TD>
                <TD mono>{item.raw.date}</TD>
                <TD mono>{item.raw.ticker}</TD>
                <TD>{item.raw.type}</TD>
                <TD align="right" mono>
                  {item.raw.quantity}
                </TD>
                <TD align="right" mono>
                  {item.raw.price}
                </TD>
                <TD align="right" mono>
                  {item.raw.fees ?? '0'}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </div>
    </div>
  );
}
