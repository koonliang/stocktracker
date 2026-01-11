'use client'

import { useState, useMemo } from 'react'
import Link from 'next/link'
import { DashboardNavigation } from '@components/layout'
import { useTransactions } from '@hooks/useTransactions'
import { usePortfolio } from '@hooks/usePortfolio'
import { TransactionGrid } from '@components/transactions/TransactionGrid'
import { TransactionFilters } from '@components/transactions/TransactionFilters'
import { TransactionPagination } from '@components/transactions/TransactionPagination'
import { ImportModal } from '@components/import'
import { QuickAddModal } from '@components/transactions/QuickAddModal'
import { useModal } from '@hooks/useModal'
import { formatCurrency } from '@utils/stockFormatters'
import type { TransactionRequest } from '@services/api/transactionApi'

export default function Transactions() {
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

  const handleSearchQueryChange = (query: string) => {
    setSearchQuery(query)
    setCurrentPage(1)
  }

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
  }

  const handlePageSizeChange = (size: number) => {
    setPageSize(size)
    setCurrentPage(1) // Reset to first page when page size changes
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      // Create CSV content
      const headers = ['Date', 'Symbol', 'Type', 'Quantity', 'Price', 'Fee', 'Total', 'Notes']
      const csvRows = [
        headers.join(','),
        ...filteredTransactions.map(tx => {
          const date = new Date(tx.transactionDate).toLocaleDateString()
          const notes = tx.notes ? `"${tx.notes.replace(/"/g, '""')}"` : ''
          return [date, tx.symbol, tx.type, tx.quantity, tx.price, tx.fee, tx.totalAmount, notes].join(',')
        }),
      ]

      const csvContent = csvRows.join('\n')
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)

      link.setAttribute('href', url)
      link.setAttribute('download', `transactions_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    } catch (error) {
      console.error('Export failed:', error)
      alert('Failed to export transactions')
    } finally {
      setExporting(false)
    }
  }

  const handleUpdate = async (id: number, data: TransactionRequest) => {
    await updateTransaction(id, data)
    refresh() // Refresh portfolio after update
  }

  const handleDelete = async (id: number) => {
    if (confirm('Are you sure you want to delete this transaction?')) {
      await deleteTransaction(id)
      refresh() // Refresh portfolio after delete
    }
  }

  const handleImportSuccess = () => {
    refreshTransactions()
    refresh()
    setIsImportModalOpen(false)
  }

  const handleQuickAddSuccess = () => {
    refreshTransactions()
    refresh()
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="flex justify-center p-8">
          <p className="text-slate-600">Loading transactions...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <Link
              href="/dashboard"
              className="inline-flex items-center text-sm text-slate-600 hover:text-slate-900 mb-2"
            >
              <svg
                className="w-4 h-4 mr-1"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 19l-7-7 7-7"
                />
              </svg>
              Back to Dashboard
            </Link>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900">Transaction History</h1>
          </div>
          <div className="flex flex-wrap gap-2 sm:gap-3">
            <button
              onClick={() => setIsImportModalOpen(true)}
              className="flex-1 sm:flex-none rounded-lg bg-slate-100 px-3 sm:px-4 py-2 text-sm sm:text-base font-medium text-slate-700 transition-colors hover:bg-slate-200"
            >
              Import CSV
            </button>
            <button
              onClick={handleExport}
              disabled={exporting || filteredTransactions.length === 0}
              className="flex-1 sm:flex-none rounded-lg bg-slate-100 px-3 sm:px-4 py-2 text-sm sm:text-base font-medium text-slate-700 transition-colors hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {exporting ? 'Exporting...' : 'Export CSV'}
            </button>
            <button
              onClick={openQuickAdd}
              className="flex-1 sm:flex-none rounded-lg bg-indigo-600 px-3 sm:px-4 py-2 text-sm sm:text-base text-white hover:bg-indigo-700 font-medium"
            >
              Add Transaction
            </button>
          </div>
        </header>

        {/* Summary Stats */}
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
            <p className="text-sm font-medium text-slate-600">Total Transactions</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{filteredTransactions.length}</p>
          </div>
          <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
            <p className="text-sm font-medium text-slate-600">Total Bought</p>
            <p className="mt-2 text-2xl font-bold text-green-600">{formatCurrency(totalBought)}</p>
          </div>
          <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
            <p className="text-sm font-medium text-slate-600">Total Sold</p>
            <p className="mt-2 text-2xl font-bold text-red-600">{formatCurrency(totalSold)}</p>
          </div>
        </div>

        {/* Filters */}
        <TransactionFilters
          symbols={uniqueSymbols}
          filterSymbol={filterSymbol}
          filterType={filterType}
          searchQuery={searchQuery}
          onFilterSymbolChange={handleFilterSymbolChange}
          onFilterTypeChange={handleFilterTypeChange}
          onSearchChange={handleSearchQueryChange}
        />

        {/* Transaction Grid */}
        <TransactionGrid
          transactions={paginatedTransactions}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
          showToolbar={false}
        />

        {/* Pagination */}
        {filteredTransactions.length > 0 && (
          <TransactionPagination
            currentPage={currentPage}
            totalPages={totalPages}
            pageSize={pageSize}
            totalItems={filteredTransactions.length}
            onPageChange={handlePageChange}
            onPageSizeChange={handlePageSizeChange}
          />
        )}

        {/* Empty state */}
        {filteredTransactions.length === 0 && (
          <div className="rounded-xl border border-slate-200 bg-white p-12 text-center shadow-soft">
            <p className="text-slate-500">
              {transactions.length === 0
                ? 'No transactions yet. Add your first transaction to get started.'
                : 'No transactions match your filters.'}
            </p>
          </div>
        )}
      </div>

      {/* Import Modal */}
      <ImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onImportComplete={handleImportSuccess}
      />

      {/* Quick Add Modal */}
      <QuickAddModal
        isOpen={showQuickAdd}
        onClose={closeQuickAdd}
        onSuccess={handleQuickAddSuccess}
      />
    </div>
  )
}
