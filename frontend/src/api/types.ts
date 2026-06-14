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

export type TransactionType =
  | 'buy'
  | 'sell'
  | 'dividend'
  | 'split'
  | 'deposit'
  | 'withdrawal'
  | 'fee';

export type Transaction = {
  id: string;
  date: string;
  ticker: string | null;
  type: TransactionType;
  quantity: number;
  price: number;
  fees: number;
  amount?: number | null;
  currency?: string | null;
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
  // Live-data fields (US1) — optional so locally-computed dashboards remain valid.
  currency?: string;
  nativePrice?: number;
  nativeMarketValue?: number;
  asOf?: string | null;
  fetchedAt?: string | null;
  stale?: boolean;
};

export type PortfolioSummary = {
  totalMarketValue: number;
  totalCostBasis: number;
  totalUnrealizedPnL: number;
  totalUnrealizedPnLPct: number;
  totalDayChange: number;
  totalDayChangePct: number;
  baseCurrency?: string;
};

export type Quote = {
  symbol: string;
  price: number | null;
  currency: string | null;
  changeAmount: number | null;
  changePct: number | null;
  previousClose: number | null;
  asOf: string | null;
  fetchedAt: string | null;
  source: string | null;
  stale: boolean;
};

export type QuoteResponse = { quotes: Quote[] };

export type SymbolSearchResult = {
  symbol: string;
  name: string;
  exchange: string;
  currency: string;
};

export type InstrumentSearchResponse = { results: SymbolSearchResult[] };

export type AddInstrumentResponse = {
  symbol: string;
  name: string;
  exchange: string;
  currency: string;
  quote: { price: number | null; asOf: string | null; stale: boolean };
};

export type BaseCurrencyResponse = { baseCurrency: string; supported: string[] };

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
  quote: {
    price: number | null;
    previousClose: number | null;
    changeAmount: number | null;
    changePct: number | null;
    asOf: string | null;
    stale: boolean;
  } | null;
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
  ticker: string | null;
  type: TransactionType;
  quantity: number | null;
  price: number | null;
  fees: number | null;
  amount?: number | null;
  currency?: string | null;
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
  detectedVersion: 'v1' | 'v2' | 'unknown';
};

export type WatchlistMutationRequest = {
  name?: string;
  ticker?: string;
  tickers?: string[];
};

export type AuthUser = {
  id: number;
  email: string;
};

export type LoginResponse = {
  token: string;
  user: AuthUser;
};

export type StatusResponse = {
  status: string;
};
