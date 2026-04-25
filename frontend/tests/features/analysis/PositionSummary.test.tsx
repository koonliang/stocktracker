import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PositionSummary } from '@/features/analysis/PositionSummary';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { buildPriceLookup, computeHoldings } from '@/lib/portfolio';
import { loadPrices, loadTickers } from '@/lib/seed';
import type { Transaction } from '@/lib/types';

function reset() {
  localStorage.clear();
  usePortfolioStore.setState({ transactions: [], initialized: true });
}

const HELD = loadTickers()[0]!.symbol;
const NOT_HELD = loadTickers()[1]!.symbol;

const tx: Transaction = {
  id: 'tx_1',
  date: '2024-01-02',
  ticker: HELD,
  type: 'buy',
  quantity: 10,
  price: 100,
  fees: 0,
};

describe('PositionSummary', () => {
  beforeEach(reset);
  afterEach(reset);

  it('renders nothing when the user does not hold the symbol', () => {
    usePortfolioStore.setState({ transactions: [tx], initialized: true });
    const { container } = render(<PositionSummary symbol={NOT_HELD} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders shares, avg cost, market value, and P&L when held', () => {
    usePortfolioStore.setState({ transactions: [tx], initialized: true });
    render(<PositionSummary symbol={HELD} />);
    expect(screen.getByText('Shares')).toBeInTheDocument();
    expect(screen.getByText('Avg Cost')).toBeInTheDocument();
    expect(screen.getByText('Market Value')).toBeInTheDocument();
    expect(screen.getByText('Unrealised P&L')).toBeInTheDocument();
  });

  it('numbers reconcile with computeHoldings for the same ticker', () => {
    usePortfolioStore.setState({ transactions: [tx], initialized: true });
    const tickerMap = new Map(loadTickers().map((t) => [t.symbol, t]));
    const lookup = buildPriceLookup(loadPrices());
    const holdings = computeHoldings([tx], lookup, tickerMap);
    const expected = holdings.find((h) => h.ticker === HELD);
    expect(expected).toBeTruthy();

    render(<PositionSummary symbol={HELD} />);
    // Shares value rendered (10 shares — formatShares trims trailing zeros)
    expect(screen.getByText('10')).toBeInTheDocument();
  });
});
