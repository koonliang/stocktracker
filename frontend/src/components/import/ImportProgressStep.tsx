import React, { useEffect, useRef } from 'react'
import {
  transactionApi,
  type CsvRowData,
  type CsvImportResultResponse,
} from '../../services/api/transactionApi'

interface ImportProgressStepProps {
  rows: CsvRowData[]
  fieldMappings: Record<string, string>
  onComplete: (result: CsvImportResultResponse) => void
}

const ImportProgressStep: React.FC<ImportProgressStepProps> = ({
  rows,
  fieldMappings,
  onComplete,
}) => {
  const importStartedRef = useRef(false)

  useEffect(() => {
    // Prevent duplicate execution if import already started
    if (importStartedRef.current) {
      return
    }

    importStartedRef.current = true

    const executeImport = async () => {
      try {
        const result = await transactionApi.importTransactions({ rows, fieldMappings })
        onComplete(result)
      } catch (err) {
        console.error('Import failed:', err)
        const error = err as {
          response?: {
            data?: {
              data?: CsvImportResultResponse
              message?: string
            }
          }
        }

        // Check if the response contains the actual import result
        const importResult = error.response?.data?.data

        if (importResult) {
          // Backend returned structured result even on error
          onComplete(importResult)
        } else {
          // Generic error - create a simple result
          console.log('Error details:', error)
          onComplete({
            importedCount: 0,
            skippedCount: rows.length,
            errors: [
              {
                rowNumber: 0,
                field: 'system',
                message: error.response?.data?.message || 'Import failed. Please try again.',
                rejectedValue: null,
              },
            ],
            importedTransactions: [],
          })
        }
      }
    }

    executeImport()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mb-6"></div>
      <h3 className="text-lg font-semibold text-gray-800 mb-2">Importing Transactions</h3>
      <p className="text-gray-600">Please wait while we process your transactions...</p>
      <p className="text-sm text-gray-500 mt-2">This may take a few moments.</p>
    </div>
  )
}

export default ImportProgressStep
