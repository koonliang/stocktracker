import { useState, useEffect, useCallback } from 'react'
import { portfolioApi } from '../services/api/portfolioApi'
import type { PortfolioResponse } from '../services/api/portfolioApi'

export function usePortfolio() {
  const [portfolio, setPortfolio] = useState<PortfolioResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchPortfolio = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await portfolioApi.getPortfolio()
      setPortfolio(data)
    } catch {
      setError('Failed to load portfolio')
    } finally {
      setLoading(false)
    }
  }, [])

  const refresh = useCallback(async () => {
    try {
      // Use refreshing state instead of loading to avoid full page flicker
      setRefreshing(true)
      setError(null)
      const data = await portfolioApi.refreshPortfolio()
      setPortfolio(data)
    } catch {
      setError('Failed to refresh prices')
    } finally {
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    fetchPortfolio()
  }, [fetchPortfolio])

  return { portfolio, loading, refreshing, error, refresh }
}
