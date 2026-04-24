import type { Holding, PortfolioSummary, PriceBar, Ticker, Transaction } from './types';

/**
 * Weighted-average cost-basis aggregation. On buy, average cost updates as a
 * volume-weighted mean of the existing position and the new lot. On sell, shares
 * are reduced but average cost is preserved (classic running-average method).
 * Closed positions (shares === 0) are returned with zero values and excluded by
 * the dashboard caller.
 *
 * Inputs:
 *   - transactions: ordered arbitrarily; this function sorts by date internally.
 *   - priceMap:     latest and previous close for each symbol (derived from PriceBar[]).
 *   - tickerMap:    Ticker metadata (for name).
 */
export type PriceLookup = {
  current: Map<string, number>;
  previous: Map<string, number>;
};

export function buildPriceLookup(prices: Record<string, PriceBar[]>): PriceLookup {
  const current = new Map<string, number>();
  const previous = new Map<string, number>();
  for (const [symbol, bars] of Object.entries(prices)) {
    if (bars.length > 0) current.set(symbol, bars[bars.length - 1]!.close);
    if (bars.length > 1) previous.set(symbol, bars[bars.length - 2]!.close);
  }
  return { current, previous };
}

type Accum = { shares: number; avgCost: number };

export function computeHoldings(
  transactions: Transaction[],
  prices: PriceLookup,
  tickerMap: Map<string, Ticker>,
): Holding[] {
  const accum = new Map<string, Accum>();
  const sorted = [...transactions].sort((a, b) => a.date.localeCompare(b.date));

  for (const tx of sorted) {
    const cur = accum.get(tx.ticker) ?? { shares: 0, avgCost: 0 };
    if (tx.type === 'buy') {
      const newShares = cur.shares + tx.quantity;
      const costWithFees = tx.quantity * tx.price + tx.fees;
      cur.avgCost = newShares > 0 ? (cur.shares * cur.avgCost + costWithFees) / newShares : 0;
      cur.shares = newShares;
    } else {
      // sell: reduce shares; preserve avg cost; cap at 0 (don't allow negative shares)
      cur.shares = Math.max(0, cur.shares - tx.quantity);
      if (cur.shares === 0) cur.avgCost = 0;
    }
    accum.set(tx.ticker, cur);
  }

  const holdings: Holding[] = [];
  let totalMv = 0;

  for (const [symbol, pos] of accum) {
    if (pos.shares <= 0) continue;
    const currentPrice = prices.current.get(symbol) ?? 0;
    const prevClose = prices.previous.get(symbol) ?? currentPrice;
    const marketValue = pos.shares * currentPrice;
    const costBasis = pos.shares * pos.avgCost;
    const pnl = marketValue - costBasis;
    const dayChange = pos.shares * (currentPrice - prevClose);
    holdings.push({
      ticker: symbol,
      name: tickerMap.get(symbol)?.name ?? symbol,
      shares: pos.shares,
      averageCost: pos.avgCost,
      costBasis,
      currentPrice,
      marketValue,
      unrealizedPnL: pnl,
      unrealizedPnLPct: costBasis > 0 ? pnl / costBasis : 0,
      dayChange,
      dayChangePct: prevClose > 0 ? (currentPrice - prevClose) / prevClose : 0,
      weight: 0, // filled below
    });
    totalMv += marketValue;
  }

  for (const h of holdings) {
    h.weight = totalMv > 0 ? h.marketValue / totalMv : 0;
  }

  return holdings.sort((a, b) => b.marketValue - a.marketValue);
}

export function computePortfolio(holdings: Holding[]): PortfolioSummary {
  let totalMarketValue = 0;
  let totalCostBasis = 0;
  let totalDayChange = 0;
  for (const h of holdings) {
    totalMarketValue += h.marketValue;
    totalCostBasis += h.costBasis;
    totalDayChange += h.dayChange;
  }
  const totalUnrealizedPnL = totalMarketValue - totalCostBasis;
  const prevTotal = totalMarketValue - totalDayChange;
  return {
    totalMarketValue,
    totalCostBasis,
    totalUnrealizedPnL,
    totalUnrealizedPnLPct: totalCostBasis > 0 ? totalUnrealizedPnL / totalCostBasis : 0,
    totalDayChange,
    totalDayChangePct: prevTotal > 0 ? totalDayChange / prevTotal : 0,
  };
}
