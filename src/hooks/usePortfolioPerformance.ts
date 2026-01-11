import { useState, useEffect, useCallback } from 'react'
import {
  portfolioApi,
  type PortfolioPerformancePoint,
  type TimeRange,
} from '../services/api/portfolioApi'

export function usePortfolioPerformance(initialRange: TimeRange = '1y') {
  const [range, setRange] = useState<TimeRange>(initialRange)
  const [data, setData] = useState<PortfolioPerformancePoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchPerformance = useCallback(async (selectedRange: TimeRange) => {
    try {
      setLoading(true)
      setError(null)
      const result = await portfolioApi.getPerformanceHistory(selectedRange)
      setData(result)
    } catch {
      setError('Failed to load performance data')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchPerformance(range)
  }, [range, fetchPerformance])

  const changeRange = useCallback((newRange: TimeRange) => {
    setRange(newRange)
  }, [])

  return { data, range, loading, error, changeRange }
}
