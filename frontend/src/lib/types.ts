export type {
  AddInstrumentResponse,
  BaseCurrencyResponse,
  DashboardResponse,
  Holding,
  InstrumentAnalysisResponse,
  InstrumentSearchResponse,
  KeyStats,
  PortfolioSummary,
  PriceBar,
  Quote,
  QuoteResponse,
  SymbolSearchResult,
  Ticker,
  Transaction,
  TransactionImportNormalizedRow,
  TransactionImportPreviewResponse,
  TransactionType,
  Watchlist,
  WatchlistResponse,
} from '@/api/types';

export const TIME_RANGES = ['1D', '1W', '1M', '3M', '1Y', '5Y', 'ALL'] as const;
export type TimeRange = (typeof TIME_RANGES)[number];
