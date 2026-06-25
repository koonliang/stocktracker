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
  pending?: boolean;
  onDelete: (id: string) => void | Promise<void>;
  onDeleteMany: (ids: string[]) => void | Promise<void>;
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

export function TransactionsTable({
  transactions,
  pending = false,
  onDelete,
  onDeleteMany,
}: Props) {
  const [direction, setDirection] = useState<Direction>('desc');
  const [pendingDelete, setPendingDelete] = useState<Transaction | null>(null);
  const [pendingBulkDelete, setPendingBulkDelete] = useState<string[] | null>(null);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [mobileSelectionMode, setMobileSelectionMode] = useState(false);

  const sorted = useMemo(() => {
    const list = [...transactions];
    list.sort((a, b) => a.date.localeCompare(b.date) * (direction === 'asc' ? 1 : -1));
    return list;
  }, [transactions, direction]);

  const allSelected = sorted.length > 0 && selectedIds.length === sorted.length;
  const selectedCount = selectedIds.length;
  const selectedTransactions = sorted.filter((tx) => selectedIds.includes(tx.id));

  function toggleSelected(id: string) {
    setSelectedIds((current) =>
      current.includes(id) ? current.filter((value) => value !== id) : [...current, id],
    );
  }

  function toggleAllSelected() {
    setSelectedIds((current) =>
      current.length === sorted.length ? [] : sorted.map((tx) => tx.id),
    );
  }

  function clearSelection() {
    setSelectedIds([]);
    setMobileSelectionMode(false);
  }

  return (
    <>
      <div className="px-4 pb-3 pt-4 sm:px-3">
        <div className="flex items-center justify-between gap-3">
          <div className="text-small text-text-muted">
            {selectedCount > 0
              ? `${selectedCount} selected`
              : 'Select more than one transaction to delete them together.'}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              className="sm:hidden"
              onClick={() => {
                if (mobileSelectionMode) {
                  clearSelection();
                } else {
                  setMobileSelectionMode(true);
                }
              }}
            >
              {mobileSelectionMode ? 'Done' : 'Select'}
            </Button>
            {selectedCount > 0 ? (
              <Button variant="ghost" size="sm" onClick={clearSelection}>
                Clear
              </Button>
            ) : null}
            {selectedCount > 1 ? (
              <Button
                variant="danger"
                size="sm"
                className="hidden whitespace-nowrap sm:inline-flex"
                disabled={pending}
                onClick={() => setPendingBulkDelete([...selectedIds])}
              >
                <Trash2 size={14} aria-hidden />
                Delete selected
              </Button>
            ) : null}
          </div>
        </div>
        {selectedCount > 1 ? (
          <Button
            variant="danger"
            size="sm"
            className="mt-3 w-full whitespace-nowrap sm:hidden"
            disabled={pending}
            onClick={() => setPendingBulkDelete([...selectedIds])}
          >
            <Trash2 size={14} aria-hidden />
            Delete {selectedCount}
          </Button>
        ) : null}
      </div>

      <ul className="sm:hidden">
        {sorted.map((tx) => (
          <li key={tx.id} className="flex items-start gap-3 border-b border-border px-4 py-3">
            {mobileSelectionMode ? (
              <label className="mt-0.5 flex items-center">
                <input
                  type="checkbox"
                  checked={selectedIds.includes(tx.id)}
                  onChange={() => toggleSelected(tx.id)}
                  aria-label={`Select transaction ${tx.id}`}
                  className="h-4 w-4 rounded border-border"
                />
              </label>
            ) : null}
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="font-mono font-semibold text-text">{tx.ticker ?? '—'}</span>
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
              </div>
              <div className="mt-0.5 whitespace-nowrap text-small text-text-muted">
                {formatDateISO(tx.date)}
                {tx.quantity ? ` · ${formatShares(tx.quantity)} @ ${formatCurrency(tx.price)}` : ''}
              </div>
            </div>
            <div className="flex-shrink-0 text-right font-mono tabular-nums text-text">
              {formatCurrencyCode(displayAmount(tx), tx.currency)}
            </div>
            {!mobileSelectionMode ? (
              <button
                type="button"
                onClick={() => setPendingDelete(tx)}
                aria-label={`Delete transaction ${tx.id}`}
                className="rounded p-1.5 text-text-muted hover:bg-negative/10 hover:text-negative focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
              >
                <Trash2 size={14} aria-hidden />
              </button>
            ) : null}
          </li>
        ))}
      </ul>

      <div className="hidden sm:block">
        <Table data-testid="transactions-table">
          <THead>
            <TR className="hover:bg-transparent">
              <TH className="w-10">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={toggleAllSelected}
                  aria-label="Select all transactions"
                  className="h-4 w-4 rounded border-border"
                />
              </TH>
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
              <TH align="right" className="hidden sm:table-cell">
                Qty
              </TH>
              <TH align="right" className="hidden sm:table-cell">
                Price
              </TH>
              <TH align="right" className="hidden md:table-cell">
                Fees
              </TH>
              <TH align="right">Amount</TH>
              <TH className="hidden sm:table-cell">Currency</TH>
              <TH align="right">
                <span className="sr-only">Actions</span>
              </TH>
            </TR>
          </THead>
          <TBody>
            {sorted.map((tx) => (
              <TR key={tx.id}>
                <TD>
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(tx.id)}
                    onChange={() => toggleSelected(tx.id)}
                    aria-label={`Select transaction ${tx.id}`}
                    className="h-4 w-4 rounded border-border"
                  />
                </TD>
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
                <TD align="right" mono className="hidden sm:table-cell">
                  {tx.quantity ? formatShares(tx.quantity) : '—'}
                </TD>
                <TD align="right" mono className="hidden sm:table-cell">
                  {tx.price ? formatCurrency(tx.price) : '—'}
                </TD>
                <TD align="right" mono className="hidden md:table-cell">
                  {formatCurrency(tx.fees)}
                </TD>
                <TD align="right" mono>
                  {formatCurrencyCode(displayAmount(tx), tx.currency)}
                </TD>
                <TD mono className="hidden sm:table-cell">
                  {tx.currency ?? '—'}
                </TD>
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
      </div>

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
              disabled={pending}
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

      <Dialog
        open={pendingBulkDelete !== null}
        onClose={() => setPendingBulkDelete(null)}
        title="Delete selected transactions?"
        description="These transactions will be removed from your portfolio. This cannot be undone."
        footer={
          <>
            <Button variant="ghost" onClick={() => setPendingBulkDelete(null)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              disabled={pending}
              onClick={() => {
                if (pendingBulkDelete) void onDeleteMany(pendingBulkDelete);
                setPendingBulkDelete(null);
                clearSelection();
              }}
            >
              Delete {pendingBulkDelete?.length ?? 0}
            </Button>
          </>
        }
      >
        <div className="space-y-3 text-small text-text-muted">
          <div>{pendingBulkDelete?.length ?? 0} transactions selected.</div>
          <div className="space-y-1">
            {selectedTransactions.slice(0, 5).map((tx) => (
              <div key={tx.id}>
                <span className="font-mono font-semibold text-text">{tx.ticker ?? 'Cash'}</span> ·{' '}
                {tx.type} on {formatDateISO(tx.date)}
              </div>
            ))}
            {selectedTransactions.length > 5 ? (
              <div>and {selectedTransactions.length - 5} more.</div>
            ) : null}
          </div>
        </div>
      </Dialog>
    </>
  );
}
