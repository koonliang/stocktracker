import React, { useState, useEffect } from 'react'
import {
  transactionApi,
  type CsvRowData,
  type CsvImportPreviewResponse,
  type TransactionPreviewRow,
} from '../../services/api/transactionApi'

interface ImportPreviewStepProps {
  rows: CsvRowData[]
  fieldMappings: Record<string, string>
  onConfirm: () => void
  onBack: () => void
}

const ImportPreviewStep: React.FC<ImportPreviewStepProps> = ({
  rows,
  fieldMappings,
  onConfirm,
  onBack,
}) => {
  const [previewData, setPreviewData] = useState<CsvImportPreviewResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set())

  useEffect(() => {
    const fetchPreview = async () => {
      try {
        const response = await transactionApi.previewImport({ rows, fieldMappings })
        setPreviewData(response)

        // Auto-expand error rows
        const errorRowNumbers = new Set(response.errorRows.map(row => row.rowNumber))
        setExpandedRows(errorRowNumbers)

        setIsLoading(false)
      } catch (err) {
        console.error('Failed to fetch preview:', err)
        const error = err as { response?: { data?: { message?: string } } }
        setError(error.response?.data?.message || 'Failed to preview import')
        setIsLoading(false)
      }
    }

    fetchPreview()
  }, [rows, fieldMappings])

  const toggleRowExpansion = (rowNumber: number) => {
    setExpandedRows(prev => {
      const updated = new Set(prev)
      if (updated.has(rowNumber)) {
        updated.delete(rowNumber)
      } else {
        updated.add(rowNumber)
      }
      return updated
    })
  }

  const renderPreviewCard = (row: TransactionPreviewRow, isError: boolean) => {
    const isExpanded = expandedRows.has(row.rowNumber)

    return (
      <div
        key={row.rowNumber}
        className={`p-4 rounded-lg border ${isError ? 'bg-red-50 border-red-200' : 'bg-white border-gray-200'}`}
      >
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-gray-500">Row {row.rowNumber}</span>
            {row.valid ? (
              <span className="text-green-600 font-bold text-lg">✓</span>
            ) : (
              <button
                onClick={() => toggleRowExpansion(row.rowNumber)}
                className="text-red-600 font-bold text-lg hover:text-red-700"
              >
                ✗
              </button>
            )}
          </div>
          <span
            className={`px-2 py-1 rounded text-xs font-medium ${
              row.type === 'BUY' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
            }`}
          >
            {row.type || '-'}
          </span>
        </div>

        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600 font-medium">Symbol:</span>
            <span className="text-gray-900">{row.symbol || '-'}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600 font-medium">Date:</span>
            <span className="text-gray-900">{row.transactionDate || '-'}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600 font-medium">Shares:</span>
            <span className="text-gray-900">{row.shares?.toString() || '-'}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600 font-medium">Price:</span>
            <span className="text-gray-900">
              {row.pricePerShare ? `$${row.pricePerShare}` : '-'}
            </span>
          </div>
          {row.notes && (
            <div className="flex justify-between text-sm">
              <span className="text-gray-600 font-medium">Notes:</span>
              <span className="text-gray-900 text-right">{row.notes}</span>
            </div>
          )}
        </div>

        {isExpanded && row.errors && row.errors.length > 0 && (
          <div className="mt-3 pt-3 border-t border-red-200">
            <p className="text-xs font-semibold text-red-900 mb-2">Validation Errors:</p>
            <ul className="space-y-1">
              {row.errors.map((err, idx) => (
                <li key={idx} className="text-xs text-red-800">
                  <span className="font-medium">{err.field}:</span> {err.message}
                  {err.rejectedValue && (
                    <span className="text-red-600"> (value: "{err.rejectedValue}")</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    )
  }

  const renderPreviewRow = (row: TransactionPreviewRow, isError: boolean) => {
    const isExpanded = expandedRows.has(row.rowNumber)

    return (
      <React.Fragment key={row.rowNumber}>
        <tr className={isError ? 'bg-red-50' : 'hover:bg-gray-50'}>
          <td className="px-4 py-3 text-sm text-gray-900">{row.rowNumber}</td>
          <td className="px-4 py-3 text-center">
            {row.valid ? (
              <span className="text-green-600 font-bold">✓</span>
            ) : (
              <button
                onClick={() => toggleRowExpansion(row.rowNumber)}
                className="text-red-600 font-bold hover:text-red-700"
              >
                ✗
              </button>
            )}
          </td>
          <td className="px-4 py-3 text-sm text-gray-900">{row.type || '-'}</td>
          <td className="px-4 py-3 text-sm text-gray-900">{row.symbol || '-'}</td>
          <td className="px-4 py-3 text-sm text-gray-900">{row.transactionDate || '-'}</td>
          <td className="px-4 py-3 text-sm text-gray-900">{row.shares?.toString() || '-'}</td>
          <td className="px-4 py-3 text-sm text-gray-900">
            {row.pricePerShare ? `$${row.pricePerShare}` : '-'}
          </td>
          <td className="px-4 py-3 text-sm text-gray-900">{row.notes || ''}</td>
        </tr>
        {isExpanded && row.errors && row.errors.length > 0 && (
          <tr className="bg-red-100">
            <td colSpan={8} className="px-4 py-3">
              <div className="text-sm">
                <p className="font-semibold text-red-900 mb-2">Validation Errors:</p>
                <ul className="list-disc list-inside space-y-1">
                  {row.errors.map((err, idx) => (
                    <li key={idx} className="text-red-800">
                      <span className="font-medium">{err.field}:</span> {err.message}
                      {err.rejectedValue && (
                        <span className="text-red-600"> (value: "{err.rejectedValue}")</span>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            </td>
          </tr>
        )}
      </React.Fragment>
    )
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
        <p className="text-gray-600">Validating transactions...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 border border-red-200 rounded-md p-4">
          <div className="flex">
            <svg className="h-5 w-5 text-red-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
            <p className="text-sm text-red-800">{error}</p>
          </div>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 sm:justify-between pt-4">
          <button
            onClick={onBack}
            className="w-full sm:w-auto px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors text-sm font-medium"
          >
            ← Back to Mapping
          </button>
        </div>
      </div>
    )
  }

  if (!previewData) {
    return null
  }

  return (
    <div className="space-y-6">
      <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
        <h3 className="text-sm font-medium text-blue-900">Import Summary</h3>
        <p className="text-sm text-blue-800 mt-1">
          {previewData.totalRows} total rows |
          <span className="text-green-700 font-semibold"> {previewData.validCount} valid</span> |
          <span className="text-red-700 font-semibold"> {previewData.errorCount} errors</span>
        </p>
      </div>

      {/* Mobile Card View */}
      <div className="sm:hidden space-y-3 max-h-[400px] overflow-y-auto">
        {previewData.validRows.map(row => renderPreviewCard(row, false))}
        {previewData.errorRows.map(row => renderPreviewCard(row, true))}
      </div>

      {/* Desktop Table View */}
      <div className="hidden sm:block border border-gray-200 rounded-lg overflow-hidden">
        <div className="overflow-x-auto max-h-[400px] overflow-y-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Row
                </th>
                <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Symbol
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Shares
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Price
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Notes
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {previewData.validRows.map(row => renderPreviewRow(row, false))}
              {previewData.errorRows.map(row => renderPreviewRow(row, true))}
            </tbody>
          </table>
        </div>
      </div>

      {previewData.errorCount > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4">
          <div className="flex">
            <svg className="h-5 w-5 text-yellow-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                clipRule="evenodd"
              />
            </svg>
            <div className="text-sm text-yellow-800">
              <p className="font-semibold">Some rows have errors and will be skipped.</p>
              <p className="mt-1">
                Click the ✗ icon to view error details. Only valid rows will be imported.
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-3 sm:justify-between pt-4">
        <button
          onClick={onBack}
          className="w-full sm:w-auto px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors text-sm font-medium"
        >
          ← Back to Mapping
        </button>
        <button
          onClick={onConfirm}
          disabled={previewData.validCount === 0}
          className={`w-full sm:w-auto px-6 py-2 rounded-md text-white transition-colors text-sm font-medium ${
            previewData.validCount === 0
              ? 'bg-gray-400 cursor-not-allowed'
              : 'bg-green-600 hover:bg-green-700'
          }`}
        >
          Import {previewData.validCount} Transaction{previewData.validCount !== 1 ? 's' : ''}
        </button>
      </div>
    </div>
  )
}

export default ImportPreviewStep
