import React, { useState } from 'react'
import FileUploadStep from './FileUploadStep'
import FieldMappingStep from './FieldMappingStep'
import ImportPreviewStep from './ImportPreviewStep'
import ImportProgressStep from './ImportProgressStep'
import ImportResultStep from './ImportResultStep'
import type { CsvRowData, CsvImportResultResponse } from '../../services/api/transactionApi'

export type ImportStep = 'UPLOAD' | 'MAPPING' | 'PREVIEW' | 'IMPORTING' | 'COMPLETE'

export interface ParsedCsvData {
  headers: string[]
  rows: CsvRowData[]
}

interface ImportModalProps {
  isOpen: boolean
  onClose: () => void
  onImportComplete: () => void
}

const ImportModal: React.FC<ImportModalProps> = ({ isOpen, onClose, onImportComplete }) => {
  const [currentStep, setCurrentStep] = useState<ImportStep>('UPLOAD')
  const [parsedData, setParsedData] = useState<ParsedCsvData | null>(null)
  const [fieldMappings, setFieldMappings] = useState<Record<string, string>>({})
  const [importResult, setImportResult] = useState<CsvImportResultResponse | null>(null)

  const handleFileUploaded = (data: ParsedCsvData) => {
    setParsedData(data)
    setCurrentStep('MAPPING')
  }

  const handleMappingComplete = (mappings: Record<string, string>) => {
    setFieldMappings(mappings)
    setCurrentStep('PREVIEW')
  }

  const handlePreviewConfirm = () => {
    setCurrentStep('IMPORTING')
  }

  const handleImportComplete = (result: CsvImportResultResponse) => {
    setImportResult(result)
    setCurrentStep('COMPLETE')
  }

  const handleClose = () => {
    if (currentStep === 'COMPLETE' && importResult && importResult.importedCount > 0) {
      onImportComplete()
    }
    setCurrentStep('UPLOAD')
    setParsedData(null)
    setFieldMappings({})
    setImportResult(null)
    onClose()
  }

  const handleBack = () => {
    if (currentStep === 'MAPPING') {
      setCurrentStep('UPLOAD')
    } else if (currentStep === 'PREVIEW') {
      setCurrentStep('MAPPING')
    }
  }

  const handleImportMore = () => {
    setCurrentStep('UPLOAD')
    setParsedData(null)
    setFieldMappings({})
    setImportResult(null)
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-800">Import Transactions from CSV</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
            aria-label="Close"
          >
            &times;
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {currentStep === 'UPLOAD' && <FileUploadStep onFileUploaded={handleFileUploaded} />}

          {currentStep === 'MAPPING' && parsedData && (
            <FieldMappingStep
              headers={parsedData.headers}
              onMappingComplete={handleMappingComplete}
              onBack={handleBack}
            />
          )}

          {currentStep === 'PREVIEW' && parsedData && (
            <ImportPreviewStep
              rows={parsedData.rows}
              fieldMappings={fieldMappings}
              onConfirm={handlePreviewConfirm}
              onBack={handleBack}
            />
          )}

          {currentStep === 'IMPORTING' && parsedData && (
            <ImportProgressStep
              rows={parsedData.rows}
              fieldMappings={fieldMappings}
              onComplete={handleImportComplete}
            />
          )}

          {currentStep === 'COMPLETE' && importResult && (
            <ImportResultStep
              result={importResult}
              onClose={handleClose}
              onImportMore={handleImportMore}
            />
          )}
        </div>
      </div>
    </div>
  )
}

export default ImportModal
