import { usePortfolioStore } from '@/stores/portfolioStore';

export function useHoldings() {
  return usePortfolioStore((state) => ({
    holdings: state.holdings,
    summary: state.summary,
    dashboardStatus: state.dashboardStatus,
    error: state.error,
  }));
}
