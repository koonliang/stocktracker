import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Card, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import { findTicker, getBars, loadStats } from '@/lib/seed';
import type { TimeRange } from '@/lib/types';
import { AnalysisHeader } from '@/features/analysis/AnalysisHeader';
import { PriceChart } from '@/features/analysis/PriceChart';
import { KeyStatsGrid } from '@/features/analysis/KeyStatsGrid';
import { PositionSummary } from '@/features/analysis/PositionSummary';

export function AnalysisRoute() {
  const { ticker } = useParams<{ ticker: string }>();
  const navigate = useNavigate();
  const symbol = (ticker ?? '').toUpperCase();
  const [range, setRange] = useState<TimeRange>('1Y');

  const tickerInfo = findTicker(symbol);
  const bars = useMemo(() => getBars(symbol), [symbol]);
  const stats = loadStats()[symbol] ?? null;

  if (!tickerInfo) {
    return (
      <>
        <Link
          to="/"
          className="mb-4 inline-flex items-center gap-1 text-small text-text-muted hover:text-text"
        >
          <ArrowLeft size={14} aria-hidden />
          Back to dashboard
        </Link>
        <EmptyState
          eyebrow="Not found"
          title={`We don't know "${symbol}".`}
          description="That ticker isn't in the bundled catalog. Try searching from the top bar, or head back to your dashboard."
          actions={<Button onClick={() => navigate('/')}>Back to dashboard</Button>}
        />
      </>
    );
  }

  const last = bars[bars.length - 1];
  const prev = bars[bars.length - 2];
  const currentPrice = last?.close ?? null;
  const prevClose = prev?.close ?? null;
  const dayChange = currentPrice != null && prevClose != null ? currentPrice - prevClose : null;
  const dayChangePct =
    currentPrice != null && prevClose != null && prevClose !== 0
      ? (currentPrice - prevClose) / prevClose
      : null;

  return (
    <>
      <Link
        to="/"
        className="mb-4 inline-flex items-center gap-1 text-small text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        Back to dashboard
      </Link>

      <AnalysisHeader
        symbol={tickerInfo.symbol}
        name={tickerInfo.name}
        sector={tickerInfo.sector}
        exchange={tickerInfo.exchange}
        currentPrice={currentPrice}
        dayChange={dayChange}
        dayChangePct={dayChangePct}
      />

      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader eyebrow="Price" title="Performance" />
          <PriceChart bars={bars} range={range} onRangeChange={setRange} />
        </Card>

        <PositionSummary symbol={symbol} />

        <Card>
          <CardHeader eyebrow="Key statistics" title="Snapshot" />
          <KeyStatsGrid stats={stats} />
        </Card>
      </div>
    </>
  );
}
