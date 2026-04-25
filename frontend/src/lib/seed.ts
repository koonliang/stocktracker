import tickersData from '@/data/tickers.json';
import pricesData from '@/data/prices.json';
import statsData from '@/data/stats.json';
import seedPortfolioData from '@/data/seed-portfolio.json';
import type { KeyStats, PriceBar, Ticker, Transaction } from './types';

const tickers = tickersData as Ticker[];
const prices = pricesData as Record<string, PriceBar[]>;
const stats = statsData as Record<string, KeyStats>;
const seedPortfolio = seedPortfolioData as Transaction[];

const tickerBySymbol = new Map<string, Ticker>(tickers.map((t) => [t.symbol, t]));

export function loadTickers(): Ticker[] {
  return tickers;
}

export function loadPrices(): Record<string, PriceBar[]> {
  return prices;
}

export function loadStats(): Record<string, KeyStats> {
  return stats;
}

export function loadSeedPortfolio(): Transaction[] {
  return seedPortfolio;
}

export function findTicker(symbol: string): Ticker | undefined {
  return tickerBySymbol.get(symbol.toUpperCase());
}

export function isKnownTicker(symbol: string): boolean {
  return tickerBySymbol.has(symbol.toUpperCase());
}

export function getBars(symbol: string): PriceBar[] {
  return prices[symbol.toUpperCase()] ?? [];
}

export function getLatestBar(symbol: string): PriceBar | undefined {
  const bars = getBars(symbol);
  return bars[bars.length - 1];
}

export function getPreviousBar(symbol: string): PriceBar | undefined {
  const bars = getBars(symbol);
  return bars[bars.length - 2];
}
