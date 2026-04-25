import { describe, expect, it } from 'vitest';
import { parseTransactionsCSV, serializeTransactionsCSV } from '@/lib/csv';
import { buildPriceLookup, computeHoldings, computePortfolio } from '@/lib/portfolio';
import { loadPrices, loadTickers } from '@/lib/seed';
import type { Transaction } from '@/lib/types';

const KNOWN = loadTickers()[0]!.symbol; // AAPL
const KNOWN2 = loadTickers()[1]!.symbol; // MSFT

describe('parseTransactionsCSV', () => {
  it('parses a valid CSV with the canonical header', () => {
    const csv = [
      'date,ticker,type,quantity,price,fees',
      `2024-01-15,${KNOWN},buy,10,185.25,0`,
      `2024-02-20,${KNOWN2},buy,5,176.10,1.00`,
    ].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.headerErrors).toEqual([]);
    expect(result.invalid).toHaveLength(0);
    expect(result.valid).toHaveLength(2);
    expect(result.valid[0]!.ticker).toBe(KNOWN);
    expect(result.valid[0]!.quantity).toBe(10);
    expect(result.valid[1]!.fees).toBe(1);
  });

  it('normalizes case and trims whitespace', () => {
    const csv = [
      'date,ticker,type,quantity,price,fees',
      `  2024-01-15 , ${KNOWN.toLowerCase()} , BUY , 10 , 185.25 , 0 `,
    ].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.invalid).toHaveLength(0);
    expect(result.valid[0]!.ticker).toBe(KNOWN);
    expect(result.valid[0]!.type).toBe('buy');
  });

  it('defaults fees to 0 when the column is omitted entirely', () => {
    const csv = ['date,ticker,type,quantity,price', `2024-01-15,${KNOWN},buy,10,185.25`].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.valid).toHaveLength(1);
    expect(result.valid[0]!.fees).toBe(0);
  });

  it('defaults fees to 0 when the column is present but blank', () => {
    const csv = ['date,ticker,type,quantity,price,fees', `2024-01-15,${KNOWN},buy,10,185.25,`].join(
      '\n',
    );
    const result = parseTransactionsCSV(csv);
    expect(result.valid[0]!.fees).toBe(0);
  });

  it('flags unknown tickers', () => {
    const csv = ['date,ticker,type,quantity,price,fees', '2024-01-15,ZZZZ,buy,10,185.25,0'].join(
      '\n',
    );
    const result = parseTransactionsCSV(csv);
    expect(result.valid).toHaveLength(0);
    expect(result.invalid[0]!.reason).toMatch(/unknown ticker/);
  });

  it('flags future dates', () => {
    const csv = [
      'date,ticker,type,quantity,price,fees',
      `2099-01-01,${KNOWN},buy,10,185.25,0`,
    ].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.invalid[0]!.reason).toMatch(/future/);
  });

  it('flags non-buy/sell type', () => {
    const csv = [
      'date,ticker,type,quantity,price,fees',
      `2024-01-15,${KNOWN},swap,10,185.25,0`,
    ].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.invalid[0]!.reason).toMatch(/buy or sell/);
  });

  it('flags non-positive quantity', () => {
    const csv = ['date,ticker,type,quantity,price,fees', `2024-01-15,${KNOWN},buy,0,185.25,0`].join(
      '\n',
    );
    const result = parseTransactionsCSV(csv);
    expect(result.invalid[0]!.reason).toMatch(/quantity/);
  });

  it('flags malformed price', () => {
    const csv = ['date,ticker,type,quantity,price,fees', `2024-01-15,${KNOWN},buy,10,abc,0`].join(
      '\n',
    );
    const result = parseTransactionsCSV(csv);
    expect(result.invalid[0]!.reason).toMatch(/price/);
  });

  it('flags malformed dates', () => {
    const csv = ['date,ticker,type,quantity,price,fees', `bad-date,${KNOWN},buy,10,185.25,0`].join(
      '\n',
    );
    const result = parseTransactionsCSV(csv);
    expect(result.invalid[0]!.reason).toMatch(/date/);
  });

  it('flags negative fees', () => {
    const csv = [
      'date,ticker,type,quantity,price,fees',
      `2024-01-15,${KNOWN},buy,10,185.25,-1`,
    ].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.invalid[0]!.reason).toMatch(/fees/);
  });

  it('reports header errors when required columns are missing', () => {
    const csv = ['ticker,type,quantity,price', `${KNOWN},buy,10,185.25`].join('\n');
    const result = parseTransactionsCSV(csv);
    expect(result.headerErrors).toEqual(['missing required column: date']);
    expect(result.valid).toHaveLength(0);
  });
});

describe('serializeTransactionsCSV', () => {
  it('emits the canonical header and LF endings', () => {
    const tx: Transaction = {
      id: '1',
      date: '2024-01-15',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 185.25,
      fees: 0,
    };
    const out = serializeTransactionsCSV([tx]);
    const lines = out.split('\n');
    expect(lines[0]).toBe('date,ticker,type,quantity,price,fees');
    expect(out).not.toContain('\r');
    expect(lines[1]).toBe('2024-01-15,AAPL,buy,10,185.25,0');
  });
});

describe('CSV round-trip equivalence (SC-008)', () => {
  it('serialize → parse produces holdings/portfolio identical to the original', () => {
    const tickerMap = new Map(loadTickers().map((t) => [t.symbol, t]));
    const lookup = buildPriceLookup(loadPrices());

    const original: Transaction[] = [
      {
        id: 'a',
        date: '2024-01-15',
        ticker: KNOWN,
        type: 'buy',
        quantity: 10,
        price: 185.25,
        fees: 0,
      },
      {
        id: 'b',
        date: '2024-02-20',
        ticker: KNOWN,
        type: 'buy',
        quantity: 5,
        price: 176.1,
        fees: 0,
      },
      {
        id: 'c',
        date: '2024-03-04',
        ticker: KNOWN2,
        type: 'buy',
        quantity: 8,
        price: 415,
        fees: 0,
      },
      {
        id: 'd',
        date: '2024-07-18',
        ticker: KNOWN,
        type: 'sell',
        quantity: 4,
        price: 228.5,
        fees: 1,
      },
    ];

    const csv = serializeTransactionsCSV(original);
    const reparsed = parseTransactionsCSV(csv);
    expect(reparsed.invalid).toHaveLength(0);
    expect(reparsed.valid).toHaveLength(original.length);

    const before = computePortfolio(computeHoldings(original, lookup, tickerMap));
    const after = computePortfolio(computeHoldings(reparsed.valid, lookup, tickerMap));

    expect(after.totalMarketValue).toBeCloseTo(before.totalMarketValue, 6);
    expect(after.totalCostBasis).toBeCloseTo(before.totalCostBasis, 6);
    expect(after.totalUnrealizedPnL).toBeCloseTo(before.totalUnrealizedPnL, 6);
    expect(after.totalDayChange).toBeCloseTo(before.totalDayChange, 6);
  });
});
