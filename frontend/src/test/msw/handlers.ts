import { getBars, findTicker, loadSeedPortfolio, loadStats, loadTickers } from '@/lib/seed';
import { parseTransactionsCSV, serializeTransactionsCSV } from '@/lib/csv';
import { buildPriceLookup, computeHoldings, computePortfolio } from '@/lib/portfolio';
import type {
  InstrumentAnalysisResponse,
  PortfolioSummary,
  PriceBar,
  Quote,
  SymbolSearchResult,
  Transaction,
  TransactionImportPreviewResponse,
  Watchlist,
  WatchlistInstrument,
} from '@/lib/types';

type MockState = {
  transactions: Transaction[];
  watchlists: Watchlist[];
  baseCurrency: string;
};

const defaultState = (): MockState => ({
  transactions: [],
  watchlists: [],
  baseCurrency: 'USD',
});

const SUPPORTED_CURRENCIES = ['USD', 'SGD', 'EUR'];

// A non-US example so the search/add + multi-currency paths are exercised offline.
const GLOBAL_CATALOG: SymbolSearchResult[] = [
  { symbol: 'D05.SI', name: 'DBS Group Holdings Ltd', exchange: 'SGX', currency: 'SGD' },
  { symbol: 'ES3.SI', name: 'SPDR Straits Times Index ETF', exchange: 'SGX', currency: 'SGD' },
];

function findInstrument(symbol: string): WatchlistInstrument | null {
  const upper = symbol.toUpperCase();
  const global = GLOBAL_CATALOG.find((entry) => entry.symbol === upper);
  if (global) {
    return global;
  }
  const ticker = findTicker(upper);
  return ticker
    ? { symbol: ticker.symbol, name: ticker.name, exchange: ticker.exchange, currency: 'USD' }
    : null;
}

function withInstruments(watchlist: Watchlist): Watchlist {
  if (watchlist.instruments && watchlist.instruments.length > 0) {
    return watchlist;
  }
  return {
    ...watchlist,
    instruments: watchlist.tickers
      .map((symbol) => findInstrument(symbol))
      .filter((instrument): instrument is WatchlistInstrument => instrument != null),
  };
}

function buildQuote(symbol: string): Quote {
  const upper = symbol.toUpperCase();
  const bars = getBars(upper);
  const stats = loadStats()[upper];
  const price = bars.length > 0 ? bars[bars.length - 1]!.close : upper === 'D05.SI' ? 45.1 : null;
  const previousClose = stats?.previousClose ?? (upper === 'D05.SI' ? 44.9 : null);
  const currency = upper.endsWith('.SI') ? 'SGD' : 'USD';
  const changeAmount = price != null && previousClose != null ? price - previousClose : null;
  return {
    symbol: upper,
    price,
    currency: price == null ? null : currency,
    changeAmount,
    changePct: changeAmount != null && previousClose ? (changeAmount / previousClose) * 100 : null,
    previousClose,
    asOf: price == null ? null : new Date().toISOString(),
    fetchedAt: price == null ? null : new Date().toISOString(),
    source: price == null ? null : 'stub',
    stale: price == null,
  };
}

let state: MockState = defaultState();

