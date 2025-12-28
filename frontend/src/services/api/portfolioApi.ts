import axiosInstance from './axiosInstance';

export interface HoldingResponse {
  id: number;
  symbol: string;
  companyName: string;
  shares: number;
  averageCost: number;
  lastPrice: number;
  previousClose: number | null;
  totalReturnDollars: number;
  totalReturnPercent: number;
  currentValue: number;
  costBasis: number;
}

export interface PortfolioResponse {
  holdings: HoldingResponse[];
  totalValue: number;
  totalCost: number;
  totalReturnDollars: number;
  totalReturnPercent: number;
  pricesUpdatedAt: string;
}

export const portfolioApi = {
  getPortfolio: async (): Promise<PortfolioResponse> => {
    const response = await axiosInstance.get('/portfolio');
    return response.data.data;
  },

  refreshPortfolio: async (): Promise<PortfolioResponse> => {
    const response = await axiosInstance.get('/portfolio/refresh');
    return response.data.data;
  }
};
