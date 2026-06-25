import { useEffect, useState } from 'react';
import { Plus, Upload } from 'lucide-react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { ImportDropzone } from '@/features/transactions/ImportDropzone';
import { ImportPreview } from '@/features/transactions/ImportPreview';
import { ExportButton } from '@/features/transactions/ExportButton';
import { TransactionsTable } from '@/features/transactions/TransactionsTable';
import { TransactionForm } from '@/features/transactions/TransactionForm';
import { FAB } from '@/components/ui/FAB';
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
    deleteTransactions,
    previewImport,
    clearPreview,
    commitPreview,
    createManualTransaction,
  } = usePortfolioStore();
  const [manualDialogOpen, setManualDialogOpen] = useState(false);
  const [importDialogOpen, setImportDialogOpen] = useState(false);

  useEffect(() => {
    void loadTransactions();
  }, [loadTransactions]);

  function closeImportDialog() {
    clearPreview();
    setImportDialogOpen(false);
  }

  return (
    <>
      <PageHeader
        eyebrow="Transactions"
        title="Import &amp; Export"
        description="Bring in transactions from a CSV, review them, then commit. Export at any time to round-trip your portfolio."
        actions={
          <>
            <Button
              size="sm"
              className="hidden md:inline-flex"
              onClick={() => setManualDialogOpen(true)}
            >
              <Plus size={14} aria-hidden />
              New Transaction
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setImportDialogOpen(true)}>
              <Upload size={14} aria-hidden />
              Import CSV
            </Button>
            <ExportButton disabled={transactions.length === 0} />
          </>
        }
      />

      <div className="flex flex-col gap-6">
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
                description="Tap + to record a trade, or import a CSV."
              />
            </div>
          ) : (
            <TransactionsTable
              transactions={transactions}
              pending={commitStatus === 'loading'}
              onDelete={(id) => void deleteTransaction(id)}
              onDeleteMany={(ids) => void deleteTransactions(ids)}
            />
          )}
        </Card>
      </div>

      <Dialog
        open={manualDialogOpen}
        onClose={() => setManualDialogOpen(false)}
        title="New transaction"
        description="Record a transaction manually."
        className="max-w-5xl"
      >
        <TransactionForm
          pending={commitStatus === 'loading'}
          onSubmit={async (row) => {
            await createManualTransaction(row);
            if (usePortfolioStore.getState().commitStatus === 'success') {
              setManualDialogOpen(false);
            }
          }}
        />
      </Dialog>

      <Dialog
        open={importDialogOpen}
        onClose={closeImportDialog}
        title={preview ? 'Review import' : 'Import CSV'}
        description={
          preview
            ? 'Review the parsed rows before committing them to your portfolio.'
            : 'Upload a CSV file, then review the parsed transactions before import.'
        }
        className="max-w-6xl"
      >
        {preview ? (
          <ImportPreview
            result={preview}
            pending={commitStatus === 'loading'}
            onConfirm={async () => {
              await commitPreview();
              if (usePortfolioStore.getState().commitStatus === 'success') {
                setImportDialogOpen(false);
              }
            }}
            onCancel={closeImportDialog}
          />
        ) : (
          <ImportDropzone
            onFile={(file) => {
              void previewImport(file);
            }}
            loading={previewStatus === 'loading'}
          />
        )}
      </Dialog>

      <FAB label="New transaction" onClick={() => setManualDialogOpen(true)} />
    </>
  );
}
