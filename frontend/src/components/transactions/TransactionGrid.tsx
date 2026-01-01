import { useState, useCallback } from 'react'
import type { TransactionResponse, TransactionRequest } from '../../services/api/transactionApi'
import { TransactionGridRow } from './TransactionGridRow'
import { TransactionCard } from './TransactionCard'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionGridProps {
  transactions: TransactionResponse[]
  onUpdate: (id: number, request: TransactionRequest) => Promise<void>
  onDelete: (id: number) => Promise<void>
  onCreate: (request: TransactionRequest) => Promise<void>
}

type SortField = 'transactionDate' | 'symbol' | 'type' | 'totalAmount'
type SortDirection = 'asc' | 'desc'

interface SortIconProps {
  field: SortField
  sortField: SortField
  sortDirection: SortDirection
}

function SortIcon({ field, sortField, sortDirection }: SortIconProps) {
  if (sortField !== field) return <span className="ml-1 text-slate-300">↕</span>
  return <span className="ml-1">{sortDirection === 'asc' ? '↑' : '↓'}</span>
}

export function TransactionGrid({
  transactions,
  onUpdate,
  onDelete,
}: TransactionGridProps) {
  const [editingId, setEditingId] = useState<number | null>(null)
  const [sortField, setSortField] = useState<SortField>('transactionDate')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [filterSymbol, setFilterSymbol] = useState<string>('')

  // Get unique symbols for filter dropdown
  const uniqueSymbols = Array.from(new Set(transactions.map(tx => tx.symbol))).sort()

  // Sort and filter transactions
  const sortedTransactions = [...transactions]
    .filter(tx => !filterSymbol || tx.symbol === filterSymbol)
    .sort((a, b) => {
      let comparison = 0
      switch (sortField) {
        case 'transactionDate':
          comparison = new Date(a.transactionDate).getTime() - new Date(b.transactionDate).getTime()
          break
        case 'symbol':
          comparison = a.symbol.localeCompare(b.symbol)
          break
        case 'type':
          comparison = a.type.localeCompare(b.type)
          break
        case 'totalAmount':
          comparison = a.totalAmount - b.totalAmount
          break
      }
      return sortDirection === 'asc' ? comparison : -comparison
    })

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(prev => (prev === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDirection('desc')
    }
  }

  const handleSave = useCallback(
    async (id: number, request: TransactionRequest) => {
      await onUpdate(id, request)
      setEditingId(null)
    },
    [onUpdate]
  )

  const handleDelete = useCallback(
    async (id: number) => {
      if (window.confirm('Are you sure you want to delete this transaction?')) {
        await onDelete(id)
      }
    },
    [onDelete]
  )

  // Calculate totals
  const totalBuys = sortedTransactions
    .filter(tx => tx.type === 'BUY')
    .reduce((sum, tx) => sum + tx.totalAmount, 0)
  const totalSells = sortedTransactions
    .filter(tx => tx.type === 'SELL')
    .reduce((sum, tx) => sum + tx.totalAmount, 0)

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <select
            value={filterSymbol}
            onChange={e => setFilterSymbol(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">All</option>
            {uniqueSymbols.map(symbol => (
              <option key={symbol} value={symbol}>
                {symbol}
              </option>
            ))}
          </select>
          <span className="text-sm text-slate-500">
            {sortedTransactions.length} transaction{sortedTransactions.length !== 1 ? 's' : ''}
          </span>
        </div>
        <div className="text-sm text-slate-600">
          <span className="text-green-600">Bought: {formatCurrency(totalBuys)}</span>
          <span className="mx-2">|</span>
          <span className="text-red-600">Sold: {formatCurrency(totalSells)}</span>
        </div>
      </div>

      {/* Mobile Card View */}
      <div className="md:hidden space-y-3">
        {sortedTransactions.map(transaction => (
          <TransactionCard
            key={transaction.id}
            transaction={transaction}
            isEditing={editingId === transaction.id}
            onEdit={() => setEditingId(transaction.id)}
            onSave={request => handleSave(transaction.id, request)}
            onCancel={() => setEditingId(null)}
            onDelete={() => handleDelete(transaction.id)}
          />
        ))}
      </div>

      {/* Desktop Table View */}
      <div className="hidden md:block overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-soft">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th
                onClick={() => handleSort('type')}
                className="cursor-pointer px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Type <SortIcon field="type" sortField={sortField} sortDirection={sortDirection} />
              </th>
              <th
                onClick={() => handleSort('symbol')}
                className="cursor-pointer px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Ticker{' '}
                <SortIcon field="symbol" sortField={sortField} sortDirection={sortDirection} />
              </th>
              <th
                onClick={() => handleSort('transactionDate')}
                className="cursor-pointer px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Date{' '}
                <SortIcon
                  field="transactionDate"
                  sortField={sortField}
                  sortDirection={sortDirection}
                />
              </th>
              <th className="px-4 py-3 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
                Shares
              </th>
              <th className="px-4 py-3 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
                Price
              </th>
              <th
                onClick={() => handleSort('totalAmount')}
                className="cursor-pointer px-4 py-3 text-right text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Total{' '}
                <SortIcon field="totalAmount" sortField={sortField} sortDirection={sortDirection} />
              </th>
              <th className="px-4 py-3 text-center text-sm font-semibold uppercase tracking-wide text-slate-600">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sortedTransactions.map(transaction => (
              <TransactionGridRow
                key={transaction.id}
                transaction={transaction}
                isEditing={editingId === transaction.id}
                onEdit={() => setEditingId(transaction.id)}
                onSave={request => handleSave(transaction.id, request)}
                onCancel={() => setEditingId(null)}
                onDelete={() => handleDelete(transaction.id)}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
