import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { buildPriceLookup } from '@/lib/portfolio';
import { findTicker, loadPrices } from '@/lib/seed';
import { WatchlistHeader } from '@/features/watchlist/WatchlistHeader';
import { AddTickerInput } from '@/features/watchlist/AddTickerInput';
import { WatchlistRow } from '@/features/watchlist/WatchlistRow';

export function WatchlistDetailRoute() {
  const { id } = useParams<{ id: string }>();
  const load = useWatchlistStore((s) => s.load);
  const watchlist = useWatchlistStore((s) => s.watchlists.find((w) => w.id === id));
  const removeTicker = useWatchlistStore((s) => s.removeTicker);
  const reorderTickers = useWatchlistStore((s) => s.reorderTickers);
  const status = useWatchlistStore((s) => s.status);
  const navigate = useNavigate();

  const priceLookup = useMemo(() => buildPriceLookup(loadPrices()), []);

  const dragFromRef = useRef<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

  useEffect(() => {
    void load();
  }, [load]);

  if (status === 'loading' && !watchlist) {
    return (
      <Card>
        <p className="text-body text-text-muted">Loading watchlist…</p>
      </Card>
    );
  }

  if (!id || !watchlist) {
    return (
      <>
        <Link
          to="/watchlists"
          className="mb-4 inline-flex items-center gap-1 text-small text-text-muted hover:text-text"
        >
          <ArrowLeft size={14} aria-hidden />
          Back to watchlists
        </Link>
        <EmptyState
          eyebrow="Not found"
          title="This watchlist no longer exists."
          description="It may have been deleted. Head back to your watchlists to create a new one."
          actions={<Button onClick={() => navigate('/watchlists')}>Back to watchlists</Button>}
        />
      </>
    );
  }

  return (
    <>
      <Link
        to="/watchlists"
        className="mb-4 inline-flex items-center gap-1 text-small text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        Back to watchlists
      </Link>

      <WatchlistHeader watchlist={watchlist} onDeleted={() => navigate('/watchlists')} />

      <div className="flex flex-col gap-6">
        <Card>
          <AddTickerInput watchlistId={watchlist.id} />
        </Card>

        {watchlist.tickers.length === 0 ? (
          <EmptyState
            eyebrow="No tickers yet"
            title="Add tickers to start tracking."
            description="Search for a ticker in the catalog and hit Add. Unknown tickers are rejected with an inline error."
          />
        ) : (
          <Card padded={false}>
            <ul className="flex flex-col" aria-label={`${watchlist.name} tickers`}>
              {watchlist.tickers.map((symbol, idx) => {
                const ticker = findTicker(symbol);
                const current = priceLookup.current.get(symbol) ?? null;
                const prev = priceLookup.previous.get(symbol) ?? null;
                const dayChange = current != null && prev != null ? current - prev : null;
                const dayChangePct =
                  current != null && prev != null && prev !== 0 ? (current - prev) / prev : null;

                return (
                  <WatchlistRow
                    key={symbol}
                    symbol={symbol}
                    name={ticker?.name ?? symbol}
                    currentPrice={current}
                    dayChange={dayChange}
                    dayChangePct={dayChangePct}
                    onRemove={() => {
                      void removeTicker(watchlist.id, symbol);
                    }}
                    onMoveUp={() => {
                      void reorderTickers(watchlist.id, idx, idx - 1);
                    }}
                    onMoveDown={() => {
                      void reorderTickers(watchlist.id, idx, idx + 1);
                    }}
                    canMoveUp={idx > 0}
                    canMoveDown={idx < watchlist.tickers.length - 1}
                    dragHandleProps={{
                      draggable: true,
                      onDragStart: () => {
                        dragFromRef.current = idx;
                      },
                      onDragOver: (e) => {
                        e.preventDefault();
                        setDragOverIndex(idx);
                      },
                      onDragEnd: () => {
                        dragFromRef.current = null;
                        setDragOverIndex(null);
                      },
                      onDrop: (e) => {
                        e.preventDefault();
                        const from = dragFromRef.current;
                        if (from != null && from !== idx) {
                          void reorderTickers(watchlist.id, from, idx);
                        }
                        dragFromRef.current = null;
                        setDragOverIndex(null);
                      },
                    }}
                  />
                );
              })}
            </ul>
            {dragOverIndex != null && <span className="sr-only">Reordering</span>}
          </Card>
        )}
      </div>
    </>
  );
}
