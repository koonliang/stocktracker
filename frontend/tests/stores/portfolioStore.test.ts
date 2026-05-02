import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { usePortfolioStore } from '@/stores/portfolioStore';

function reset() {
  localStorage.clear();
  usePortfolioStore.setState({ transactions: [], initialized: true });
}

describe('portfolioStore', () => {
  beforeEach(reset);
  afterEach(reset);

  it('addTransaction appends with a generated id', () => {
    const store = usePortfolioStore.getState();
    store.addTransaction({
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    const transactions = usePortfolioStore.getState().transactions;
    expect(transactions).toHaveLength(1);
    expect(transactions[0]!.id).toBeTruthy();
    expect(transactions[0]!.ticker).toBe('AAPL');
  });

  it('removeTransaction removes by id', () => {
    const store = usePortfolioStore.getState();
    store.addTransaction({
      id: 'a',
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    store.addTransaction({
      id: 'b',
      date: '2024-02-01',
      ticker: 'MSFT',
      type: 'buy',
      quantity: 5,
      price: 300,
      fees: 0,
    });
    usePortfolioStore.getState().removeTransaction('a');
    expect(usePortfolioStore.getState().transactions.map((transaction) => transaction.id)).toEqual([
      'b',
    ]);
  });

  it('replaceAll replaces the transaction list', () => {
    usePortfolioStore.getState().replaceAll([
      {
        id: 'x',
        date: '2024-03-01',
        ticker: 'NVDA',
        type: 'buy',
        quantity: 1,
        price: 500,
        fees: 0,
      },
    ]);
    expect(usePortfolioStore.getState().transactions).toHaveLength(1);
    expect(usePortfolioStore.getState().transactions[0]!.ticker).toBe('NVDA');
  });

  it('clear empties the list', () => {
    usePortfolioStore.getState().addTransaction({
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    usePortfolioStore.getState().clear();
    expect(usePortfolioStore.getState().transactions).toEqual([]);
  });

  it('seedFromFixture populates from bundled demo data', () => {
    usePortfolioStore.getState().seedFromFixture();
    expect(usePortfolioStore.getState().transactions.length).toBeGreaterThan(0);
  });

  it('keeps added transactions in memory for local test helpers', () => {
    usePortfolioStore.getState().addTransaction({
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    expect(usePortfolioStore.getState().transactions[0]!.ticker).toBe('AAPL');
  });
});
