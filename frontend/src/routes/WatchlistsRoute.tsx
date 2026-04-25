import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ListChecks, Plus } from 'lucide-react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { NewWatchlistDialog } from '@/features/watchlist/NewWatchlistDialog';
import { formatDateISO } from '@/lib/format';

export function WatchlistsRoute() {
  const watchlists = useWatchlistStore((s) => s.watchlists);
  const [dialogOpen, setDialogOpen] = useState(false);

  const hasAny = watchlists.length > 0;

  return (
    <>
      <PageHeader
        eyebrow="Track without owning"
        title="Watchlists"
        description="Named lists of tickers you want to monitor. Create as many as you like."
        actions={
          <Button onClick={() => setDialogOpen(true)}>
            <Plus size={14} aria-hidden />
            New watchlist
          </Button>
        }
      />

      {!hasAny ? (
        <EmptyState
          eyebrow="No watchlists yet"
          title="Start by creating your first watchlist."
          description="Group tickers you want to keep an eye on — by theme, sector, or any way you like."
          icon={<ListChecks size={18} aria-hidden />}
          actions={
            <Button onClick={() => setDialogOpen(true)}>
              <Plus size={14} aria-hidden />
              New watchlist
            </Button>
          }
        />
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {watchlists.map((wl) => (
            <li key={wl.id}>
              <Link
                to={`/watchlists/${wl.id}`}
                className="block focus-visible:outline-none"
                aria-label={`Open watchlist ${wl.name}`}
              >
                <Card className="transition-colors hover:border-border-strong focus-within:ring-2 focus-within:ring-focus-ring">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="eyebrow mb-1">Watchlist</div>
                      <h2 className="truncate font-display text-title text-text">{wl.name}</h2>
                    </div>
                    <span className="font-mono tabular text-small text-text-muted">
                      {wl.tickers.length}
                    </span>
                  </div>
                  <p className="mt-3 line-clamp-1 text-small text-text-muted">
                    {wl.tickers.length === 0 ? 'Empty — add tickers' : wl.tickers.join(' · ')}
                  </p>
                  <div className="mt-4 text-small text-text-subtle">
                    Updated {formatDateISO(wl.updatedAt.slice(0, 10))}
                  </div>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <NewWatchlistDialog open={dialogOpen} onClose={() => setDialogOpen(false)} />
    </>
  );
}
