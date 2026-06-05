import { Download } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { exportTransactionsCsv } from '@/api/transactionsApi';

type Props = {
  disabled: boolean;
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

export function ExportButton({ disabled, onExport }: Props) {
  const filename = `stocktracker-transactions-${todayStamp()}.csv`;
  const [pending, setPending] = useState(false);

  async function handleClick() {
    setPending(true);
    try {
      const csv = await exportTransactionsCsv();
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
    } finally {
      setPending(false);
    }
  }

  return (
    <Button
      variant="secondary"
      size="sm"
      data-testid="csv-export"
      onClick={() => {
        void handleClick();
      }}
      disabled={disabled}
      loading={pending}
    >
      <Download size={14} aria-hidden />
      Export CSV
    </Button>
  );
}
