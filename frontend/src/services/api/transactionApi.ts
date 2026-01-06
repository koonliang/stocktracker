import axiosInstance from './axiosInstance'

export type TransactionType = 'BUY' | 'SELL'

export interface TransactionRequest {
  type: TransactionType
  symbol: string
  transactionDate: string // ISO date string (YYYY-MM-DD)
  shares: number
  pricePerShare: number
  brokerFee?: number
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
  brokerFee: number | null
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

// CSV Import Types
export interface CsvRowData {
  values: Record<string, string>
  rowNumber: number
}

export interface CsvMappingSuggestionRequest {
  headers: string[]
}

export interface CsvMappingSuggestionResponse {
  suggestedMappings: Record<string, string>
  confidenceScores: Record<string, number>
  unmappedColumns: string[]
}

export interface CsvImportRequest {
  rows: CsvRowData[]
  fieldMappings: Record<string, string>
}

export interface CsvImportError {
  rowNumber: number
  field: string
  message: string
  rejectedValue: string | null
}

export interface TransactionPreviewRow {
  rowNumber: number
  type: TransactionType | null
  symbol: string | null
  transactionDate: string | null
  shares: number | null
  pricePerShare: number | null
  brokerFee: number | null
  notes: string | null
  valid: boolean
  errors: CsvImportError[]
}

export interface CsvImportPreviewResponse {
  validRows: TransactionPreviewRow[]
  errorRows: TransactionPreviewRow[]
  totalRows: number
  validCount: number
  errorCount: number
}

export interface CsvImportResultResponse {
  importedCount: number
  skippedCount: number
  errors: CsvImportError[]
  importedTransactions: TransactionResponse[]
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

  /**
   * Suggest field mappings for CSV import based on headers.
   */
  suggestMapping: async (
    request: CsvMappingSuggestionRequest
  ): Promise<CsvMappingSuggestionResponse> => {
    const response = await axiosInstance.post('/transactions/import/suggest-mapping', request)
    return response.data.data
  },

  /**
   * Preview CSV import with validation.
   */
  previewImport: async (request: CsvImportRequest): Promise<CsvImportPreviewResponse> => {
    const response = await axiosInstance.post('/transactions/import/preview', request)
    return response.data.data
  },

  /**
   * Import transactions from CSV.
   */
  importTransactions: async (request: CsvImportRequest): Promise<CsvImportResultResponse> => {
    const response = await axiosInstance.post('/transactions/import', request)
    return response.data.data
  },

  /**
   * Export all transactions as CSV file.
   */
  exportTransactions: async (): Promise<Blob> => {
    const response = await axiosInstance.get('/transactions/export', {
      responseType: 'blob',
    })
    return response.data
  },
}