function json(body: unknown, init?: ResponseInit) {
  return new Response(JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

function buildDashboard(transactions: Transaction[]) {
  const tickerMap = new Map(
    Object.values(loadStats()).map((stats) => {
      const ticker = findTicker(stats.symbol ?? '');
      return [ticker?.symbol ?? stats.symbol ?? '', ticker];
    }),
  );
  const prices = Object.fromEntries(
    Object.keys(loadStats()).map((symbol) => [symbol, getBars(symbol)]),
  ) as Record<string, PriceBar[]>;
  const holdings = computeHoldings(transactions, buildPriceLookup(prices), tickerMap as never);
  const summary: PortfolioSummary = computePortfolio(holdings);
  return { holdings, summary };
}

function buildInstrumentAnalysis(symbol: string): InstrumentAnalysisResponse | null {
  const ticker = findTicker(symbol);
  if (!ticker) return null;
  const statsMap = loadStats();
  const stats = statsMap[symbol] ?? null;
  const bars = getBars(symbol);
  const dashboard = buildDashboard(state.transactions);
  const holding = dashboard.holdings.find((entry) => entry.ticker === symbol) ?? null;
  return {
    ticker,
    stats: stats
      ? {
          open: stats.open,
          high: stats.high,
          low: stats.low,
          previousClose: stats.previousClose,
          volume: stats.volume,
          week52High: stats.week52High,
          week52Low: stats.week52Low,
          marketCap: stats.marketCap,
          peRatio: stats.peRatio,
        }
      : null,
    quote: (() => {
      const q = buildQuote(symbol);
      return {
        price: q.price,
        previousClose: q.previousClose,
        changeAmount: q.changeAmount,
        changePct: q.changePct,
        asOf: q.asOf,
        stale: q.stale,
      };
    })(),
    priceHistory: bars,
    positionSummary: holding
      ? {
          shares: holding.shares,
          averageCost: holding.averageCost,
          marketValue: holding.marketValue,
          unrealizedPnL: holding.unrealizedPnL,
          unrealizedPnLPct: holding.unrealizedPnLPct,
        }
      : null,
  };
}

function normalizeUrl(input: RequestInfo | URL): URL {
  if (typeof input === 'string') {
    return new URL(input, 'http://localhost');
  }
  if (input instanceof URL) return input;
  return new URL(input.url, 'http://localhost');
}

function newId(prefix: string) {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
}

export function resetMockApiState() {
  state = defaultState();
}

export function setMockApiState(next: Partial<MockState>) {
  state = { ...state, ...next };
}

export function seedMockPortfolio() {
  state.transactions = loadSeedPortfolio();
}

export async function handleMockApi(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  const url = normalizeUrl(input);
  const method = (init?.method ?? 'GET').toUpperCase();
  const path = url.pathname;

  if (!path.startsWith('/api/')) {
    return new Response('Not found', { status: 404 });
  }

  if (path === '/api/dashboard' && method === 'GET') {
    return json(buildDashboard(state.transactions));
  }

  if (path === '/api/transactions' && method === 'GET') {
    return json([...state.transactions].sort((a, b) => b.date.localeCompare(a.date)));
  }

  if (path === '/api/transactions' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as {
      rows: Array<Omit<Transaction, 'id'>>;
    };
    state.transactions = [
      ...state.transactions,
      ...body.rows.map((row) => ({ ...row, id: newId('tx') })),
    ];
    return json(buildDashboard(state.transactions), { status: 201 });
  }

  if (path.startsWith('/api/transactions/') && method === 'DELETE') {
    const id = path.split('/').pop() ?? '';
    state.transactions = state.transactions.filter((transaction) => transaction.id !== id);
    return json(buildDashboard(state.transactions));
  }

  if (path === '/api/transactions/import/preview' && method === 'POST') {
    const form = init?.body as FormData;
    const file = form.get('file');
    const text = file instanceof File ? await file.text() : '';
    const parsed = parseTransactionsCSV(text);
    const preview: TransactionImportPreviewResponse = {
      validRows: parsed.valid.map((transaction, index) => ({
        row: index + 2,
        normalized: {
          date: transaction.date,
          ticker: transaction.ticker,
          type: transaction.type,
          quantity: transaction.quantity,
          price: transaction.price,
          fees: transaction.fees,
          amount: transaction.amount,
          currency: transaction.currency,
        },
      })),
      invalidRows: parsed.invalid,
      headerErrors: parsed.headerErrors,
      detectedVersion: parsed.valid.some((transaction) =>
        ['dividend', 'split', 'deposit', 'withdrawal', 'fee'].includes(transaction.type),
      )
        ? 'v2'
        : 'v1',
    };
    return json(preview);
  }

  if (path === '/api/transactions/import/commit' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as {
      rows: Array<Omit<Transaction, 'id'>>;
    };
    state.transactions = [
      ...state.transactions,
      ...body.rows.map((row) => ({ ...row, id: newId('tx') })),
    ];
    return json(buildDashboard(state.transactions));
  }

  if (path === '/api/transactions/export' && method === 'GET') {
    return new Response(serializeTransactionsCSV(state.transactions), {
      headers: { 'Content-Type': 'text/csv' },
    });
  }

  if (path === '/api/watchlists' && method === 'GET') {
    return json({ watchlists: state.watchlists.map(withInstruments) });
  }

  if (path === '/api/watchlists' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { name?: string };
    const name = body.name?.trim() ?? '';
    if (!name)
      return json({ code: 'validation_error', message: 'Name is required' }, { status: 400 });
    if (state.watchlists.some((watchlist) => watchlist.name.toLowerCase() === name.toLowerCase())) {
      return json({ code: 'duplicate_name', message: 'Watchlist already exists' }, { status: 409 });
    }
    const now = new Date().toISOString();
    const watchlist: Watchlist = {
      id: newId('wl'),
      name,
      tickers: [],
      createdAt: now,
      updatedAt: now,
    };
    state.watchlists = [watchlist, ...state.watchlists];
    return json(withInstruments(watchlist));
  }

  if (path.startsWith('/api/watchlists/') && !path.includes('/tickers') && method === 'PATCH') {
    const id = path.split('/')[3] ?? '';
    const body = JSON.parse(String(init?.body ?? '{}')) as { name?: string };
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist)
      return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    watchlist.name = body.name?.trim() ?? watchlist.name;
    watchlist.updatedAt = new Date().toISOString();
    return json(withInstruments(watchlist));
  }

  if (path.startsWith('/api/watchlists/') && !path.includes('/tickers') && method === 'DELETE') {
    const id = path.split('/')[3] ?? '';
    state.watchlists = state.watchlists.filter((entry) => entry.id !== id);
    return new Response(null, { status: 204 });
  }

  if (path.endsWith('/tickers') && method === 'POST') {
    const id = path.split('/')[3] ?? '';
    const body = JSON.parse(String(init?.body ?? '{}')) as { ticker?: string };
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist)
      return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    const ticker = body.ticker?.trim().toUpperCase() ?? '';
    if (!findInstrument(ticker))
      return json({ code: 'validation_error', message: 'Ticker is unknown' }, { status: 422 });
    if (watchlist.tickers.includes(ticker)) {
      return json({ code: 'duplicate_ticker', message: 'Ticker already exists' }, { status: 409 });
    }
    watchlist.tickers = [...watchlist.tickers, ticker];
    watchlist.updatedAt = new Date().toISOString();
    return json(withInstruments(watchlist));
  }

  if (path.includes('/tickers/') && method === 'DELETE') {
    const [_, __, ___, id, ____, ticker] = path.split('/');
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist)
      return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    watchlist.tickers = watchlist.tickers.filter(
      (entry) => entry !== decodeURIComponent(ticker ?? ''),
    );
    watchlist.updatedAt = new Date().toISOString();
    return json(withInstruments(watchlist));
  }

  if (path.endsWith('/ticker-order') && method === 'PUT') {
    const id = path.split('/')[3] ?? '';
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist)
      return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    const body = JSON.parse(String(init?.body ?? '{}')) as { tickers?: string[] };
    watchlist.tickers = body.tickers ?? watchlist.tickers;
    watchlist.updatedAt = new Date().toISOString();
    return json(withInstruments(watchlist));
  }

  if (path === '/api/auth/login' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { email?: string; password?: string };
    const email = body.email?.trim().toLowerCase() ?? '';
    if (email === 'unverified@example.com') {
      return json(
        { code: 'EMAIL_UNVERIFIED', message: 'Please verify your email' },
        { status: 403 },
      );
    }
    if (email === 'investor@example.com' && body.password === 'Passw0rd!') {
      return json({ token: 'mock-jwt', user: { id: 1, email } });
    }
    return json({ code: 'AUTH_FAILED', message: 'Invalid email or password' }, { status: 401 });
  }

  if (path === '/api/auth/signup' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { email?: string; password?: string };
    const email = body.email?.trim() ?? '';
    const password = body.password ?? '';
    const policyOk =
      password.length >= 8 &&
      /[A-Z]/.test(password) &&
      /[a-z]/.test(password) &&
      /[0-9]/.test(password);
    if (!email.includes('@') || !policyOk) {
      return json({ code: 'VALIDATION', message: 'Invalid email or password' }, { status: 400 });
    }
    return json({ status: 'verification_sent' }, { status: 202 });
  }

  if (path === '/api/auth/verify-email' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { token?: string };
    if (body.token === 'valid-token') {
      return json({ status: 'verified' });
    }
    return json(
      { code: 'TOKEN_INVALID', message: 'This link is invalid or has expired' },
      {
        status: 400,
      },
    );
  }

  if (path === '/api/auth/resend-verification' && method === 'POST') {
    return json({ status: 'verification_sent' }, { status: 202 });
  }

  if (path === '/api/auth/forgot-password' && method === 'POST') {
    // Non-enumerating: always the same accepted response (FR-016).
    return json({ status: 'reset_sent' }, { status: 202 });
  }

  if (path === '/api/auth/reset-password' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as {
      token?: string;
      newPassword?: string;
    };
    if (body.token !== 'valid-reset-token') {
      return json(
        { code: 'TOKEN_INVALID', message: 'This link is invalid or has expired' },
        { status: 400 },
      );
    }
    const password = body.newPassword ?? '';
    const policyOk =
      password.length >= 8 &&
      /[A-Z]/.test(password) &&
      /[a-z]/.test(password) &&
      /[0-9]/.test(password);
    if (!policyOk) {
      return json(
        { code: 'VALIDATION', message: 'Password does not meet the policy' },
        {
          status: 400,
        },
      );
    }
    return json({ status: 'reset' });
  }

  if (path === '/api/auth/me' && method === 'GET') {
    return json({ id: 1, email: 'investor@example.com' });
  }

  if (path === '/api/auth/logout' && method === 'POST') {
    return new Response(null, { status: 204 });
  }

  if (path === '/api/quotes' && method === 'GET') {
    const raw = url.searchParams.get('symbols') ?? '';
    const symbols = raw
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    return json({ quotes: symbols.map(buildQuote) });
  }

  if (path === '/api/instruments/search' && method === 'GET') {
    const query = (url.searchParams.get('q') ?? '').trim().toLowerCase();
    if (!query) return json({ results: [] });
    const fromSeed: SymbolSearchResult[] = loadTickers()
      .filter(
        (ticker) =>
          ticker.symbol.toLowerCase().includes(query) || ticker.name.toLowerCase().includes(query),
      )
      .map((ticker) => ({
        symbol: ticker.symbol,
        name: ticker.name,
        exchange: ticker.exchange,
        currency: 'USD',
      }));
    const fromGlobal = GLOBAL_CATALOG.filter(
      (entry) =>
        entry.symbol.toLowerCase().includes(query) || entry.name.toLowerCase().includes(query),
    );
    const bySymbol = new Map<string, SymbolSearchResult>();
    [...fromSeed, ...fromGlobal].forEach((entry) => bySymbol.set(entry.symbol, entry));
    return json({ results: Array.from(bySymbol.values()) });
  }

  if (path === '/api/instruments' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { symbol?: string };
    const symbol = (body.symbol ?? '').trim().toUpperCase();
    const known =
      findTicker(symbol) != null || GLOBAL_CATALOG.some((entry) => entry.symbol === symbol);
    if (!known) {
      return json(
        { code: 'unknown_symbol', message: `Symbol not recognized: ${symbol}` },
        { status: 422 },
      );
    }
    const match = GLOBAL_CATALOG.find((entry) => entry.symbol === symbol);
    const quote = buildQuote(symbol);
    return json(
      {
        symbol,
        name: match?.name ?? findTicker(symbol)?.name ?? symbol,
        exchange: match?.exchange ?? findTicker(symbol)?.exchange ?? '',
        currency: match?.currency ?? 'USD',
        quote: { price: quote.price, asOf: quote.asOf, stale: quote.stale },
      },
      { status: 201 },
    );
  }

  if (path === '/api/me/base-currency' && method === 'GET') {
    return json({ baseCurrency: state.baseCurrency, supported: SUPPORTED_CURRENCIES });
  }

  if (path === '/api/me/base-currency' && method === 'PUT') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { baseCurrency?: string };
    const next = (body.baseCurrency ?? '').trim().toUpperCase();
    if (!SUPPORTED_CURRENCIES.includes(next)) {
      return json(
        { code: 'unsupported_currency', message: `Unsupported base currency: ${next}` },
        { status: 422 },
      );
    }
    state.baseCurrency = next;
    return json({ baseCurrency: next, supported: SUPPORTED_CURRENCIES });
  }

  if (path.startsWith('/api/instruments/') && method === 'GET') {
    const symbol = decodeURIComponent(path.split('/').pop() ?? '').toUpperCase();
    const analysis = buildInstrumentAnalysis(symbol);
    if (!analysis) {
      return json({ code: 'not_found', message: 'Ticker not found' }, { status: 404 });
    }
    return json(analysis);
  }

  return new Response('Not found', { status: 404 });
}
