import { Link } from 'react-router-dom';
import { Upload, ListChecks } from 'lucide-react';
import { EmptyState } from '@/components/ui/EmptyState';

export function DashboardEmptyState() {
  return (
    <EmptyState
      eyebrow="No positions yet"
      title="Your portfolio is empty."
      description="Import transactions from a CSV or add a watchlist while you decide what to buy."
      icon={<Upload size={18} aria-hidden />}
      actions={
        <>
          <Link
            to="/transactions"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-border bg-surface px-4 text-body font-medium text-text transition-colors hover:border-border-strong hover:bg-surface-alt focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
          >
            <Upload size={14} aria-hidden />
            Import transactions
          </Link>
          <Link
            to="/watchlists"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-border bg-surface px-4 text-body font-medium text-text transition-colors hover:border-border-strong hover:bg-surface-alt focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
          >
            <ListChecks size={14} aria-hidden />
            Create a watchlist
          </Link>
        </>
      }
    />
  );
}
