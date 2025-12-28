import type { StockHolding, StockTableRow } from '../types/stock'

/**
 * Calculate derived values for table display from a stock holding
 */
export function calculateStockTableRow(holding: StockHolding): StockTableRow {
  const currentValue = holding.lastPrice * holding.shares
  const costBasis = holding.averageCost * holding.shares
  const totalReturnDollars = currentValue - costBasis
  const totalReturnPercent = ((holding.lastPrice - holding.averageCost) / holding.averageCost) * 100

  return {
    symbol: holding.symbol,
    companyName: holding.companyName,
    lastPrice: holding.lastPrice,
    totalReturnDollars,
    totalReturnPercent,
    currentValue,
    costBasis,
  }
}

/**
 * Format currency with proper symbols and decimals
 */
export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

/**
 * Format percentage with sign indicator
 */
export function formatPercent(value: number): string {
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

/**
 * Get CSS class for positive/negative returns
 */
export function getReturnColorClass(value: number): string {
  if (value > 0) return 'text-emerald-600'
  if (value < 0) return 'text-red-500'
  return 'text-slate-600'
}
