import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Card, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import type { TimeRange } from '@/lib/types';
import { getInstrumentAnalysis } from '@/api/instrumentsApi';
import { AnalysisHeader } from '@/features/analysis/AnalysisHeader';
import { PriceChart } from '@/features/analysis/PriceChart';
import { KeyStatsGrid } from '@/features/analysis/KeyStatsGrid';
import { PositionSummary } from '@/features/analysis/PositionSummary';

export function AnalysisRoute() {
  const { ticker } = useParams<{ ticker: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const symbol = (ticker ?? '').toUpperCase();
  const backLink = getBackLink(location.state);
  const [range, setRange] = useState<TimeRange>('1Y');
  const [state, setState] = useState<{
    status: 'loading' | 'success' | 'error';
    error: string | null;
    data: Awaited<ReturnType<typeof getInstrumentAnalysis>> | null;
  }>({ status: 'loading', error: null, data: null });

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading', error: null, data: null });
    void getInstrumentAnalysis(symbol)
      .then((data) => {
        if (!cancelled) {
          setState({ status: 'success', error: null, data });
        }
      })
      .catch((error: Error) => {
        if (!cancelled) {
          setState({ status: 'error', error: error.message, data: null });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [symbol]);

  if (state.status === 'loading') {
    return (
      <Card>
        <CardHeader eyebrow="Loading" title={`Fetching ${symbol}`} />
        <p className="text-body text-text-muted">Analysis data is loading.</p>
      </Card>
    );
  }

  if (state.status === 'error' || !state.data) {
    return (
      <>
        <Link
          to={backLink.to}
          className="mb-4 inline-flex items-center gap-1 text-small text-text-muted hover:text-text"
        >
          <ArrowLeft size={14} aria-hidden />
          {backLink.label}
        </Link>
        <EmptyState
          eyebrow="Analysis unavailable"
          title={`We could not load "${symbol}".`}
          description={state.error ?? 'Try again from the dashboard or watchlists.'}
          actions={<Button onClick={() => navigate(backLink.to)}>{backLink.label}</Button>}
        />
      </>
    );
  }

  const { data } = state;
  const tickerInfo = data.ticker;
  const bars = data.priceHistory;
  const stats = data.stats;

  // Prefer the live quote (matches the dashboard); fall back to the last price bar when absent.
  const last = bars[bars.length - 1];
  const prev = bars[bars.length - 2];
  const currentPrice = data.quote?.price ?? last?.close ?? null;
  const prevClose = data.quote?.previousClose ?? prev?.close ?? null;
  const dayChange = currentPrice != null && prevClose != null ? currentPrice - prevClose : null;
  const dayChangePct =
    currentPrice != null && prevClose != null && prevClose !== 0
      ? (currentPrice - prevClose) / prevClose
      : null;

  return (
    <>
      <Link
        to={backLink.to}
        className="mb-4 inline-flex items-center gap-1 text-small text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        {backLink.label}
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

        <PositionSummary summary={data.positionSummary} />

        <Card>
          <CardHeader eyebrow="Key statistics" title="Snapshot" />
          <KeyStatsGrid stats={stats} />
        </Card>
      </div>
    </>
  );
}

function getBackLink(state: unknown): { to: string; label: string } {
  if (
    state &&
    typeof state === 'object' &&
    'backTo' in state &&
    'backLabel' in state &&
    typeof state.backTo === 'string' &&
    typeof state.backLabel === 'string'
  ) {
    return { to: state.backTo, label: state.backLabel };
  }

  return { to: '/', label: 'Back to dashboard' };
}
