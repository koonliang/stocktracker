export type Ticker = {
  symbol: string;
  name: string;
  sector: string;
  exchange: 'NASDAQ' | 'NYSE';
};

export type PriceBar = {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

export type KeyStats = {
  symbol: string;
  open: number;
  high: number;
  low: number;
  previousClose: number;
  volume: number;
  week52High: number;
  week52Low: number;
  marketCap: number;
  peRatio: number | null;
};

export type TransactionType = 'buy' | 'sell';

export type Transaction = {
  id: string;
  date: string; // ISO YYYY-MM-DD
  ticker: string;
  type: TransactionType;
  quantity: number;
  price: number;
  fees: number;
};

export type Holding = {
  ticker: string;
  name: string;
  shares: number;
  averageCost: number;
  costBasis: number;
  currentPrice: number;
  marketValue: number;
  unrealizedPnL: number;
  unrealizedPnLPct: number;
  dayChange: number;
  dayChangePct: number;
  weight: number; // 0..1
};

export type PortfolioSummary = {
  totalMarketValue: number;
  totalCostBasis: number;
  totalUnrealizedPnL: number;
  totalUnrealizedPnLPct: number;
  totalDayChange: number;
  totalDayChangePct: number;
};

export type Watchlist = {
  id: string;
  name: string;
  tickers: string[];
  createdAt: string;
  updatedAt: string;
};

export const TIME_RANGES = ['1D', '1W', '1M', '3M', '1Y', '5Y', 'ALL'] as const;
export type TimeRange = (typeof TIME_RANGES)[number];
