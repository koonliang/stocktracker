import { useMemo, useState } from 'react';
import { Trash2 } from 'lucide-react';
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/Table';
import { Dialog } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { formatCurrency, formatCurrencyCode, formatDateISO, formatShares } from '@/lib/format';
import type { Transaction } from '@/lib/types';
import { cn } from '@/lib/cn';

type Props = {
  transactions: Transaction[];
  onDelete: (id: string) => void | Promise<void>;
};

type Direction = 'asc' | 'desc';

function displayAmount(tx: Transaction) {
  if (tx.amount != null) {
    return tx.amount;
  }
  if (tx.type === 'buy') {
    return tx.quantity * tx.price + tx.fees;
  }
  if (tx.type === 'sell') {
    return tx.quantity * tx.price - tx.fees;
  }
  return null;
}

export function TransactionsTable({ transactions, onDelete }: Props) {
  const [direction, setDirection] = useState<Direction>('desc');
  const [pendingDelete, setPendingDelete] = useState<Transaction | null>(null);

  const sorted = useMemo(() => {
    const list = [...transactions];
    list.sort((a, b) => a.date.localeCompare(b.date) * (direction === 'asc' ? 1 : -1));
    return list;
  }, [transactions, direction]);

  return (
    <>
      <Table data-testid="transactions-table">
        <THead>
          <TR className="hover:bg-transparent">
            <TH>
              <button
                type="button"
                onClick={() => setDirection((d) => (d === 'asc' ? 'desc' : 'asc'))}
                className="inline-flex items-center gap-1 text-text-muted hover:text-text"
                aria-label={`Sort by date ${direction === 'asc' ? 'descending' : 'ascending'}`}
              >
                Date {direction === 'asc' ? '↑' : '↓'}
              </button>
            </TH>
            <TH>Ticker</TH>
            <TH>Type</TH>
            <TH align="right">Qty</TH>
            <TH align="right">Price</TH>
            <TH align="right">Fees</TH>
            <TH align="right">Amount</TH>
            <TH>Currency</TH>
            <TH align="right">
              <span className="sr-only">Actions</span>
            </TH>
          </TR>
        </THead>
        <TBody>
          {sorted.map((tx) => (
            <TR key={tx.id}>
              <TD mono>{formatDateISO(tx.date)}</TD>
              <TD mono>
                <span className="font-mono font-semibold text-text">{tx.ticker}</span>
              </TD>
              <TD>
                <span
                  className={cn(
                    'inline-flex items-center rounded px-2 py-0.5 text-small font-medium uppercase',
                    tx.type === 'buy' || tx.type === 'deposit' || tx.type === 'dividend'
                      ? 'bg-positive/10 text-positive'
                      : 'bg-negative/10 text-negative',
                  )}
                >
                  {tx.type}
                </span>
              </TD>
              <TD align="right" mono>
                {tx.quantity ? formatShares(tx.quantity) : '—'}
              </TD>
              <TD align="right" mono>
                {tx.price ? formatCurrency(tx.price) : '—'}
              </TD>
              <TD align="right" mono>
                {formatCurrency(tx.fees)}
              </TD>
              <TD align="right" mono>
                {formatCurrencyCode(displayAmount(tx), tx.currency)}
              </TD>
              <TD mono>{tx.currency ?? '—'}</TD>
              <TD align="right">
                <button
                  type="button"
                  onClick={() => setPendingDelete(tx)}
                  aria-label={`Delete transaction ${tx.id}`}
                  className="rounded p-1.5 text-text-muted hover:bg-negative/10 hover:text-negative focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
                >
                  <Trash2 size={14} aria-hidden />
                </button>
              </TD>
            </TR>
          ))}
        </TBody>
      </Table>

      <Dialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        title="Delete this transaction?"
        description="It will be removed from your portfolio. This cannot be undone."
        footer={
          <>
            <Button variant="ghost" onClick={() => setPendingDelete(null)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                if (pendingDelete) void onDelete(pendingDelete.id);
                setPendingDelete(null);
              }}
            >
              Delete
            </Button>
          </>
        }
      >
        {pendingDelete && (
          <div className="text-small text-text-muted">
            <span className="font-mono font-semibold text-text">
              {pendingDelete.ticker ?? 'Cash'}
            </span>{' '}
            · {pendingDelete.type} on {formatDateISO(pendingDelete.date)}
          </div>
        )}
      </Dialog>
    </>
  );
}
