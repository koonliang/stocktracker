import React from 'react'
import type { CsvImportResultResponse } from '../../services/api/transactionApi'

interface ImportResultStepProps {
  result: CsvImportResultResponse
  onClose: () => void
  onImportMore: () => void
}

const ImportResultStep: React.FC<ImportResultStepProps> = ({ result, onClose, onImportMore }) => {
  const hasErrors = result.errors.length > 0
  const isSuccess = result.importedCount > 0

  return (
    <div className="space-y-6">
      <div className="text-center">
        {isSuccess ? (
          <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-4">
            <svg
              className="h-10 w-10 text-green-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
        ) : (
          <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-100 mb-4">
            <svg
              className="h-10 w-10 text-red-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </div>
        )}

        <h3 className="text-xl font-semibold text-gray-800 mb-2">
          {isSuccess ? 'Import Complete!' : 'Import Failed'}
        </h3>
      </div>

      <div className="bg-gray-50 rounded-lg p-6 space-y-3">
        <div className="flex justify-between items-center">
          <span className="text-gray-700 font-medium">Total Transactions Imported:</span>
          <span className="text-2xl font-bold text-green-600">{result.importedCount}</span>
        </div>

        {result.skippedCount > 0 && (
          <div className="flex justify-between items-center">
            <span className="text-gray-700 font-medium">Skipped/Failed:</span>
            <span className="text-2xl font-bold text-red-600">{result.skippedCount}</span>
          </div>
        )}
      </div>

      {hasErrors && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <h4 className="font-semibold text-red-900 mb-3">Errors:</h4>
          <div className="space-y-2 max-h-48 overflow-y-auto">
            {result.errors.map((error, idx) => (
              <div key={idx} className="text-sm text-red-800">
                {error.rowNumber > 0 && (
                  <span className="font-medium">Row {error.rowNumber}: </span>
                )}
                {error.field && <span className="font-medium">{error.field}: </span>}
                {error.message}
                {error.rejectedValue && (
                  <span className="text-red-600"> (value: &quot;{error.rejectedValue}&quot;)</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {isSuccess && result.importedTransactions.length > 0 && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <h4 className="font-semibold text-green-900 mb-3">Successfully Imported:</h4>
          <div className="space-y-2 max-h-48 overflow-y-auto">
            {result.importedTransactions.slice(0, 10).map(transaction => (
              <div key={transaction.id} className="text-sm text-green-800">
                {transaction.type} {transaction.shares} shares of {transaction.symbol} @ $
                {transaction.pricePerShare}
              </div>
            ))}
            {result.importedTransactions.length > 10 && (
              <div className="text-sm text-green-700 font-medium">
                ... and {result.importedTransactions.length - 10} more
              </div>
            )}
          </div>
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-3 sm:justify-between pt-4">
        {isSuccess ? (
          <>
            <button
              onClick={onImportMore}
              className="w-full sm:w-auto px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors text-sm font-medium"
            >
              Import More
            </button>
            <button
              onClick={onClose}
              className="w-full sm:w-auto px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors text-sm font-medium"
            >
              Done
            </button>
          </>
        ) : (
          <button
            onClick={onClose}
            className="w-full px-6 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors text-sm font-medium"
          >
            Close
          </button>
        )}
      </div>
    </div>
  )
}

export default ImportResultStep
