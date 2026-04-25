import { getBars, findTicker, loadSeedPortfolio, loadStats } from '@/lib/seed';
import { parseTransactionsCSV, serializeTransactionsCSV } from '@/lib/csv';
import { buildPriceLookup, computeHoldings, computePortfolio } from '@/lib/portfolio';
import type {
  InstrumentAnalysisResponse,
  PortfolioSummary,
  PriceBar,
  Transaction,
  TransactionImportPreviewResponse,
  Watchlist,
} from '@/lib/types';

type MockState = {
  transactions: Transaction[];
  watchlists: Watchlist[];
};

const defaultState = (): MockState => ({
  transactions: [],
  watchlists: [],
});

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

export async function handleMockApi(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
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
        },
      })),
      invalidRows: parsed.invalid,
      headerErrors: parsed.headerErrors,
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
    return json({ watchlists: state.watchlists });
  }

  if (path === '/api/watchlists' && method === 'POST') {
    const body = JSON.parse(String(init?.body ?? '{}')) as { name?: string };
    const name = body.name?.trim() ?? '';
    if (!name) return json({ code: 'validation_error', message: 'Name is required' }, { status: 400 });
    if (state.watchlists.some((watchlist) => watchlist.name.toLowerCase() === name.toLowerCase())) {
      return json({ code: 'duplicate_name', message: 'Watchlist already exists' }, { status: 409 });
    }
    const now = new Date().toISOString();
    const watchlist: Watchlist = { id: newId('wl'), name, tickers: [], createdAt: now, updatedAt: now };
    state.watchlists = [watchlist, ...state.watchlists];
    return json(watchlist);
  }

  if (path.startsWith('/api/watchlists/') && !path.includes('/tickers') && method === 'PATCH') {
    const id = path.split('/')[3] ?? '';
    const body = JSON.parse(String(init?.body ?? '{}')) as { name?: string };
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist) return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    watchlist.name = body.name?.trim() ?? watchlist.name;
    watchlist.updatedAt = new Date().toISOString();
    return json(watchlist);
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
    if (!watchlist) return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    const ticker = body.ticker?.trim().toUpperCase() ?? '';
    if (!findTicker(ticker)) return json({ code: 'validation_error', message: 'Ticker is unknown' }, { status: 422 });
    if (watchlist.tickers.includes(ticker)) {
      return json({ code: 'duplicate_ticker', message: 'Ticker already exists' }, { status: 409 });
    }
    watchlist.tickers = [...watchlist.tickers, ticker];
    watchlist.updatedAt = new Date().toISOString();
    return json(watchlist);
  }

  if (path.includes('/tickers/') && method === 'DELETE') {
    const [_, __, ___, id, ____, ticker] = path.split('/');
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist) return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    watchlist.tickers = watchlist.tickers.filter((entry) => entry !== decodeURIComponent(ticker ?? ''));
    watchlist.updatedAt = new Date().toISOString();
    return json(watchlist);
  }

  if (path.endsWith('/ticker-order') && method === 'PUT') {
    const id = path.split('/')[3] ?? '';
    const watchlist = state.watchlists.find((entry) => entry.id === id);
    if (!watchlist) return json({ code: 'not_found', message: 'Watchlist not found' }, { status: 404 });
    const body = JSON.parse(String(init?.body ?? '{}')) as { tickers?: string[] };
    watchlist.tickers = body.tickers ?? watchlist.tickers;
    watchlist.updatedAt = new Date().toISOString();
    return json(watchlist);
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
