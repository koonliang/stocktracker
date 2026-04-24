import { Link } from 'react-router-dom';
import { Upload, ListChecks, Sparkles } from 'lucide-react';
import { EmptyState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import { usePortfolioStore } from '@/stores/portfolioStore';

export function DashboardEmptyState() {
  const seed = usePortfolioStore((s) => s.seedFromFixture);
  return (
    <EmptyState
      eyebrow="No positions yet"
      title="Your portfolio is a blank page."
      description="Reload the demo portfolio to explore the prototype, import transactions from a CSV, or start by tracking tickers in a watchlist."
      icon={<Upload size={18} aria-hidden />}
      actions={
        <>
          <Button variant="primary" onClick={seed}>
            <Sparkles size={14} aria-hidden />
            Reload demo data
          </Button>
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
