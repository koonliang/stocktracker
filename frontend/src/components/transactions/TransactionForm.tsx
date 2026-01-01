import { useState, useEffect } from 'react'
import type { TransactionRequest, TransactionType } from '../../services/api/transactionApi'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionFormProps {
  onSubmit: (request: TransactionRequest) => Promise<void>
  onCancel?: () => void
  initialData?: Partial<TransactionRequest>
  isEditing?: boolean
}

export function TransactionForm({
  onSubmit,
  onCancel,
  initialData,
  isEditing = false,
}: TransactionFormProps) {
  const [type, setType] = useState<TransactionType>(initialData?.type || 'BUY')
  const [symbol, setSymbol] = useState(initialData?.symbol || '')
  const [date, setDate] = useState(
    initialData?.transactionDate || new Date().toISOString().split('T')[0]
  )
  const [shares, setShares] = useState(initialData?.shares?.toString() || '')
  const [price, setPrice] = useState(initialData?.pricePerShare?.toString() || '')
  const [notes, setNotes] = useState(initialData?.notes || '')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { validating, validation, validateTicker, clearValidation } = useTickerValidation()

  // Debounced ticker validation
  useEffect(() => {
    const timer = setTimeout(() => {
      if (symbol.length >= 1) {
        validateTicker(symbol)
      } else {
        clearValidation()
      }
    }, 500)

    return () => clearTimeout(timer)
  }, [symbol, validateTicker, clearValidation])

  const totalAmount = shares && price ? parseFloat(shares) * parseFloat(price) : 0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    // Validation
    if (!symbol || !date || !shares || !price) {
      setError('All fields are required')
      return
    }

    if (validation && !validation.valid) {
      setError(validation.errorMessage || 'Invalid ticker symbol')
      return
    }

    try {
      setSubmitting(true)
      await onSubmit({
        type,
        symbol: symbol.toUpperCase(),
        transactionDate: date,
        shares: parseFloat(shares),
        pricePerShare: parseFloat(price),
        notes: notes || undefined,
      })

      // Reset form if not editing
      if (!isEditing) {
        setSymbol('')
        setShares('')
        setPrice('')
        setNotes('')
        clearValidation()
      }
    } catch (err) {
      const error = err as { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to save transaction')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</div>}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
        {/* Type */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Type</label>
          <select
            value={type}
            onChange={e => setType(e.target.value as TransactionType)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </div>

        {/* Ticker */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Ticker</label>
          <div className="relative">
            <input
              type="text"
              value={symbol}
              onChange={e => setSymbol(e.target.value.toUpperCase())}
              placeholder="AAPL"
              className={`mt-1 block w-full rounded-lg border px-3 py-2 uppercase
                       focus:outline-none focus:ring-1
                       ${
                         validation?.valid === false
                           ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                           : validation?.valid
                             ? 'border-green-300 focus:border-green-500 focus:ring-green-500'
                             : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500'
                       }`}
            />
            {validating && (
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">...</span>
            )}
          </div>
          {validation?.valid && validation.companyName && (
            <p className="mt-1 text-xs text-green-600">{validation.companyName}</p>
          )}
          {validation?.valid === false && (
            <p className="mt-1 text-xs text-red-600">{validation.errorMessage}</p>
          )}
        </div>

        {/* Date */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Date</label>
          <input
            type="date"
            value={date}
            onChange={e => setDate(e.target.value)}
            max={new Date().toISOString().split('T')[0]}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Shares */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Shares</label>
          <input
            type="number"
            value={shares}
            onChange={e => setShares(e.target.value)}
            placeholder="100"
            min="0.0001"
            step="0.0001"
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Price */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Price</label>
          <input
            type="number"
            value={price}
            onChange={e => setPrice(e.target.value)}
            placeholder="150.00"
            min="0.01"
            step="0.01"
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Total (calculated) */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Total</label>
          <div className="mt-1 rounded-lg bg-slate-100 px-3 py-2 font-medium text-slate-700">
            {formatCurrency(totalAmount)}
          </div>
        </div>
      </div>

      {/* Notes (optional) */}
      <div>
        <label className="block text-sm font-medium text-slate-700">Notes (optional)</label>
        <input
          type="text"
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="Optional notes..."
          maxLength={500}
          className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                   focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>

      {/* Actions */}
      <div className="flex gap-3">
        <button
          type="submit"
          disabled={submitting || validating}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white
                   hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? 'Saving...' : isEditing ? 'Update' : 'Add Transaction'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg border border-slate-300 px-4 py-2 font-medium text-slate-700
                     hover:bg-slate-50"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
