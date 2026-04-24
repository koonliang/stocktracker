import { useMemo } from 'react';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { loadPrices, loadTickers } from '@/lib/seed';
import { buildPriceLookup, computeHoldings, computePortfolio } from '@/lib/portfolio';

export function useHoldings() {
  const transactions = usePortfolioStore((s) => s.transactions);
  return useMemo(() => {
    const tickerMap = new Map(loadTickers().map((t) => [t.symbol, t]));
    const lookup = buildPriceLookup(loadPrices());
    const holdings = computeHoldings(transactions, lookup, tickerMap);
    const summary = computePortfolio(holdings);
    return { holdings, summary };
  }, [transactions]);
}
