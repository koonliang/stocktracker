import { DashboardNavigation } from '@components/layout';
import { usePortfolio } from '../../hooks/usePortfolio';
import { PortfolioTable } from '../../components/dashboard/PortfolioTable';
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters';

interface SummaryCardProps {
  label: string;
  value: string;
  className?: string;
}

function SummaryCard({ label, value, className = '' }: SummaryCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-soft">
      <p className="text-sm font-medium text-slate-600">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${className || 'text-slate-900'}`}>{value}</p>
    </div>
  );
}

const Dashboard = () => {
  const { portfolio, loading, error, refresh } = usePortfolio();

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="flex justify-center p-8">
          <p className="text-slate-600">Loading portfolio...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="p-8 text-center">
          <p className="text-red-500">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="container mx-auto px-4 py-8">
        <header className="mb-8 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">Portfolio Overview</h1>
          <button
            onClick={refresh}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700"
          >
            Refresh Prices
          </button>
        </header>

        {/* Portfolio Summary Card */}
        {portfolio && (
          <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
            <SummaryCard label="Total Cost" value={formatCurrency(portfolio.totalCost)} />
            <SummaryCard
              label="Total Return"
              value={formatCurrency(portfolio.totalReturnDollars)}
              className={getReturnColorClass(portfolio.totalReturnDollars)}
            />
            <SummaryCard
              label="Return %"
              value={formatPercent(portfolio.totalReturnPercent)}
              className={getReturnColorClass(portfolio.totalReturnPercent)}
            />
          </div>
        )}

        {/* Holdings Table */}
        <section>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">Your Holdings</h2>
            <span className="text-sm text-slate-500">
              Prices updated: {portfolio?.pricesUpdatedAt ? new Date(portfolio.pricesUpdatedAt).toLocaleTimeString() : '-'}
            </span>
          </div>
          {portfolio?.holdings.length ? (
            <PortfolioTable holdings={portfolio.holdings} />
          ) : (
            <p className="text-slate-500">No holdings yet.</p>
          )}
        </section>
      </div>
    </div>
  );
};

export default Dashboard;
