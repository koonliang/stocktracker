import React, { useState, useRef } from 'react'
import Papa from 'papaparse'
import type { ParsedCsvData } from './ImportModal'

interface FileUploadStepProps {
  onFileUploaded: (data: ParsedCsvData) => void
}

const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const MAX_ROWS = 1000

const FileUploadStep: React.FC<FileUploadStepProps> = ({ onFileUploaded }) => {
  const [isDragging, setIsDragging] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isProcessing, setIsProcessing] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)

    const file = e.dataTransfer.files[0]
    if (file) {
      processFile(file)
    }
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      processFile(file)
    }
  }

  const processFile = (file: File) => {
    setError(null)

    // Validate file type
    if (!file.name.endsWith('.csv')) {
      setError('Please upload a CSV file')
      return
    }

    // Validate file size
    if (file.size > MAX_FILE_SIZE) {
      setError('File size must be less than 5MB')
      return
    }

    setIsProcessing(true)

    // Parse CSV
    Papa.parse(file, {
      header: true,
      skipEmptyLines: true,
      complete: results => {
        try {
          if (results.errors.length > 0) {
            const criticalErrors = results.errors.filter(
              e => e.type === 'Quotes' || e.type === 'FieldMismatch'
            )
            if (criticalErrors.length > 0) {
              setError(`CSV parsing error: ${criticalErrors[0].message}`)
              setIsProcessing(false)
              return
            }
          }

          if (!results.meta.fields || results.meta.fields.length === 0) {
            setError('CSV file must have headers')
            setIsProcessing(false)
            return
          }

          if (results.data.length === 0) {
            setError('CSV file is empty')
            setIsProcessing(false)
            return
          }

          if (results.data.length > MAX_ROWS) {
            setError(`Cannot import more than ${MAX_ROWS} rows at once`)
            setIsProcessing(false)
            return
          }

          const headers = results.meta.fields
          const rows = (results.data as Record<string, string>[]).map((row, index) => ({
            values: row,
            rowNumber: index + 1,
          }))

          onFileUploaded({ headers, rows })
          setIsProcessing(false)
        } catch {
          setError('Failed to process CSV file')
          setIsProcessing(false)
        }
      },
      error: error => {
        setError(`Failed to parse CSV: ${error.message}`)
        setIsProcessing(false)
      },
    })
  }

  const handleBrowseClick = () => {
    fileInputRef.current?.click()
  }

  return (
    <div className="space-y-4">
      <div className="text-center text-gray-600 mb-6">
        <p>Upload a CSV file containing your transaction data.</p>
        <p className="text-sm mt-2">Maximum file size: 5MB | Maximum rows: {MAX_ROWS}</p>
      </div>

      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
          isDragging ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
        }`}
      >
        {isProcessing ? (
          <div className="flex flex-col items-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
            <p className="text-gray-600">Processing CSV file...</p>
          </div>
        ) : (
          <>
            <svg
              className="mx-auto h-12 w-12 text-gray-400 mb-4"
              stroke="currentColor"
              fill="none"
              viewBox="0 0 48 48"
              aria-hidden="true"
            >
              <path
                d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                strokeWidth={2}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <p className="text-gray-700 font-medium mb-2">Drag and drop your CSV file here</p>
            <p className="text-gray-500 text-sm mb-4">or</p>
            <button
              type="button"
              onClick={handleBrowseClick}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
            >
              Browse Files
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              onChange={handleFileChange}
              className="hidden"
            />
          </>
        )}
      </div>

      {error && (
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
      )}

      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-md p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">CSV Format Tips:</h3>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li>Include headers in the first row</li>
          <li>Required fields: Type (BUY/SELL), Symbol, Date, Shares, Price</li>
          <li>Dates can be in various formats (MM/DD/YYYY, YYYY-MM-DD, etc.)</li>
          <li>Works with exports from major brokerages (Fidelity, Schwab, Robinhood, etc.)</li>
        </ul>
      </div>
    </div>
  )
}

export default FileUploadStep
