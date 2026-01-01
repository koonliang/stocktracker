import { useState } from 'react'
import type {
  TransactionResponse,
  TransactionRequest,
  TransactionType,
} from '../../services/api/transactionApi'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionGridRowProps {
  transaction: TransactionResponse
  isEditing: boolean
  onEdit: () => void
  onSave: (request: TransactionRequest) => Promise<void>
  onCancel: () => void
  onDelete: () => void
}

export function TransactionGridRow({
  transaction,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onDelete,
}: TransactionGridRowProps) {
  const [type, setType] = useState<TransactionType>(transaction.type)
  const [symbol, setSymbol] = useState(transaction.symbol)
  const [date, setDate] = useState(transaction.transactionDate)
  const [shares, setShares] = useState(transaction.shares.toString())
  const [price, setPrice] = useState(transaction.pricePerShare.toString())
  const [saving, setSaving] = useState(false)

  const { validating, validation, validateTicker } = useTickerValidation()

  const handleSymbolChange = async (value: string) => {
    const upperValue = value.toUpperCase()
    setSymbol(upperValue)
    if (upperValue !== transaction.symbol) {
      await validateTicker(upperValue)
    }
  }

  const handleSave = async () => {
    // Validate if symbol changed
    if (symbol !== transaction.symbol && validation && !validation.valid) {
      return
    }

    setSaving(true)
    try {
      await onSave({
        type,
        symbol,
        transactionDate: date,
        shares: parseFloat(shares),
        pricePerShare: parseFloat(price),
      })
    } finally {
      setSaving(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSave()
    } else if (e.key === 'Escape') {
      onCancel()
    }
  }

  if (!isEditing) {
    // Display mode
    return (
      <tr className="transition-colors hover:bg-slate-50">
        <td className="px-4 py-3">
          <span
            className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold
            ${
              transaction.type === 'BUY' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
            }`}
          >
            {transaction.type}
          </span>
        </td>
        <td className="px-4 py-3">
          <div className="font-medium text-slate-900">{transaction.symbol}</div>
          <div className="text-xs text-slate-500">{transaction.companyName}</div>
        </td>
        <td className="px-4 py-3 text-slate-700">
          {new Date(transaction.transactionDate).toLocaleDateString()}
        </td>
        <td className="px-4 py-3 text-right font-medium text-slate-900">
          {transaction.shares.toFixed(4).replace(/\.?0+$/, '')}
        </td>
        <td className="px-4 py-3 text-right font-medium text-slate-900">
          {formatCurrency(transaction.pricePerShare)}
        </td>
        <td
          className={`px-4 py-3 text-right font-semibold
          ${transaction.type === 'BUY' ? 'text-slate-900' : 'text-slate-900'}`}
        >
          {formatCurrency(transaction.totalAmount)}
        </td>
        <td className="px-4 py-3 text-center">
          <button
            onClick={onEdit}
            className="mr-2 rounded px-2 py-1 text-sm text-indigo-600 hover:bg-indigo-50"
          >
            Edit
          </button>
          <button
            onClick={onDelete}
            className="rounded px-2 py-1 text-sm text-red-600 hover:bg-red-50"
          >
            Delete
          </button>
        </td>
      </tr>
    )
  }

  // Edit mode
  const totalAmount = parseFloat(shares || '0') * parseFloat(price || '0')

  return (
    <tr className="bg-indigo-50" onKeyDown={handleKeyDown}>
      <td className="px-4 py-3">
        <select
          value={type}
          onChange={e => setType(e.target.value as TransactionType)}
          className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
        >
          <option value="BUY">BUY</option>
          <option value="SELL">SELL</option>
        </select>
      </td>
      <td className="px-4 py-3">
        <input
          type="text"
          value={symbol}
          onChange={e => handleSymbolChange(e.target.value)}
          className={`w-full rounded border px-2 py-1 text-sm uppercase
            ${validation?.valid === false ? 'border-red-300' : 'border-slate-300'}`}
        />
        {validating && <span className="text-xs text-slate-400">Validating...</span>}
        {validation?.valid === false && (
          <span className="text-xs text-red-500">{validation.errorMessage}</span>
        )}
      </td>
      <td className="px-4 py-3">
        <input
          type="date"
          value={date}
          onChange={e => setDate(e.target.value)}
          max={new Date().toISOString().split('T')[0]}
          className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
        />
      </td>
      <td className="px-4 py-3">
        <input
          type="number"
          value={shares}
          onChange={e => setShares(e.target.value)}
          min="0.0001"
          step="0.0001"
          className="w-full rounded border border-slate-300 px-2 py-1 text-right text-sm"
        />
      </td>
      <td className="px-4 py-3">
        <input
          type="number"
          value={price}
          onChange={e => setPrice(e.target.value)}
          min="0.01"
          step="0.01"
          className="w-full rounded border border-slate-300 px-2 py-1 text-right text-sm"
        />
      </td>
      <td className="px-4 py-3 text-right font-medium text-slate-700">
        {formatCurrency(totalAmount)}
      </td>
      <td className="px-4 py-3 text-center">
        <button
          onClick={handleSave}
          disabled={saving || validating}
          className="mr-2 rounded bg-indigo-600 px-2 py-1 text-sm text-white hover:bg-indigo-700
                   disabled:opacity-50"
        >
          {saving ? '...' : 'Save'}
        </button>
        <button
          onClick={onCancel}
          className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-600 hover:bg-slate-100"
        >
          Cancel
        </button>
      </td>
    </tr>
  )
}
