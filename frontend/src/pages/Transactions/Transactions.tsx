import { useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { DashboardNavigation } from '@components/layout'
import { useTransactions } from '../../hooks/useTransactions'
import { usePortfolio } from '../../hooks/usePortfolio'
import { TransactionGrid } from '../../components/transactions/TransactionGrid'
import { TransactionFilters } from '../../components/transactions/TransactionFilters'
import { TransactionPagination } from '../../components/transactions/TransactionPagination'
import { ImportModal } from '../../components/import'
import { QuickAddModal } from '../../components/transactions/QuickAddModal'
import { useModal } from '../../hooks/useModal'
import { formatCurrency } from '../../utils/stockFormatters'
import type { TransactionRequest } from '../../services/api/transactionApi'

const Transactions = () => {
  const {
    transactions,
    loading,
    updateTransaction,
    deleteTransaction,
    refresh: refreshTransactions,
  } = useTransactions()
  const { refresh } = usePortfolio()

  // Modal state
  const [isImportModalOpen, setIsImportModalOpen] = useState(false)
  const { isOpen: showQuickAdd, open: openQuickAdd, close: closeQuickAdd } = useModal()

  // Export state
  const [exporting, setExporting] = useState(false)

  // Filter state
  const [filterSymbol, setFilterSymbol] = useState<string>('')
  const [filterType, setFilterType] = useState<'ALL' | 'BUY' | 'SELL'>('ALL')
  const [searchQuery, setSearchQuery] = useState<string>('')

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(25)

  // Get unique symbols for filter
  const uniqueSymbols = useMemo(
    () => Array.from(new Set(transactions.map(tx => tx.symbol))).sort(),
    [transactions]
  )

  // Apply filters and search
  const filteredTransactions = useMemo(() => {
    return transactions.filter(tx => {
      // Symbol filter
      if (filterSymbol && tx.symbol !== filterSymbol) return false

      // Type filter
      if (filterType !== 'ALL' && tx.type !== filterType) return false

      // Search filter (symbol or notes)
      if (searchQuery) {
        const query = searchQuery.toLowerCase()
        const matchesSymbol = tx.symbol.toLowerCase().includes(query)
        const matchesNotes = tx.notes?.toLowerCase().includes(query)
        if (!matchesSymbol && !matchesNotes) return false
      }

      return true
    })
  }, [transactions, filterSymbol, filterType, searchQuery])

  // Calculate pagination
  const totalPages = Math.ceil(filteredTransactions.length / pageSize)
  const paginatedTransactions = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize
    return filteredTransactions.slice(startIndex, startIndex + pageSize)
  }, [filteredTransactions, currentPage, pageSize])

  // Calculate totals for filtered transactions
  const totalBought = useMemo(
    () =>
      filteredTransactions
        .filter(tx => tx.type === 'BUY')
        .reduce((sum, tx) => sum + tx.totalAmount, 0),
    [filteredTransactions]
  )

  const totalSold = useMemo(
    () =>
      filteredTransactions
        .filter(tx => tx.type === 'SELL')
        .reduce((sum, tx) => sum + tx.totalAmount, 0),
    [filteredTransactions]
  )

  // Reset to page 1 when filters change
  const handleFilterSymbolChange = (symbol: string) => {
    setFilterSymbol(symbol)
    setCurrentPage(1)
  }

  const handleFilterTypeChange = (type: 'ALL' | 'BUY' | 'SELL') => {
    setFilterType(type)
    setCurrentPage(1)
  }

  const handleSearchChange = (query: string) => {
    setSearchQuery(query)
    setCurrentPage(1)
  }

  const handlePageSizeChange = (size: number) => {
    setPageSize(size)
    setCurrentPage(1)
  }

  const handleUpdateTransaction = async (id: number, request: TransactionRequest) => {
    await updateTransaction(id, request)
    refresh()
  }

  const handleDeleteTransaction = async (id: number) => {
    await deleteTransaction(id)
    refresh()
  }

  const handleImportComplete = async () => {
    await refreshTransactions()
    refresh()
    setIsImportModalOpen(false)
  }

  const handleQuickAddSuccess = async () => {
    await refreshTransactions()
    refresh()
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const { transactionApi } = await import('../../services/api/transactionApi')
      const blob = await transactionApi.exportTransactions()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `transactions_${new Date().toISOString().split('T')[0]}.csv`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('Export failed:', error)
      // TODO: Show error notification if you have a toast system
    } finally {
      setExporting(false)
    }
  }

  const clearFilters = () => {
    setFilterSymbol('')
    setFilterType('ALL')
    setSearchQuery('')
    setCurrentPage(1)
  }

  const hasActiveFilters = filterSymbol || filterType !== 'ALL' || searchQuery

  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        {/* Header */}
        <div className="mb-6 sm:mb-8">
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900
                     mb-4 transition-colors"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
            Back to Dashboard
          </Link>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">Transactions</h1>
        </div>

        {/* Main Content Card */}
        <div className="rounded-xl border border-slate-200 bg-white shadow-soft">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <p className="text-slate-500">Loading transactions...</p>
            </div>
          ) : (
            <>
              {/* Toolbar */}
              <div className="border-b border-slate-200 p-4 sm:p-6 space-y-4">
                {/* Import/Export Buttons and Filters */}
                <div className="flex flex-col sm:flex-row sm:items-start gap-4">
                  <div className="flex gap-2">
                    <button
                      onClick={() => setIsImportModalOpen(true)}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg
                               hover:bg-indigo-700 transition-colors text-sm font-medium whitespace-nowrap"
                    >
                      <svg
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                        />
                      </svg>
                      Import CSV
                    </button>

                    <button
                      onClick={handleExport}
                      disabled={exporting || transactions.length === 0}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg
                               hover:bg-slate-200 transition-colors text-sm font-medium whitespace-nowrap
                               disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <svg
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
                        />
                      </svg>
                      {exporting ? 'Exporting...' : 'Export CSV'}
                    </button>
                  </div>

                  <div className="flex-1">
                    <TransactionFilters
                      symbols={uniqueSymbols}
                      filterSymbol={filterSymbol}
                      filterType={filterType}
                      searchQuery={searchQuery}
                      onFilterSymbolChange={handleFilterSymbolChange}
                      onFilterTypeChange={handleFilterTypeChange}
                      onSearchChange={handleSearchChange}
                    />
                  </div>
                </div>

                {/* Summary Row */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pt-2">
                  <div className="text-sm text-slate-600">
                    Showing {filteredTransactions.length} transaction
                    {filteredTransactions.length !== 1 ? 's' : ''}
                    {hasActiveFilters && (
                      <button
                        onClick={clearFilters}
                        className="ml-2 text-indigo-600 hover:text-indigo-700 underline"
                      >
                        Clear filters
                      </button>
                    )}
                  </div>
                  <div className="text-sm sm:text-base font-medium text-slate-700">
                    <span className="text-emerald-600">Bought: {formatCurrency(totalBought)}</span>
                    <span className="mx-2 text-slate-400">|</span>
                    <span className="text-red-600">Sold: {formatCurrency(totalSold)}</span>
                  </div>
                </div>
              </div>

              {/* Transaction Grid */}
              <div className="p-4 sm:p-6">
                {filteredTransactions.length === 0 ? (
                  <div className="text-center py-12">
                    {hasActiveFilters ? (
                      <>
                        <svg
                          className="mx-auto h-12 w-12 text-slate-300 mb-4"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                          />
                        </svg>
                        <p className="text-slate-600 mb-2">No transactions match your filters</p>
                        <button
                          onClick={clearFilters}
                          className="text-sm text-indigo-600 hover:text-indigo-700 underline"
                        >
                          Clear all filters
                        </button>
                      </>
                    ) : (
                      <>
                        <svg
                          className="mx-auto h-12 w-12 text-slate-300 mb-4"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                          />
                        </svg>
                        <p className="text-slate-600 mb-4">No transactions yet</p>
                        <button
                          onClick={openQuickAdd}
                          className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2
                                   text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
                        >
                          <svg
                            className="h-4 w-4"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M12 4v16m8-8H4"
                            />
                          </svg>
                          Add your first transaction
                        </button>
                      </>
                    )}
                  </div>
                ) : (
                  <TransactionGrid
                    transactions={paginatedTransactions}
                    onUpdate={handleUpdateTransaction}
                    onDelete={handleDeleteTransaction}
                    onCreate={async () => {}}
                    showToolbar={false}
                  />
                )}
              </div>

              {/* Pagination */}
              {filteredTransactions.length > 0 && (
                <div className="border-t border-slate-200 px-4 sm:px-6 pb-4 sm:pb-6">
                  <TransactionPagination
                    currentPage={currentPage}
                    totalPages={totalPages}
                    pageSize={pageSize}
                    totalItems={filteredTransactions.length}
                    onPageChange={setCurrentPage}
                    onPageSizeChange={handlePageSizeChange}
                  />
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Import Modal */}
      <ImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onImportComplete={handleImportComplete}
      />

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

export default Transactions
