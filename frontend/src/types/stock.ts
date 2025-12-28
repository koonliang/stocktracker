// Re-export from API for convenience
export type { HoldingResponse, PortfolioResponse } from '../services/api/portfolioApi';

// UI-specific type for display formatting
export interface StockTableRow {
  id: number;
  symbol: string;
  companyName: string;
  lastPrice: number;
  totalReturnDollars: number;
  totalReturnPercent: number;
  currentValue: number;
  costBasis: number;
}
