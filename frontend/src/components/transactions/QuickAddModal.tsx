import { useState, useEffect } from 'react'
import { Modal } from '../common/Modal'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'
import type { TransactionRequest, TransactionType } from '../../services/api/transactionApi'

export interface QuickAddModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export function QuickAddModal({ isOpen, onClose, onSuccess }: QuickAddModalProps) {
  const [type, setType] = useState<TransactionType>('BUY')
  const [symbol, setSymbol] = useState('')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [shares, setShares] = useState('')
  const [price, setPrice] = useState('')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isClosing, setIsClosing] = useState(false)

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

  // Calculate total amount
  const totalAmount = shares && price ? parseFloat(shares) * parseFloat(price) : 0

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      // Delay reset to allow closing animation
      setTimeout(() => {
        setType('BUY')
        setSymbol('')
        setDate(new Date().toISOString().split('T')[0])
        setShares('')
        setPrice('')
        setNotes('')
        setError(null)
        clearValidation()
        setIsClosing(false)
      }, 200)
    }
  }, [isOpen, clearValidation])

  const handleClose = () => {
    setIsClosing(true)
    setTimeout(() => {
      onClose()
    }, 150)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    // Validation
    if (!symbol || !date || !shares || !price) {
      setError('All fields are required except notes')
      return
    }

    if (validation && !validation.valid) {
      setError(validation.errorMessage || 'Invalid ticker symbol')
      return
    }

    const sharesNum = parseFloat(shares)
    const priceNum = parseFloat(price)

    if (isNaN(sharesNum) || sharesNum <= 0) {
      setError('Shares must be a positive number')
      return
    }

    if (isNaN(priceNum) || priceNum <= 0) {
      setError('Price must be a positive number')
      return
    }

    if (new Date(date) > new Date()) {
      setError('Date cannot be in the future')
      return
    }

    try {
      setSubmitting(true)
      const request: TransactionRequest = {
        type,
        symbol: symbol.toUpperCase(),
        transactionDate: date,
        shares: sharesNum,
        pricePerShare: priceNum,
        notes: notes || undefined,
      }

      // Use the transaction API to create the transaction
      const { transactionApi } = await import('../../services/api/transactionApi')
      await transactionApi.createTransaction(request)

      // Success - close modal and notify parent
      handleClose()
      onSuccess()
    } catch (err) {
      const error = err as { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to create transaction')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      isClosing={isClosing}
      title="Add Transaction"
      size="xl"
      showCloseButton={true}
      closeOnEscape={true}
      closeOnBackdropClick={true}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</div>}

        {/* Two-column grid for form fields */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Type */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Type</label>
            <select
              value={type}
              onChange={e => setType(e.target.value as TransactionType)}
              className="block w-full rounded-lg border border-slate-300 px-3 py-2
                       focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              autoFocus
            >
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
            </select>
          </div>

          {/* Ticker */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Ticker</label>
            <div className="relative">
              <input
                type="text"
                value={symbol}
                onChange={e => setSymbol(e.target.value.toUpperCase())}
                placeholder="AAPL"
                className={`block w-full rounded-lg border px-3 py-2 uppercase
                         focus:outline-none focus:ring-2
                         ${
                           validation?.valid === false
                             ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                             : validation?.valid
                               ? 'border-green-300 focus:border-green-500 focus:ring-green-500'
                               : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500'
                         }`}
              />
              {validating && (
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                  ...
                </span>
              )}
            </div>
            {validation?.valid && validation.companyName && (
              <p className="mt-1 flex items-center text-xs text-green-600">
                <svg className="mr-1 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M5 13l4 4L19 7"
                  />
                </svg>
                {validation.companyName}
              </p>
            )}
            {validation?.valid === false && (
              <p className="mt-1 text-xs text-red-600">{validation.errorMessage}</p>
            )}
          </div>

          {/* Date */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Date</label>
            <input
              type="date"
              value={date}
              onChange={e => setDate(e.target.value)}
              max={new Date().toISOString().split('T')[0]}
              className="block w-full rounded-lg border border-slate-300 px-3 py-2
                       focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Shares */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Shares</label>
            <input
              type="number"
              value={shares}
              onChange={e => setShares(e.target.value)}
              placeholder="100"
              min="0.0001"
              step="0.0001"
              className="block w-full rounded-lg border border-slate-300 px-3 py-2
                       focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Price */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Price</label>
            <input
              type="number"
              value={price}
              onChange={e => setPrice(e.target.value)}
              placeholder="150.00"
              min="0.01"
              step="0.01"
              className="block w-full rounded-lg border border-slate-300 px-3 py-2
                       focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>

        {/* Total (calculated) */}
        <div className="rounded-lg border-2 border-slate-200 bg-slate-50 px-4 py-3">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-slate-600">Total</span>
            <span className="text-xl font-bold text-slate-900">{formatCurrency(totalAmount)}</span>
          </div>
        </div>

        {/* Notes (optional) */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Notes (optional)</label>
          <textarea
            value={notes}
            onChange={e => setNotes(e.target.value)}
            placeholder="Optional notes..."
            maxLength={500}
            rows={2}
            className="block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500
                     resize-none"
          />
        </div>

        {/* Actions */}
        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={handleClose}
            className="flex-1 rounded-lg border border-slate-300 px-4 py-2 font-medium text-slate-700
                     hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting || validating || validation?.valid === false}
            className="flex-1 rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white
                     hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50
                     transition-colors"
          >
            {submitting ? 'Adding...' : 'Add Transaction'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
