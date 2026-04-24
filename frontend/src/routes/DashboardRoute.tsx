import { Trash2 } from 'lucide-react';
import { SummaryTiles } from '@/features/dashboard/SummaryTiles';
import { HoldingsTable } from '@/features/dashboard/HoldingsTable';
import { AllocationChart } from '@/features/dashboard/AllocationChart';
import { DashboardEmptyState } from '@/features/dashboard/DashboardEmptyState';
import { useHoldings } from '@/features/dashboard/useHoldings';
import { PageHeader } from '@/components/layout/PageHeader';
import { CardHeader, Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { formatDateISO, todayISO } from '@/lib/format';

export function DashboardRoute() {
  const { holdings, summary } = useHoldings();
  const clear = usePortfolioStore((s) => s.clear);

  const hasData = holdings.length > 0;

  return (
    <>
      <PageHeader
        eyebrow={`As of ${formatDateISO(todayISO())}`}
        title="Portfolio"
        description="A running ledger of what you own, what it cost, and what it's doing today."
        actions={
          hasData ? (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                if (confirm('Clear all transactions? This cannot be undone.')) clear();
              }}
            >
              <Trash2 size={14} aria-hidden />
              Clear demo data
            </Button>
          ) : null
        }
      />

      {!hasData ? (
        <DashboardEmptyState />
      ) : (
        <div className="flex flex-col gap-6">
          <SummaryTiles summary={summary} />

          <Card>
            <CardHeader eyebrow="Allocation" title="Composition" />
            <AllocationChart holdings={holdings} />
          </Card>

          <Card padded={false}>
            <div className="p-5 pb-0 sm:p-6 sm:pb-0">
              <CardHeader eyebrow="Positions" title={`${holdings.length} holdings`} />
            </div>
            <HoldingsTable holdings={holdings} />
          </Card>
        </div>
      )}
    </>
  );
}
