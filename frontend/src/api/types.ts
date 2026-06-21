export type Ticker = {
  symbol: string;
  name: string;
  sector: string;
  exchange: string;
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
  open: number | null;
  high: number | null;
  low: number | null;
  previousClose: number | null;
  volume: number | null;
  week52High: number | null;
  week52Low: number | null;
  marketCap: number | null;
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
  currencySource?: 'instrument' | 'manual' | 'import' | 'user_base_backfill';
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
  baseCurrency?: string;
  costBasisConversion?: ConversionMetadata;
  priceConversion?: ConversionMetadata;
  marketValueConversion?: ConversionMetadata;
  dayChangeConversion?: ConversionMetadata;
};

export type PortfolioSummary = {
  totalMarketValue: number;
  totalCostBasis: number;
  totalUnrealizedPnL: number;
  totalUnrealizedPnLPct: number;
  totalDayChange: number;
  totalDayChangePct: number;
  baseCurrency?: string;
  marketValueConversion?: ConversionMetadata;
  costBasisConversion?: ConversionMetadata;
  dayChangeConversion?: ConversionMetadata;
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
  warnings?: string[];
};

export type Watchlist = {
  id: string;
  name: string;
  tickers: string[];
  instruments?: WatchlistInstrument[];
  createdAt: string;
  updatedAt: string;
};

export type WatchlistInstrument = {
  symbol: string;
  name: string;
  exchange: string;
  currency: string;
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

export type SocialExchangeRequest = {
  code: string;
  redirectUri: string;
};

export type DemoUserListItem = {
  slot: number;
  label: string;
  email: string;
};

export type DemoUserCatalog = {
  users: DemoUserListItem[];
  maxUsers: number;
  canCreate: boolean;
};

export type DemoUserSession = {
  token: string;
  user: AuthUser;
  demoUser: {
    slot: number;
    label: string;
  };
};

export type StatusResponse = {
  status: string;
};

export type ClosedLot = {
  symbol: string;
  currency: string;
  openDate: string;
  closeDate: string;
  quantity: number;
  costBasisNative: number;
  proceedsNative: number;
  realizedPnLNative: number;
  realizedPnLBase: number;
  realizedPnlConversion?: ConversionMetadata;
};

export type IncomeEvent = {
  symbol: string;
  currency: string;
  date: string;
  type: 'dividend';
  amountNative: number;
  amountBase: number;
  amountConversion?: ConversionMetadata;
};

export type ReturnPoint = {
  date: string;
  cumulativeReturnPct: number;
};

export type Contribution = {
  symbol: string;
  contributionPct: number;
  contributionBase?: number;
  contributionConversion?: ConversionMetadata;
};

export type PerformanceResponse = {
  window: string;
  method: 'fifo' | 'lifo' | 'specific';
  baseCurrency: string;
  realizedPnL: number;
  unrealizedPnL: number;
  timeWeightedReturnPct: number;
  closedLots: ClosedLot[];
  incomeEvents: IncomeEvent[];
  returnSeries: ReturnPoint[];
  contributions: Contribution[];
};

export type AlertCondition = 'price_above' | 'price_below' | 'pct_change';

export type Alert = {
  id: string;
  symbol: string;
  conditionType: AlertCondition;
  threshold: number;
  armed: boolean;
  lastTriggeredAt: string | null;
  createdAt: string;
};

export type AlertListResponse = { alerts: Alert[] };

export type AlertRequest = {
  symbol: string;
  conditionType: AlertCondition;
  threshold: number;
};

export type Notification = {
  id: string;
  alertId: string | null;
  symbol: string | null;
  conditionType: string | null;
  threshold: number;
  thresholdCurrency: string | null;
  observedValue: number;
  observedCurrency: string | null;
  triggeredAt: string | null;
  read: boolean;
  message: string;
};

export type NotificationListResponse = {
  unreadCount: number;
  notifications: Notification[];
  nextCursor: string | null;
};

export type ReadAllResponse = {
  updated: number;
  unreadCount: number;
};

export type FxStatus = 'current' | 'stale' | 'unavailable';

export type ConversionMetadata = {
  baseCurrency: string;
  amountBase: number;
  fxDate: string | null;
  fxStatus: FxStatus;
};
