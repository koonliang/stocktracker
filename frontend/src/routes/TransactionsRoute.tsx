import { useEffect } from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ImportDropzone } from '@/features/transactions/ImportDropzone';
import { ImportPreview } from '@/features/transactions/ImportPreview';
import { ExportButton } from '@/features/transactions/ExportButton';
import { TransactionsTable } from '@/features/transactions/TransactionsTable';
import { usePortfolioStore } from '@/stores/portfolioStore';

export function TransactionsRoute() {
  const {
    transactions,
    preview,
    transactionsStatus,
    previewStatus,
    commitStatus,
    error,
    loadTransactions,
    deleteTransaction,
    previewImport,
    clearPreview,
    commitPreview,
  } = usePortfolioStore();

  useEffect(() => {
    void loadTransactions();
  }, [loadTransactions]);

  return (
    <>
      <PageHeader
        eyebrow="Transactions"
        title="Import &amp; Export"
        description="Bring in transactions from a CSV, review them, then commit. Export at any time to round-trip your portfolio."
        actions={<ExportButton disabled={transactions.length === 0} />}
      />

      <div className="flex flex-col gap-6">
        {preview ? (
          <Card>
            <CardHeader eyebrow="Preview" title="Review before commit" />
            <ImportPreview
              result={preview}
              pending={commitStatus === 'loading'}
              onConfirm={() => void commitPreview()}
              onCancel={clearPreview}
            />
          </Card>
        ) : (
          <Card>
            <CardHeader eyebrow="Import" title="Upload a CSV" />
            <ImportDropzone
              onFile={(file) => {
                void previewImport(file);
              }}
              loading={previewStatus === 'loading'}
            />
          </Card>
        )}

        <Card padded={false}>
          <div className="p-5 pb-0 sm:p-6 sm:pb-0">
            <CardHeader
              eyebrow="Ledger"
              title={`${transactions.length} transaction${transactions.length === 1 ? '' : 's'}`}
            />
          </div>
          {transactionsStatus === 'loading' ? (
            <div className="p-5 text-body text-text-muted sm:p-6">Loading transactions…</div>
          ) : transactionsStatus === 'error' ? (
            <div className="p-5 sm:p-6">
              <EmptyState
                eyebrow="Transactions error"
                title="The transaction history could not be loaded."
                description={error ?? 'Check the backend and try again.'}
              />
            </div>
          ) : transactions.length === 0 ? (
            <div className="p-5 sm:p-6">
              <EmptyState
                eyebrow="Nothing yet"
                title="No transactions on file."
                description="Import a CSV above to populate the server-backed ledger."
              />
            </div>
          ) : (
            <TransactionsTable
              transactions={transactions}
              onDelete={(id) => void deleteTransaction(id)}
            />
          )}
        </Card>
      </div>
    </>
  );
}
