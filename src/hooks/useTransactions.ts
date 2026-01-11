import { useState, useEffect, useCallback } from 'react'
import { transactionApi } from '../services/api/transactionApi'
import type {
  TransactionResponse,
  TransactionRequest,
  TickerValidationResponse,
} from '../services/api/transactionApi'

export function useTransactions() {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchTransactions = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await transactionApi.getTransactions()
      setTransactions(data)
    } catch (err) {
      setError('Failed to load transactions')
      console.error('Error fetching transactions:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchTransactions()
  }, [fetchTransactions])

  const createTransaction = useCallback(async (request: TransactionRequest) => {
    const response = await transactionApi.createTransaction(request)
    setTransactions(prev => [response, ...prev])
    return response
  }, [])

  const updateTransaction = useCallback(async (id: number, request: TransactionRequest) => {
    const response = await transactionApi.updateTransaction(id, request)
    setTransactions(prev => prev.map(tx => (tx.id === id ? response : tx)))
    return response
  }, [])

  const deleteTransaction = useCallback(async (id: number) => {
    await transactionApi.deleteTransaction(id)
    setTransactions(prev => prev.filter(tx => tx.id !== id))
  }, [])

  return {
    transactions,
    loading,
    error,
    refresh: fetchTransactions,
    createTransaction,
    updateTransaction,
    deleteTransaction,
  }
}

export function useTickerValidation() {
  const [validating, setValidating] = useState(false)
  const [validation, setValidation] = useState<TickerValidationResponse | null>(null)

  const validateTicker = useCallback(async (symbol: string) => {
    if (!symbol || symbol.length < 1) {
      setValidation(null)
      return null
    }

    try {
      setValidating(true)
      const result = await transactionApi.validateTicker(symbol)
      setValidation(result)
      return result
    } catch {
      const errorResult: TickerValidationResponse = {
        valid: false,
        symbol,
        companyName: null,
        errorMessage: 'Failed to validate ticker',
      }
      setValidation(errorResult)
      return errorResult
    } finally {
      setValidating(false)
    }
  }, [])

  const clearValidation = useCallback(() => {
    setValidation(null)
  }, [])

  return {
    validating,
    validation,
    validateTicker,
    clearValidation,
  }
}
