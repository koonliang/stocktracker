import { Link } from 'react-router-dom'
import { DashboardNavigation } from '@components/layout'
import { usePortfolio } from '../../hooks/usePortfolio'
import { usePortfolioPerformance } from '../../hooks/usePortfolioPerformance'
import { useModal } from '../../hooks/useModal'
import { PortfolioTable } from '../../components/dashboard/PortfolioTable'
import { PerformanceChart } from '../../components/dashboard/PerformanceChart'
import { QuickAddModal } from '../../components/transactions/QuickAddModal'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters'
import { formatTimeWithRelative } from '../../utils/dateFormatters'

interface SummaryCardProps {
  label: string
  value: string
  className?: string
}

function SummaryCard({ label, value, className = '' }: SummaryCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6 shadow-soft">
      <p className="text-xs sm:text-sm font-medium text-slate-600">{label}</p>
      <p className={`mt-1 sm:mt-2 text-xl sm:text-2xl font-bold ${className || 'text-slate-900'}`}>
        {value}
      </p>
    </div>
  )
}

function TotalReturnCard({ dollars, percent }: { dollars: number; percent: number }) {
  const colorClass = getReturnColorClass(dollars)
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6 shadow-soft">
      <p className="text-xs sm:text-sm font-medium text-slate-600">Total Return</p>
      <div className="mt-1 sm:mt-2">
        <div className={`text-xl sm:text-2xl font-bold ${colorClass}`}>
          {formatCurrency(dollars)}
        </div>
        <div className={`text-base font-semibold mt-0.5 ${colorClass}`}>
          ({formatPercent(percent)})
        </div>
      </div>
    </div>
  )
}

function AnnualizedYieldCard({ yieldPercent, years }: { yieldPercent: number; years: number }) {
  const colorClass = getReturnColorClass(yieldPercent)
  const yearsDisplay =
    years < 1
      ? `${Math.round(years * 12)} months`
      : years === 1
        ? '1 year'
        : `${years.toFixed(1)} years`

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6 shadow-soft">
      <p className="text-xs sm:text-sm font-medium text-slate-600">Annualized Yield</p>
      <div className="mt-1 sm:mt-2">
        <div className={`text-xl sm:text-2xl font-bold ${colorClass}`}>
          {formatPercent(yieldPercent)}
        </div>
        <div className="text-xs text-slate-500 mt-0.5">over {yearsDisplay}</div>
      </div>
    </div>
  )
}

const Dashboard = () => {
  const { portfolio, loading, refreshing, error, refresh } = usePortfolio()
  const {
    data: performanceData,
    range,
    loading: chartLoading,
    changeRange,
  } = usePortfolioPerformance('1y')

  const { isOpen: showQuickAdd, open: openQuickAdd, close: closeQuickAdd } = useModal()

  const handleQuickAddSuccess = () => {
    // Refresh portfolio to reflect changes
    refresh()
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="flex justify-center p-8">
          <p className="text-slate-600">Loading portfolio...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="p-8 text-center">
          <p className="text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900">Portfolio Overview</h1>
          <div className="flex flex-wrap gap-2 sm:gap-3">
            <Link
              to="/dashboard/transactions"
              className="flex-1 sm:flex-none rounded-lg bg-slate-100 px-3 sm:px-4 py-2 text-sm sm:text-base font-medium text-slate-700 transition-colors hover:bg-slate-200 text-center"
            >
              <span className="hidden sm:inline">Manage Transactions</span>
              <span className="sm:hidden">Transactions</span>
            </Link>
            <button
              onClick={refresh}
              disabled={refreshing}
              className="flex-1 sm:flex-none rounded-lg bg-indigo-600 px-3 sm:px-4 py-2 text-sm sm:text-base text-white hover:bg-indigo-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span className="hidden sm:inline">{refreshing ? 'Refreshing...' : 'Refresh Prices'}</span>
              <span className="sm:hidden">{refreshing ? '...' : 'Refresh'}</span>
            </button>
          </div>
        </header>

        {/* Portfolio Summary Card */}
        {portfolio && (
          <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
            <SummaryCard label="Total Cost" value={formatCurrency(portfolio.totalCost)} />
            <TotalReturnCard
              dollars={portfolio.totalReturnDollars}
              percent={portfolio.totalReturnPercent}
            />
            <AnnualizedYieldCard
              yieldPercent={portfolio.annualizedYield}
              years={portfolio.investmentYears}
            />
          </div>
        )}

        {/* Performance Chart Widget */}
        <section className="mb-8">
          <PerformanceChart
            data={performanceData}
            range={range}
            onRangeChange={changeRange}
            loading={chartLoading}
          />
        </section>

        {/* Holdings Table */}
        <section>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">Your Holdings</h2>
            <span className="text-sm text-slate-500">
              Prices updated: {formatTimeWithRelative(portfolio?.pricesUpdatedAt)}
            </span>
          </div>
          {portfolio?.holdings.length ? (
            <PortfolioTable holdings={portfolio.holdings} />
          ) : (
            <p className="text-slate-500">No holdings yet. Add transactions to get started.</p>
          )}
        </section>
      </div>

      {/* Quick Add Modal */}
      <QuickAddModal
        isOpen={showQuickAdd}
        onClose={closeQuickAdd}
        onSuccess={handleQuickAddSuccess}
      />

      {/* Floating Action Button */}
      <button
        onClick={openQuickAdd}
        className="fixed bottom-6 right-6 h-14 w-14 rounded-full bg-indigo-600 text-white shadow-lg
                   transition-all duration-200 hover:bg-indigo-700 hover:shadow-xl hover:scale-110
                   focus:outline-none focus:ring-4 focus:ring-indigo-300
                   sm:h-16 sm:w-16"
        aria-label="Add transaction"
        title="Add Transaction"
      >
        <svg
          className="h-6 w-6 sm:h-8 sm:w-8 mx-auto"
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2.5"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path d="M12 4v16m8-8H4" />
        </svg>
      </button>
    </div>
  )
}

export default Dashboard
