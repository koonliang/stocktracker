import React, { useState, useEffect } from 'react'
import { transactionApi } from '../../services/api/transactionApi'

interface FieldMappingStepProps {
  headers: string[]
  onMappingComplete: (mappings: Record<string, string>) => void
  onBack: () => void
}

interface StandardField {
  value: string
  label: string
  required: boolean
  hint?: string
}

const STANDARD_FIELDS: StandardField[] = [
  {
    value: 'type',
    label: 'Type (BUY/SELL)',
    required: false,
    hint: 'Optional - can be inferred from quantity sign',
  },
  { value: 'symbol', label: 'Symbol', required: true },
  {
    value: 'exchange',
    label: 'Exchange/Market',
    required: false,
    hint: 'Optional - adds suffix to symbol (e.g., LSEETF → .L)',
  },
  { value: 'transactionDate', label: 'Transaction Date', required: true },
  {
    value: 'shares',
    label: 'Shares/Quantity',
    required: true,
    hint: 'Negative values indicate SELL',
  },
  { value: 'pricePerShare', label: 'Price Per Share', required: true },
  { value: 'notes', label: 'Notes', required: false },
  { value: 'skip', label: 'Skip this column', required: false },
]

const FieldMappingStep: React.FC<FieldMappingStepProps> = ({
  headers,
  onMappingComplete,
  onBack,
}) => {
  // Map from standard field to CSV column (inverted from backend response)
  const [mappings, setMappings] = useState<Record<string, string>>({})
  const [confidenceScores, setConfidenceScores] = useState<Record<string, number>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchSuggestions = async () => {
      try {
        const response = await transactionApi.suggestMapping({ headers })

        // Invert the mapping: backend gives us csvColumn->standardField, we want standardField->csvColumn
        // Only pre-select mappings with confidence >= 90%
        const invertedMappings: Record<string, string> = {}
        const invertedConfidence: Record<string, number> = {}

        Object.entries(response.suggestedMappings).forEach(([csvColumn, standardField]) => {
          const confidence = response.confidenceScores[csvColumn] || 0
          invertedConfidence[standardField] = confidence

          // Only auto-select if confidence is 90% or higher
          if (confidence >= 0.9) {
            invertedMappings[standardField] = csvColumn
          }
        })

        setMappings(invertedMappings)
        setConfidenceScores(invertedConfidence)
        setIsLoading(false)
      } catch (err) {
        console.error('Failed to fetch mapping suggestions:', err)
        setError('Failed to load field mapping suggestions')
        setIsLoading(false)
      }
    }

    fetchSuggestions()
  }, [headers])

  const handleMappingChange = (standardField: string, csvColumn: string) => {
    setMappings(prev => {
      const updated = { ...prev }
      if (csvColumn === 'none') {
        delete updated[standardField]
      } else {
        updated[standardField] = csvColumn
      }
      return updated
    })
  }

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.9) return 'text-green-600'
    if (confidence >= 0.7) return 'text-yellow-600'
    return 'text-red-600'
  }

  const getConfidenceIcon = (confidence: number) => {
    if (confidence >= 0.9) return '✓'
    if (confidence >= 0.7) return '⚠'
    return '✗'
  }

  const validateMappings = (): boolean => {
    const requiredFields = STANDARD_FIELDS.filter(f => f.required).map(f => f.value)

    for (const field of requiredFields) {
      if (!mappings[field]) {
        setError(`Missing required field: ${STANDARD_FIELDS.find(f => f.value === field)?.label}`)
        return false
      }
    }

    setError(null)
    return true
  }

  const handleContinue = () => {
    if (validateMappings()) {
      // Convert back to csvColumn->standardField format for backend
      const backendMappings: Record<string, string> = {}
      Object.entries(mappings).forEach(([standardField, csvColumn]) => {
        backendMappings[csvColumn] = standardField
      })
      onMappingComplete(backendMappings)
    }
  }

  const getMappedFieldsStatus = () => {
    const requiredFields = STANDARD_FIELDS.filter(f => f.required)

    return requiredFields.map(field => ({
      name: field.label,
      mapped: !!mappings[field.value],
    }))
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
        <p className="text-gray-600">Analyzing CSV headers...</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="text-center text-gray-600 mb-6">
        <p>Map your CSV columns to the standard transaction fields.</p>
        <p className="text-sm mt-2">
          The system has automatically suggested mappings based on column names.
        </p>
      </div>

      <div className="space-y-4">
        {STANDARD_FIELDS.filter(f => f.value !== 'skip').map(field => {
          const currentMapping = mappings[field.value] || 'none'
          const confidence = confidenceScores[field.value]

          return (
            <div
              key={field.value}
              className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4 p-4 bg-gray-50 rounded-lg"
            >
              <div className="flex-1 min-w-0">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  {field.label}
                  {field.required && <span className="text-red-600 ml-1">*</span>}
                </label>
                {field.hint && <p className="text-xs text-gray-500 mt-1">{field.hint}</p>}
              </div>

              <div className="flex items-center gap-3 sm:flex-1">
                <select
                  value={currentMapping}
                  onChange={e => handleMappingChange(field.value, e.target.value)}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                >
                  <option value="none">-- Select CSV Column --</option>
                  {headers.map(header => (
                    <option key={header} value={header}>
                      {header}
                    </option>
                  ))}
                </select>

                {confidence !== undefined && currentMapping !== 'none' && (
                  <span
                    className={`text-sm font-medium whitespace-nowrap ${getConfidenceColor(confidence)}`}
                  >
                    {getConfidenceIcon(confidence)} {Math.round(confidence * 100)}%
                  </span>
                )}
              </div>
            </div>
          )
        })}
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">Required Fields Status:</h3>
        <div className="flex flex-wrap gap-4">
          {getMappedFieldsStatus().map(field => (
            <div key={field.name} className="flex items-center gap-2">
              <span className={field.mapped ? 'text-green-600' : 'text-red-600'}>
                {field.mapped ? '✓' : '✗'}
              </span>
              <span className="text-sm text-gray-700">{field.name}</span>
            </div>
          ))}
        </div>
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

      <div className="flex flex-col sm:flex-row gap-3 sm:justify-between pt-4">
        <button
          onClick={onBack}
          className="w-full sm:w-auto px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors text-sm font-medium"
        >
          ← Back
        </button>
        <button
          onClick={handleContinue}
          className="w-full sm:w-auto px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors text-sm font-medium"
        >
          Preview Import →
        </button>
      </div>
    </div>
  )
}

export default FieldMappingStep
