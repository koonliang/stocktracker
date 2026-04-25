import { useState } from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ImportDropzone } from '@/features/transactions/ImportDropzone';
import { ImportPreview } from '@/features/transactions/ImportPreview';
import { ExportButton } from '@/features/transactions/ExportButton';
import { TransactionsTable } from '@/features/transactions/TransactionsTable';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { parseTransactionsCSV, type ParseResult } from '@/lib/csv';

export function TransactionsRoute() {
  const transactions = usePortfolioStore((s) => s.transactions);
  const appendMany = usePortfolioStore((s) => s.appendMany);
  const removeTransaction = usePortfolioStore((s) => s.removeTransaction);

  const [pending, setPending] = useState<ParseResult | null>(null);

  function handleFile(text: string) {
    setPending(parseTransactionsCSV(text));
  }

  function confirm() {
    if (!pending) return;
    appendMany(pending.valid);
    setPending(null);
  }

  return (
    <>
      <PageHeader
        eyebrow="Transactions"
        title="Import &amp; Export"
        description="Bring in transactions from a CSV, review them, then commit. Export at any time to round-trip your portfolio."
        actions={<ExportButton transactions={transactions} />}
      />

      <div className="flex flex-col gap-6">
        {pending ? (
          <Card>
            <CardHeader eyebrow="Preview" title="Review before commit" />
            <ImportPreview result={pending} onConfirm={confirm} onCancel={() => setPending(null)} />
          </Card>
        ) : (
          <Card>
            <CardHeader eyebrow="Import" title="Upload a CSV" />
            <ImportDropzone onFile={handleFile} />
          </Card>
        )}

        <Card padded={false}>
          <div className="p-5 pb-0 sm:p-6 sm:pb-0">
            <CardHeader
              eyebrow="Ledger"
              title={`${transactions.length} transaction${transactions.length === 1 ? '' : 's'}`}
            />
          </div>
          {transactions.length === 0 ? (
            <div className="p-5 sm:p-6">
              <EmptyState
                eyebrow="Nothing yet"
                title="No transactions on file."
                description="Import a CSV above, or seed the demo portfolio from the dashboard."
              />
            </div>
          ) : (
            <TransactionsTable transactions={transactions} onDelete={removeTransaction} />
          )}
        </Card>
      </div>
    </>
  );
}
