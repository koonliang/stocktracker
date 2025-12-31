import { DashboardNavigation } from '@components/layout'
import { usePortfolio } from '../../hooks/usePortfolio'
import { usePortfolioPerformance } from '../../hooks/usePortfolioPerformance'
import { useTransactions } from '../../hooks/useTransactions'
import { PortfolioTable } from '../../components/dashboard/PortfolioTable'
import { PerformanceChart } from '../../components/dashboard/PerformanceChart'
import { TransactionForm } from '../../components/transactions/TransactionForm'
import { TransactionGrid } from '../../components/transactions/TransactionGrid'
import type { TransactionRequest } from '../../services/api/transactionApi'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters'
import { useState } from 'react'

interface SummaryCardProps {
  label: string
  value: string
  className?: string
}

function SummaryCard({ label, value, className = '' }: SummaryCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-soft">
      <p className="text-sm font-medium text-slate-600">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${className || 'text-slate-900'}`}>{value}</p>
    </div>
  )
}

const Dashboard = () => {
  const { portfolio, loading, error, refresh } = usePortfolio()
  const {
    data: performanceData,
    range,
    loading: chartLoading,
    changeRange,
  } = usePortfolioPerformance('1y')
  const {
    transactions,
    loading: txLoading,
    createTransaction,
    updateTransaction,
    deleteTransaction,
  } = useTransactions()

  const [showTransactions, setShowTransactions] = useState(false)
  const [showAddForm, setShowAddForm] = useState(false)

  const handleCreateTransaction = async (request: TransactionRequest) => {
    await createTransaction(request)
    setShowAddForm(false)
    // Refresh portfolio to reflect changes
    refresh()
  }

  const handleUpdateTransaction = async (id: number, request: TransactionRequest) => {
    await updateTransaction(id, request)
    refresh()
  }

  const handleDeleteTransaction = async (id: number) => {
    await deleteTransaction(id)
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
        <header className="mb-8 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">Portfolio Overview</h1>
          <div className="flex gap-3">
            <button
              onClick={() => setShowTransactions(!showTransactions)}
              className={`rounded-lg px-4 py-2 font-medium transition-colors
                ${
                  showTransactions
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
            >
              {showTransactions ? 'Hide Transactions' : 'Manage Transactions'}
            </button>
            <button
              onClick={refresh}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700"
            >
              Refresh Prices
            </button>
          </div>
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

        {/* Transaction Management Section */}
        {showTransactions && (
          <section className="mb-8">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-soft">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-900">Transactions</h2>
                {!showAddForm && (
                  <button
                    onClick={() => setShowAddForm(true)}
                    className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white
                             hover:bg-indigo-700"
                  >
                    + Add Transaction
                  </button>
                )}
              </div>

              {/* Quick Add Form */}
              {showAddForm && (
                <div className="mb-6 rounded-lg border border-indigo-200 bg-indigo-50 p-4">
                  <h3 className="mb-3 font-medium text-indigo-900">New Transaction</h3>
                  <TransactionForm
                    onSubmit={handleCreateTransaction}
                    onCancel={() => setShowAddForm(false)}
                  />
                </div>
              )}

              {/* Transaction Grid */}
              {txLoading ? (
                <p className="text-slate-500">Loading transactions...</p>
              ) : (
                <TransactionGrid
                  transactions={transactions}
                  onCreate={handleCreateTransaction}
                  onUpdate={handleUpdateTransaction}
                  onDelete={handleDeleteTransaction}
                />
              )}
            </div>
          </section>
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
              Prices updated:{' '}
              {portfolio?.pricesUpdatedAt
                ? new Date(portfolio.pricesUpdatedAt).toLocaleTimeString()
                : '-'}
            </span>
          </div>
          {portfolio?.holdings.length ? (
            <PortfolioTable holdings={portfolio.holdings} />
          ) : (
            <p className="text-slate-500">No holdings yet. Add transactions to get started.</p>
          )}
        </section>
      </div>
    </div>
  )
}

export default Dashboard
