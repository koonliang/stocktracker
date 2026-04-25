import { Download } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { serializeTransactionsCSV } from '@/lib/csv';
import type { Transaction } from '@/lib/types';

type Props = {
  transactions: Transaction[];
  /** Override Blob/URL handling — used by tests to capture the produced CSV. */
  onExport?: (csv: string, filename: string) => void;
};

function todayStamp(): string {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}${mm}${dd}`;
}

export function ExportButton({ transactions, onExport }: Props) {
  const disabled = transactions.length === 0;
  const filename = `stocktracker-transactions-${todayStamp()}.csv`;

  function handleClick() {
    const csv = serializeTransactionsCSV(transactions);
    if (onExport) {
      onExport(csv, filename);
      return;
    }
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  return (
    <Button variant="secondary" size="sm" onClick={handleClick} disabled={disabled}>
      <Download size={14} aria-hidden />
      Export CSV
    </Button>
  );
}
