import axiosInstance from './axiosInstance'

export interface HoldingResponse {
  id: number
  symbol: string
  companyName: string
  shares: number
  averageCost: number
  lastPrice: number
  previousClose: number | null
  totalReturnDollars: number
  totalReturnPercent: number
  currentValue: number
  costBasis: number

  // NEW fields
  sevenDayReturnPercent: number
  sevenDayReturnDollars: number
  weight: number
  sparklineData: number[]
}

export interface PortfolioResponse {
  holdings: HoldingResponse[]
  totalValue: number
  totalCost: number
  totalReturnDollars: number
  totalReturnPercent: number
  pricesUpdatedAt: string
}

// NEW: Performance history point
export interface PortfolioPerformancePoint {
  date: string // ISO date string
  totalValue: number
  dailyChange: number
  dailyChangePercent: number
}

// NEW: Time range type
export type TimeRange = '7d' | '1mo' | '3mo' | 'ytd' | '1y'

export const portfolioApi = {
  getPortfolio: async (): Promise<PortfolioResponse> => {
    const response = await axiosInstance.get('/portfolio')
    return response.data.data
  },

  refreshPortfolio: async (): Promise<PortfolioResponse> => {
    const response = await axiosInstance.get('/portfolio/refresh')
    return response.data.data
  },

  // NEW: Get performance history
  getPerformanceHistory: async (range: TimeRange = '1y'): Promise<PortfolioPerformancePoint[]> => {
    const response = await axiosInstance.get(`/portfolio/performance?range=${range}`)
    return response.data.data
  },
}
