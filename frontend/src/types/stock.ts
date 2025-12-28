export interface StockHolding {
  id: number
  symbol: string
  companyName: string
  shares: number
  averageCost: number // Cost per share
  lastPrice: number // Current market price
  previousClose?: number // For daily change calculation
}

export interface PortfolioSummary {
  totalValue: number
  totalCost: number
  totalReturn: number
  totalReturnPercent: number
}

// Computed helper type for table display
export interface StockTableRow {
  symbol: string
  companyName: string
  lastPrice: number
  totalReturnDollars: number // (lastPrice - avgCost) * shares
  totalReturnPercent: number // ((lastPrice - avgCost) / avgCost) * 100
  currentValue: number // lastPrice * shares
  costBasis: number // avgCost * shares
}
