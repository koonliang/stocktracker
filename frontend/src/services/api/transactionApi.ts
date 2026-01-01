import axiosInstance from './axiosInstance'

export type TransactionType = 'BUY' | 'SELL'

export interface TransactionRequest {
  type: TransactionType
  symbol: string
  transactionDate: string // ISO date string (YYYY-MM-DD)
  shares: number
  pricePerShare: number
  notes?: string
}

export interface TransactionResponse {
  id: number
  type: TransactionType
  symbol: string
  companyName: string
  transactionDate: string
  shares: number
  pricePerShare: number
  totalAmount: number
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface TickerValidationResponse {
  valid: boolean
  symbol: string
  companyName: string | null
  errorMessage: string | null
}

export const transactionApi = {
  /**
   * Get all transactions for the authenticated user.
   */
  getTransactions: async (): Promise<TransactionResponse[]> => {
    const response = await axiosInstance.get('/transactions')
    return response.data.data
  },

  /**
   * Validate a ticker symbol.
   */
  validateTicker: async (symbol: string): Promise<TickerValidationResponse> => {
    const response = await axiosInstance.get(
      `/transactions/validate-ticker?symbol=${encodeURIComponent(symbol)}`
    )
    return response.data.data
  },

  /**
   * Create a new transaction.
   */
  createTransaction: async (request: TransactionRequest): Promise<TransactionResponse> => {
    const response = await axiosInstance.post('/transactions', request)
    return response.data.data
  },

  /**
   * Update an existing transaction.
   */
  updateTransaction: async (
    id: number,
    request: TransactionRequest
  ): Promise<TransactionResponse> => {
    const response = await axiosInstance.put(`/transactions/${id}`, request)
    return response.data.data
  },

  /**
   * Delete a transaction.
   */
  deleteTransaction: async (id: number): Promise<void> => {
    await axiosInstance.delete(`/transactions/${id}`)
  },
}
