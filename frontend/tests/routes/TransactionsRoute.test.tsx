import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { axe } from 'vitest-axe';
import { TransactionsRoute } from '@/routes/TransactionsRoute';
import { renderWithProviders } from '@/test/utils';
import { usePortfolioStore } from '@/stores/portfolioStore';
import type { Transaction } from '@/lib/types';

function reset() {
  localStorage.clear();
  usePortfolioStore.setState({ transactions: [], initialized: true });
}

const seeded: Transaction[] = [
  {
    id: '1',
    date: '2024-01-15',
    ticker: 'AAPL',
    type: 'buy',
    quantity: 10,
    price: 185.25,
    fees: 0,
  },
  { id: '2', date: '2024-02-20', ticker: 'MSFT', type: 'buy', quantity: 5, price: 415, fees: 0 },
];

describe('TransactionsRoute', () => {
  beforeEach(reset);
  afterEach(reset);

  it('renders the empty state when no transactions are present', () => {
    renderWithProviders(<TransactionsRoute />);
    expect(screen.getByText(/No transactions on file/i)).toBeInTheDocument();
  });

  it('renders the dropzone affordance', () => {
    renderWithProviders(<TransactionsRoute />);
    expect(screen.getByText(/Drop a CSV file to import/i)).toBeInTheDocument();
  });

  it('lists committed transactions', () => {
    usePortfolioStore.setState({ transactions: seeded, initialized: true });
    renderWithProviders(<TransactionsRoute />);
    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('MSFT')).toBeInTheDocument();
  });

  it('has no critical accessibility violations', async () => {
    usePortfolioStore.setState({ transactions: seeded, initialized: true });
    const { container } = renderWithProviders(<TransactionsRoute />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
