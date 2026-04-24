/*
 * Deterministic synthetic seed generator for StockTracker.
 *
 * Produces:
 *   - src/data/tickers.json       — 30 tickers across 6 sectors
 *   - src/data/prices.json        — 5 years of daily OHLCV per ticker
 *   - src/data/stats.json         — per-ticker key statistics (latest session)
 *   - src/data/seed-portfolio.json — 7 holdings with 2–3 transactions each
 *
 * All randomness is seeded (mulberry32) so re-runs are identical.
 */

import { writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT = join(__dirname, '..', 'src', 'data');
mkdirSync(OUT, { recursive: true });

// ------------ seeded RNG ------------
function mulberry32(seed: number) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

type Ticker = {
  symbol: string;
  name: string;
  sector: string;
  exchange: 'NASDAQ' | 'NYSE';
};

const TICKERS: Ticker[] = [
  // Tech
  { symbol: 'AAPL', name: 'Apple Inc.', sector: 'Technology', exchange: 'NASDAQ' },
  { symbol: 'MSFT', name: 'Microsoft Corporation', sector: 'Technology', exchange: 'NASDAQ' },
  { symbol: 'NVDA', name: 'NVIDIA Corporation', sector: 'Technology', exchange: 'NASDAQ' },
  { symbol: 'GOOGL', name: 'Alphabet Inc. Class A', sector: 'Technology', exchange: 'NASDAQ' },
  { symbol: 'META', name: 'Meta Platforms, Inc.', sector: 'Technology', exchange: 'NASDAQ' },
  { symbol: 'ADBE', name: 'Adobe Inc.', sector: 'Technology', exchange: 'NASDAQ' },

  // Consumer
  { symbol: 'AMZN', name: 'Amazon.com, Inc.', sector: 'Consumer', exchange: 'NASDAQ' },
  { symbol: 'TSLA', name: 'Tesla, Inc.', sector: 'Consumer', exchange: 'NASDAQ' },
  { symbol: 'NKE', name: 'NIKE, Inc.', sector: 'Consumer', exchange: 'NYSE' },
  { symbol: 'SBUX', name: 'Starbucks Corporation', sector: 'Consumer', exchange: 'NASDAQ' },
  { symbol: 'COST', name: 'Costco Wholesale Corporation', sector: 'Consumer', exchange: 'NASDAQ' },

  // Financials
  { symbol: 'JPM', name: 'JPMorgan Chase & Co.', sector: 'Financials', exchange: 'NYSE' },
  { symbol: 'V', name: 'Visa Inc.', sector: 'Financials', exchange: 'NYSE' },
  { symbol: 'MA', name: 'Mastercard Incorporated', sector: 'Financials', exchange: 'NYSE' },
  {
    symbol: 'BRK.B',
    name: 'Berkshire Hathaway Inc. Class B',
    sector: 'Financials',
    exchange: 'NYSE',
  },

  // Healthcare
  { symbol: 'JNJ', name: 'Johnson & Johnson', sector: 'Healthcare', exchange: 'NYSE' },
  { symbol: 'UNH', name: 'UnitedHealth Group Inc.', sector: 'Healthcare', exchange: 'NYSE' },
  { symbol: 'LLY', name: 'Eli Lilly and Company', sector: 'Healthcare', exchange: 'NYSE' },
  { symbol: 'PFE', name: 'Pfizer Inc.', sector: 'Healthcare', exchange: 'NYSE' },

  // Industrials / Energy
  { symbol: 'BA', name: 'The Boeing Company', sector: 'Industrials', exchange: 'NYSE' },
  { symbol: 'CAT', name: 'Caterpillar Inc.', sector: 'Industrials', exchange: 'NYSE' },
  { symbol: 'XOM', name: 'Exxon Mobil Corporation', sector: 'Energy', exchange: 'NYSE' },
  { symbol: 'CVX', name: 'Chevron Corporation', sector: 'Energy', exchange: 'NYSE' },

  // Communications / Media
  { symbol: 'NFLX', name: 'Netflix, Inc.', sector: 'Communications', exchange: 'NASDAQ' },
  { symbol: 'DIS', name: 'The Walt Disney Company', sector: 'Communications', exchange: 'NYSE' },
  { symbol: 'T', name: 'AT&T Inc.', sector: 'Communications', exchange: 'NYSE' },

  // Utilities / Real Estate
  { symbol: 'NEE', name: 'NextEra Energy, Inc.', sector: 'Utilities', exchange: 'NYSE' },
  { symbol: 'DUK', name: 'Duke Energy Corporation', sector: 'Utilities', exchange: 'NYSE' },

  // Semiconductors
  { symbol: 'AMD', name: 'Advanced Micro Devices, Inc.', sector: 'Technology', exchange: 'NASDAQ' },
  {
    symbol: 'TSM',
    name: 'Taiwan Semiconductor Manufacturing',
    sector: 'Technology',
    exchange: 'NYSE',
  },
];

// Starting price (approximate, not real market) and annualized drift/vol per ticker.
const PROFILE: Record<string, { start: number; drift: number; vol: number; shares: number }> = {
  AAPL: { start: 72, drift: 0.18, vol: 0.25, shares: 15.3e9 },
  MSFT: { start: 160, drift: 0.19, vol: 0.24, shares: 7.4e9 },
  NVDA: { start: 55, drift: 0.55, vol: 0.48, shares: 2.45e9 },
  GOOGL: { start: 70, drift: 0.12, vol: 0.28, shares: 12.6e9 },
  META: { start: 200, drift: 0.14, vol: 0.38, shares: 2.55e9 },
  ADBE: { start: 300, drift: 0.11, vol: 0.32, shares: 0.45e9 },
  AMZN: { start: 90, drift: 0.17, vol: 0.32, shares: 10.6e9 },
  TSLA: { start: 30, drift: 0.25, vol: 0.55, shares: 3.18e9 },
  NKE: { start: 90, drift: 0.04, vol: 0.26, shares: 1.51e9 },
  SBUX: { start: 80, drift: 0.06, vol: 0.24, shares: 1.13e9 },
  COST: { start: 290, drift: 0.2, vol: 0.2, shares: 0.44e9 },
  JPM: { start: 130, drift: 0.12, vol: 0.24, shares: 2.88e9 },
  V: { start: 180, drift: 0.13, vol: 0.22, shares: 1.82e9 },
  MA: { start: 285, drift: 0.13, vol: 0.22, shares: 0.93e9 },
  'BRK.B': { start: 225, drift: 0.1, vol: 0.18, shares: 2.15e9 },
  JNJ: { start: 145, drift: 0.04, vol: 0.18, shares: 2.41e9 },
  UNH: { start: 300, drift: 0.12, vol: 0.22, shares: 0.93e9 },
  LLY: { start: 170, drift: 0.35, vol: 0.28, shares: 0.95e9 },
  PFE: { start: 38, drift: -0.02, vol: 0.24, shares: 5.67e9 },
  BA: { start: 330, drift: -0.04, vol: 0.35, shares: 0.61e9 },
  CAT: { start: 140, drift: 0.17, vol: 0.28, shares: 0.5e9 },
  XOM: { start: 65, drift: 0.12, vol: 0.3, shares: 4.1e9 },
  CVX: { start: 110, drift: 0.08, vol: 0.26, shares: 1.82e9 },
  NFLX: { start: 340, drift: 0.13, vol: 0.4, shares: 0.44e9 },
  DIS: { start: 140, drift: -0.05, vol: 0.28, shares: 1.82e9 },
  T: { start: 32, drift: -0.04, vol: 0.22, shares: 7.18e9 },
  NEE: { start: 60, drift: 0.06, vol: 0.22, shares: 2.06e9 },
  DUK: { start: 90, drift: 0.02, vol: 0.18, shares: 0.77e9 },
  AMD: { start: 36, drift: 0.3, vol: 0.5, shares: 1.62e9 },
  TSM: { start: 60, drift: 0.22, vol: 0.32, shares: 5.19e9 },
};

// ------------ date helpers ------------
function isWeekend(d: Date) {
  const day = d.getUTCDay();
  return day === 0 || day === 6;
}
function toISO(d: Date) {
  return d.toISOString().slice(0, 10);
}
function addDays(d: Date, n: number) {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + n);
  return x;
}

