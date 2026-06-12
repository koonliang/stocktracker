import { useEffect } from 'react';
import { SummaryTiles } from '@/features/dashboard/SummaryTiles';
import { HoldingsTable } from '@/features/dashboard/HoldingsTable';
import { AllocationChart } from '@/features/dashboard/AllocationChart';
import { DashboardEmptyState } from '@/features/dashboard/DashboardEmptyState';
import { useHoldings } from '@/features/dashboard/useHoldings';
import { LiveQuotesIndicator } from '@/features/quotes/LiveQuotesIndicator';
import { SymbolSearch } from '@/features/search/SymbolSearch';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { useQuotesStore } from '@/stores/quotesStore';
import { formatDateISO, todayISO } from '@/lib/format';

export function DashboardRoute() {
  const { holdings, summary, dashboardStatus, error } = useHoldings();
  const loadDashboard = usePortfolioStore((state) => state.loadDashboard);
  const setSymbols = useQuotesStore((state) => state.setSymbols);
  const startPolling = useQuotesStore((state) => state.startPolling);
  const stopPolling = useQuotesStore((state) => state.stopPolling);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  // Visibility-aware quote polling; each tick re-loads the dashboard so values tick without reload.
  useEffect(() => {
    startPolling(() => void loadDashboard());
    return () => stopPolling();
  }, [startPolling, stopPolling, loadDashboard]);

  // Keep the polled symbol set in sync with what's held.
  useEffect(() => {
    setSymbols(holdings.map((h) => h.ticker));
  }, [holdings, setSymbols]);

  return (
    <>
      <PageHeader
        eyebrow={`As of ${formatDateISO(todayISO())}`}
        title="Portfolio"
        description="A running ledger of what you own, what it cost, and what it's doing today."
        actions={<LiveQuotesIndicator />}
      />

      {dashboardStatus === 'loading' ? (
        <Card>
          <CardHeader eyebrow="Loading" title="Fetching your dashboard" />
          <p className="text-body text-text-muted">Holdings and summary data are loading.</p>
        </Card>
      ) : dashboardStatus === 'error' ? (
        <EmptyState
          eyebrow="Dashboard error"
          title="We could not load the portfolio."
          description={error ?? 'Try refreshing the page or checking the backend service.'}
        />
      ) : (
        <div className="flex flex-col gap-6">
          <Card>
            <CardHeader eyebrow="Add a symbol" title="Track any market" />
            <SymbolSearch onAdded={() => void loadDashboard()} />
          </Card>

          {holdings.length === 0 ? (
            <DashboardEmptyState />
          ) : (
            <>
              <SummaryTiles summary={summary} />

              <Card>
                <CardHeader eyebrow="Allocation" title="Composition" />
                <AllocationChart holdings={holdings} />
              </Card>

              <Card padded={false}>
                <div className="p-5 pb-0 sm:p-6 sm:pb-0">
                  <CardHeader eyebrow="Positions" title={`${holdings.length} holdings`} />
                </div>
                <HoldingsTable holdings={holdings} baseCurrency={summary.baseCurrency} />
              </Card>
            </>
          )}
        </div>
      )}
    </>
  );
}
