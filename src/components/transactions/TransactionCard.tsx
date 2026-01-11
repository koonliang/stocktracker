import { useState, useEffect } from 'react'
import type {
  TransactionResponse,
  TransactionRequest,
  TransactionType,
} from '../../services/api/transactionApi'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionCardProps {
  transaction: TransactionResponse
  isEditing: boolean
  onEdit: () => void
  onSave: (request: TransactionRequest) => Promise<void>
  onCancel: () => void
  onDelete: () => void
}

export function TransactionCard({
  transaction,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onDelete,
}: TransactionCardProps) {
  const [type, setType] = useState<TransactionType>(transaction.type)
  const [symbol, setSymbol] = useState(transaction.symbol)
  const [date, setDate] = useState(transaction.transactionDate)
  const [shares, setShares] = useState(transaction.shares.toString())
  const [price, setPrice] = useState(transaction.pricePerShare.toString())
  const [brokerFee, setBrokerFee] = useState(transaction.brokerFee?.toString() || '')
  const [saving, setSaving] = useState(false)

  const { validating, validation, validateTicker } = useTickerValidation()

  // Sync state when transaction changes or editing starts
  useEffect(() => {
    if (isEditing) {
      setType(transaction.type)
      setSymbol(transaction.symbol)
      setDate(transaction.transactionDate)
      setShares(transaction.shares.toString())
      setPrice(transaction.pricePerShare.toString())
      setBrokerFee(transaction.brokerFee?.toString() || '')
    }
  }, [isEditing, transaction])

  const handleSymbolChange = async (value: string) => {
    const upperValue = value.toUpperCase()
    setSymbol(upperValue)
    if (upperValue !== transaction.symbol) {
      await validateTicker(upperValue)
    }
  }

  const handleSave = async () => {
    if (symbol !== transaction.symbol && validation && !validation.valid) return
    setSaving(true)
    try {
      await onSave({
        type,
        symbol,
        transactionDate: date,
        shares: parseFloat(shares),
        pricePerShare: parseFloat(price),
        brokerFee: brokerFee ? parseFloat(brokerFee) : undefined,
      })
    } finally {
      setSaving(false)
    }
  }

  if (!isEditing) {
    // Display Mode Card
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold
              ${transaction.type === 'BUY' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}
            >
              {transaction.type}
            </span>
            <div>
              <span className="font-semibold text-slate-900">{transaction.symbol}</span>
              <span className="text-xs text-slate-500 ml-1 hidden sm:inline">
                {transaction.companyName}
              </span>
            </div>
          </div>
          <div className="text-right">
            <div className="font-semibold text-slate-900">
              {formatCurrency(transaction.totalAmount)}
            </div>
            <div className="text-xs text-slate-500">
              {new Date(transaction.transactionDate).toLocaleDateString()}
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between text-sm">
          <div className="text-slate-600">
            <span className="font-medium">
              {transaction.shares.toFixed(4).replace(/\.?0+$/, '')}
            </span>
            <span className="text-slate-400"> shares @ </span>
            <span className="font-medium">{formatCurrency(transaction.pricePerShare)}</span>
            {transaction.brokerFee && transaction.brokerFee > 0 && (
              <>
                <span className="text-slate-400"> + </span>
                <span className="font-medium">{formatCurrency(transaction.brokerFee)}</span>
                <span className="text-slate-400"> fee</span>
              </>
            )}
          </div>
          <div className="flex gap-2">
            <button
              onClick={onEdit}
              className="rounded px-2 py-1 text-sm text-indigo-600 hover:bg-indigo-50"
            >
              Edit
            </button>
            <button
              onClick={onDelete}
              className="rounded px-2 py-1 text-sm text-red-600 hover:bg-red-50"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    )
  }

  // Edit Mode Card
  const totalAmount =
    parseFloat(shares || '0') * parseFloat(price || '0') + parseFloat(brokerFee || '0')

  return (
    <div className="rounded-xl border-2 border-indigo-300 bg-indigo-50 p-4">
      <div className="space-y-3">
        {/* Type & Symbol Row */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Type</label>
            <select
              value={type}
              onChange={e => setType(e.target.value as TransactionType)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Symbol</label>
            <input
              type="text"
              value={symbol}
              onChange={e => handleSymbolChange(e.target.value)}
              className={`w-full rounded border px-3 py-2 text-sm uppercase
                ${validation?.valid === false ? 'border-red-300' : 'border-slate-300'}`}
            />
            {validating && <span className="text-xs text-slate-400">Validating...</span>}
            {validation?.valid === false && (
              <span className="text-xs text-red-500">{validation.errorMessage}</span>
            )}
          </div>
        </div>

        {/* Date Row */}
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Date</label>
          <input
            type="date"
            value={date}
            onChange={e => setDate(e.target.value)}
            max={new Date().toISOString().split('T')[0]}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        {/* Shares & Price Row */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Shares</label>
            <input
              type="number"
              value={shares}
              onChange={e => setShares(e.target.value)}
              min="0.0001"
              step="0.0001"
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Price</label>
            <input
              type="number"
              value={price}
              onChange={e => setPrice(e.target.value)}
              min="0.01"
              step="0.01"
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
        </div>

        {/* Broker Fee Row */}
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">
            Broker Fee (Optional)
          </label>
          <input
            type="number"
            value={brokerFee}
            onChange={e => setBrokerFee(e.target.value)}
            min="0"
            step="0.01"
            placeholder="0.00"
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        {/* Total & Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-indigo-200">
          <div className="text-sm">
            <span className="text-slate-600">Total: </span>
            <span className="font-semibold text-slate-900">{formatCurrency(totalAmount)}</span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={onCancel}
              className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-white"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={saving || validating}
              className="rounded bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