// ------------ Gaussian via Box-Muller on seeded uniform ------------
function gaussian(rng: () => number) {
  let u = 0,
    v = 0;
  while (u === 0) u = rng();
  while (v === 0) v = rng();
  return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
}

// ------------ simulate one ticker's daily OHLCV ------------
type Bar = { date: string; open: number; high: number; low: number; close: number; volume: number };

function simulateSeries(symbol: string, startDate: Date, endDate: Date, rngSeed: number): Bar[] {
  const profile = PROFILE[symbol];
  if (!profile) throw new Error(`Missing profile for ${symbol}`);
  const rng = mulberry32(rngSeed);
  const dt = 1 / 252; // one trading day
  const mu = profile.drift;
  const sigma = profile.vol;

  const bars: Bar[] = [];
  let price = profile.start;
  for (let d = new Date(startDate); d <= endDate; d = addDays(d, 1)) {
    if (isWeekend(d)) continue;
    const z = gaussian(rng);
    const logReturn = (mu - 0.5 * sigma * sigma) * dt + sigma * Math.sqrt(dt) * z;
    const close = price * Math.exp(logReturn);

    // Intraday range: open near prev close with small gap, high/low widen by vol
    const open = price * (1 + gaussian(rng) * sigma * 0.05);
    const intraVol = Math.abs(gaussian(rng)) * sigma * 0.3;
    const high = Math.max(open, close) * (1 + intraVol * 0.5);
    const low = Math.min(open, close) * (1 - intraVol * 0.5);
    const volume = Math.round(
      profile.shares * 0.002 * (0.6 + rng() * 0.8) * (1 + Math.abs(z) * 0.5),
    );

    bars.push({
      date: toISO(d),
      open: round4(open),
      high: round4(high),
      low: round4(low),
      close: round4(close),
      volume,
    });
    price = close;
  }
  return bars;
}

