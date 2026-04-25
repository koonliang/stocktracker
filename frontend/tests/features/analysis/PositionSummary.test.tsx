import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PositionSummary } from '@/features/analysis/PositionSummary';
import { buildPriceLookup, computeHoldings } from '@/lib/portfolio';
import { loadPrices, loadTickers } from '@/lib/seed';
import type { Transaction } from '@/lib/types';

const HELD = loadTickers()[0]!.symbol;

const tx: Transaction = {
  id: 'tx_1',
  date: '2024-01-02',
  ticker: HELD,
  type: 'buy',
  quantity: 10,
  price: 100,
  fees: 0,
};

function buildSummary() {
  const tickerMap = new Map(loadTickers().map((ticker) => [ticker.symbol, ticker]));
  const lookup = buildPriceLookup(loadPrices());
  const holding = computeHoldings([tx], lookup, tickerMap).find((entry) => entry.ticker === HELD)!;
  return {
    shares: holding.shares,
    averageCost: holding.averageCost,
    marketValue: holding.marketValue,
    unrealizedPnL: holding.unrealizedPnL,
    unrealizedPnLPct: holding.unrealizedPnLPct,
  };
}

describe('PositionSummary', () => {
  it('renders nothing when no position summary is provided', () => {
    const { container } = render(<PositionSummary summary={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders shares, avg cost, market value, and P&L when held', () => {
    render(<PositionSummary summary={buildSummary()} />);
    expect(screen.getByText('Shares')).toBeInTheDocument();
    expect(screen.getByText('Avg Cost')).toBeInTheDocument();
    expect(screen.getByText('Market Value')).toBeInTheDocument();
    expect(screen.getByText('Unrealised P&L')).toBeInTheDocument();
  });

  it('renders the computed share count', () => {
    render(<PositionSummary summary={buildSummary()} />);
    expect(screen.getByText('10')).toBeInTheDocument();
  });
});
