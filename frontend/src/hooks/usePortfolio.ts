import { useState, useEffect, useCallback } from 'react';
import { portfolioApi } from '../services/api/portfolioApi';
import type { PortfolioResponse } from '../services/api/portfolioApi';

export function usePortfolio() {
  const [portfolio, setPortfolio] = useState<PortfolioResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPortfolio = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await portfolioApi.getPortfolio();
      setPortfolio(data);
    } catch (err) {
      setError('Failed to load portfolio');
    } finally {
      setLoading(false);
    }
  }, []);

  const refresh = useCallback(async () => {
    try {
      setError(null);
      const data = await portfolioApi.refreshPortfolio();
      setPortfolio(data);
    } catch (err) {
      setError('Failed to refresh prices');
    }
  }, []);

  useEffect(() => {
    fetchPortfolio();
  }, [fetchPortfolio]);

  return { portfolio, loading, error, refresh };
}