function round4(n: number) {
  return Math.round(n * 10000) / 10000;
}
function round2(n: number) {
  return Math.round(n * 100) / 100;
}

// ------------ run ------------
const END = new Date(Date.UTC(2026, 3, 24)); // 2026-04-24
const START = addDays(END, -365 * 5 - 10); // ~5 years

const pricesOut: Record<string, Bar[]> = {};
const statsOut: Record<
  string,
  {
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
  }
> = {};

TICKERS.forEach((t, i) => {
  const series = simulateSeries(t.symbol, START, END, 0x5ee7 + i * 137);
  pricesOut[t.symbol] = series;

  const last = series[series.length - 1]!;
  const prev = series[series.length - 2] ?? last;
  const last252 = series.slice(-252);
  const week52High = round2(Math.max(...last252.map((b) => b.high)));
  const week52Low = round2(Math.min(...last252.map((b) => b.low)));
  const profile = PROFILE[t.symbol]!;
  const marketCap = Math.round(last.close * profile.shares);
  // Synthetic earnings yield → P/E; allow null for one or two to exercise the "—" render path.
  const peSeed = (i % 11) + 14; // 14..24
  const peRatio = t.symbol === 'PFE' || t.symbol === 'T' ? null : peSeed + (i % 3);

  statsOut[t.symbol] = {
    symbol: t.symbol,
    open: last.open,
    high: last.high,
    low: last.low,
    previousClose: prev.close,
    volume: last.volume,
    week52High,
    week52Low,
    marketCap,
    peRatio,
  };
});

// ------------ seed portfolio: 7 holdings with 2–3 transactions each ------------
type SeedTx = {
  id: string;
  date: string;
  ticker: string;
  type: 'buy' | 'sell';
  quantity: number;
  price: number;
  fees: number;
};

function priceOnOrBefore(symbol: string, iso: string): number {
  const series = pricesOut[symbol]!;
  for (let i = series.length - 1; i >= 0; i--) {
    if (series[i]!.date <= iso) return series[i]!.close;
  }
  return series[0]!.close;
}

// deterministic UUID-ish ids (v4 shape) seeded
const idRng = mulberry32(0xbeef);
function uid() {
  const hex = (n: number) =>
    Math.floor(idRng() * 16 ** n)
      .toString(16)
      .padStart(n, '0');
  return `${hex(8)}-${hex(4)}-4${hex(3)}-a${hex(3)}-${hex(12)}`;
}

const PLAN: Array<{
  ticker: string;
  dates: string[];
  qtys: number[];
  sellDate?: string;
  sellQty?: number;
}> = [
  { ticker: 'AAPL', dates: ['2023-02-14', '2023-10-03'], qtys: [25, 15] },
  { ticker: 'MSFT', dates: ['2022-06-20', '2024-01-09'], qtys: [18, 12] },
  { ticker: 'NVDA', dates: ['2023-01-17', '2023-07-11', '2024-05-22'], qtys: [20, 10, 8] },
  {
    ticker: 'GOOGL',
    dates: ['2022-11-30', '2024-02-06'],
    qtys: [30, 15],
    sellDate: '2025-03-10',
    sellQty: 10,
  },
  { ticker: 'COST', dates: ['2023-04-12'], qtys: [8] },
  { ticker: 'JPM', dates: ['2022-09-19', '2024-08-05'], qtys: [22, 14] },
  { ticker: 'V', dates: ['2023-05-30', '2025-01-21'], qtys: [16, 9] },
];

const txs: SeedTx[] = [];
for (const p of PLAN) {
  for (let i = 0; i < p.dates.length; i++) {
    txs.push({
      id: uid(),
      date: p.dates[i]!,
      ticker: p.ticker,
      type: 'buy',
      quantity: p.qtys[i]!,
      price: round4(priceOnOrBefore(p.ticker, p.dates[i]!)),
      fees: 0,
    });
  }
  if (p.sellDate && p.sellQty) {
    txs.push({
      id: uid(),
      date: p.sellDate,
      ticker: p.ticker,
      type: 'sell',
      quantity: p.sellQty,
      price: round4(priceOnOrBefore(p.ticker, p.sellDate)),
      fees: 1.0,
    });
  }
}
txs.sort((a, b) => a.date.localeCompare(b.date));

// ------------ write ------------
writeFileSync(join(OUT, 'tickers.json'), JSON.stringify(TICKERS, null, 2) + '\n');
writeFileSync(join(OUT, 'prices.json'), JSON.stringify(pricesOut) + '\n');
writeFileSync(join(OUT, 'stats.json'), JSON.stringify(statsOut, null, 2) + '\n');
writeFileSync(join(OUT, 'seed-portfolio.json'), JSON.stringify(txs, null, 2) + '\n');

console.log(
  `wrote ${TICKERS.length} tickers, ${Object.values(pricesOut).reduce((a, b) => a + b.length, 0)} bars, ${txs.length} seed transactions`,
);
