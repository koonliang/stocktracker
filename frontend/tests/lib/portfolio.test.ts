import { describe, expect, it } from 'vitest';
import { buildPriceLookup, computeHoldings, computePortfolio } from '@/lib/portfolio';
import type { PriceBar, Ticker, Transaction } from '@/lib/types';

function tx(partial: Partial<Transaction>): Transaction {
  return {
    id: partial.id ?? Math.random().toString(36),
    date: partial.date ?? '2024-01-01',
    ticker: partial.ticker ?? 'AAPL',
    type: partial.type ?? 'buy',
    quantity: partial.quantity ?? 10,
    price: partial.price ?? 100,
    fees: partial.fees ?? 0,
  };
}

const tickerMap = new Map<string, Ticker>([
  ['AAPL', { symbol: 'AAPL', name: 'Apple Inc.', sector: 'Tech', exchange: 'NASDAQ' }],
  ['MSFT', { symbol: 'MSFT', name: 'Microsoft', sector: 'Tech', exchange: 'NASDAQ' }],
]);

function barsAt(prev: number, latest: number): PriceBar[] {
  const mk = (c: number): PriceBar => ({
    date: '2025-01-01',
    open: c,
    high: c,
    low: c,
    close: c,
    volume: 0,
  });
  return [mk(prev), mk(latest)];
}

describe('buildPriceLookup', () => {
  it('extracts latest and previous closes', () => {
    const lookup = buildPriceLookup({ AAPL: barsAt(150, 180) });
    expect(lookup.current.get('AAPL')).toBe(180);
    expect(lookup.previous.get('AAPL')).toBe(150);
  });

  it('handles single-bar series (no previous)', () => {
    const lookup = buildPriceLookup({
      AAPL: [{ date: '2025-01-01', open: 100, high: 100, low: 100, close: 100, volume: 0 }],
    });
    expect(lookup.current.get('AAPL')).toBe(100);
    expect(lookup.previous.get('AAPL')).toBeUndefined();
  });
});

describe('computeHoldings', () => {
  const prices = buildPriceLookup({
    AAPL: barsAt(150, 180),
    MSFT: barsAt(380, 400),
  });

  it('aggregates multiple buys using weighted-average cost basis', () => {
    const holdings = computeHoldings(
      [
        tx({ ticker: 'AAPL', date: '2024-01-01', quantity: 10, price: 100 }),
        tx({ ticker: 'AAPL', date: '2024-06-01', quantity: 10, price: 200 }),
      ],
      prices,
      tickerMap,
    );
    const aapl = holdings.find((h) => h.ticker === 'AAPL')!;
    expect(aapl.shares).toBe(20);
    expect(aapl.averageCost).toBe(150); // (10*100 + 10*200) / 20
    expect(aapl.costBasis).toBe(3000);
    expect(aapl.marketValue).toBe(3600);
    expect(aapl.unrealizedPnL).toBe(600);
    expect(aapl.unrealizedPnLPct).toBeCloseTo(0.2);
  });

  it('reduces shares on sell while preserving average cost', () => {
    const holdings = computeHoldings(
      [
        tx({ ticker: 'AAPL', date: '2024-01-01', quantity: 10, price: 100 }),
        tx({ ticker: 'AAPL', date: '2024-06-01', type: 'sell', quantity: 4, price: 180 }),
      ],
      prices,
      tickerMap,
    );
    const aapl = holdings.find((h) => h.ticker === 'AAPL')!;
    expect(aapl.shares).toBe(6);
    expect(aapl.averageCost).toBe(100);
  });

  it('excludes closed positions (shares = 0)', () => {
    const holdings = computeHoldings(
      [
        tx({ ticker: 'AAPL', date: '2024-01-01', quantity: 10, price: 100 }),
        tx({ ticker: 'AAPL', date: '2024-06-01', type: 'sell', quantity: 10, price: 180 }),
      ],
      prices,
      tickerMap,
    );
    expect(holdings.find((h) => h.ticker === 'AAPL')).toBeUndefined();
  });

  it('includes fees in weighted-average cost basis', () => {
    const holdings = computeHoldings(
      [tx({ ticker: 'AAPL', quantity: 10, price: 100, fees: 50 })],
      prices,
      tickerMap,
    );
    const aapl = holdings.find((h) => h.ticker === 'AAPL')!;
    expect(aapl.averageCost).toBe(105); // (10*100 + 50) / 10
  });

  it('computes day change from previous close', () => {
    const holdings = computeHoldings(
      [tx({ ticker: 'AAPL', quantity: 10, price: 100 })],
      prices,
      tickerMap,
    );
    const aapl = holdings.find((h) => h.ticker === 'AAPL')!;
    expect(aapl.dayChange).toBe(300); // 10 * (180 - 150)
    expect(aapl.dayChangePct).toBeCloseTo(0.2);
  });

  it('assigns correct weights', () => {
    const holdings = computeHoldings(
      [
        tx({ ticker: 'AAPL', quantity: 10, price: 100 }), // mv = 1800
        tx({ ticker: 'MSFT', quantity: 5, price: 300 }), // mv = 2000
      ],
      prices,
      tickerMap,
    );
    const total = 1800 + 2000;
    expect(holdings.find((h) => h.ticker === 'AAPL')!.weight).toBeCloseTo(1800 / total);
    expect(holdings.find((h) => h.ticker === 'MSFT')!.weight).toBeCloseTo(2000 / total);
  });
});

describe('computePortfolio', () => {
  const prices = buildPriceLookup({
    AAPL: barsAt(150, 180),
    MSFT: barsAt(380, 400),
  });

  it('reconciles totals against holdings', () => {
    const holdings = computeHoldings(
      [
        tx({ ticker: 'AAPL', quantity: 10, price: 100 }),
        tx({ ticker: 'MSFT', quantity: 5, price: 300 }),
      ],
      prices,
      tickerMap,
    );
    const p = computePortfolio(holdings);
    expect(p.totalMarketValue).toBe(1800 + 2000);
    expect(p.totalCostBasis).toBe(1000 + 1500);
    expect(p.totalUnrealizedPnL).toBe(800 + 500);
    expect(p.totalDayChange).toBe(300 + 100); // 10*30 + 5*20
  });

  it('returns zeros for an empty portfolio', () => {
    const p = computePortfolio([]);
    expect(p.totalMarketValue).toBe(0);
    expect(p.totalUnrealizedPnLPct).toBe(0);
    expect(p.totalDayChangePct).toBe(0);
  });
});
