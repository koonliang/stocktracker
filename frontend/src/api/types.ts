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
  symbol?: string;
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
  date: string;
  ticker: string;
  type: TransactionType;
  quantity: number;
  price: number;
  fees: number;
  source?: 'MANUAL' | 'CSV_IMPORT';
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
  weight: number;
};

export type PortfolioSummary = {
  totalMarketValue: number;
  totalCostBasis: number;
  totalUnrealizedPnL: number;
  totalUnrealizedPnLPct: number;
  totalDayChange: number;
  totalDayChangePct: number;
};

export type DashboardResponse = {
  summary: PortfolioSummary;
  holdings: Holding[];
};

export type Watchlist = {
  id: string;
  name: string;
  tickers: string[];
  createdAt: string;
  updatedAt: string;
};

export type WatchlistResponse = {
  watchlists: Watchlist[];
};

export type InstrumentAnalysisResponse = {
  ticker: Ticker;
  stats: KeyStats | null;
  priceHistory: PriceBar[];
  positionSummary: {
    shares: number;
    averageCost: number;
    marketValue: number;
    unrealizedPnL: number;
    unrealizedPnLPct: number;
  } | null;
};

export type TransactionImportNormalizedRow = {
  date: string;
  ticker: string;
  type: TransactionType;
  quantity: number;
  price: number;
  fees: number;
};

export type TransactionImportPreviewResponse = {
  validRows: Array<{
    row: number;
    normalized: TransactionImportNormalizedRow;
  }>;
  invalidRows: Array<{
    row: number;
    reason: string;
    raw: Record<string, string>;
  }>;
  headerErrors: string[];
};

export type WatchlistMutationRequest = {
  name?: string;
  ticker?: string;
  tickers?: string[];
};
