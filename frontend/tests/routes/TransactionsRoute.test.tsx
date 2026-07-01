import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { TransactionsRoute } from '@/routes/TransactionsRoute';
import { renderWithProviders } from '@tests/utils';
import { setMockApiState } from '@tests/server';
import type { Transaction } from '@/lib/types';

function reset() {
  localStorage.clear();
  setMockApiState({ transactions: [] });
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

  it('renders the empty state when no transactions are present', async () => {
    renderWithProviders(<TransactionsRoute />);
    expect(await screen.findByText(/No transactions on file/i)).toBeInTheDocument();
  });

  it('keeps manual entry closed by default on mobile', () => {
    renderWithProviders(<TransactionsRoute />);
    expect(screen.queryByText(/Record a transaction manually/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Drop a CSV file to import/i)).not.toBeInTheDocument();
  });

  it('opens manual entry when FAB is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionsRoute />);

    await user.click(screen.getByTestId('fab'));

    expect(screen.getByText(/Record a transaction manually/i)).toBeInTheDocument();
    expect(screen.getByTestId('transaction-form')).toBeInTheDocument();
    expect(screen.queryByText(/Drop a CSV file to import/i)).not.toBeInTheDocument();
  });

  it('renders desktop header actions for transaction workflows', () => {
    renderWithProviders(<TransactionsRoute />);
    expect(screen.getAllByRole('button', { name: /New Transaction/i })).toHaveLength(2);
    expect(screen.getByRole('button', { name: /Import CSV/i })).toBeInTheDocument();
  });

  it('lists committed transactions', async () => {
    setMockApiState({ transactions: seeded });
    renderWithProviders(<TransactionsRoute />);
    expect((await screen.findAllByText('AAPL'))[0]).toBeInTheDocument();
    expect(screen.getAllByText('MSFT')[0]).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
  });

  it('shows computed trade amount when a buy row has no stored amount', async () => {
    setMockApiState({
      transactions: [
        {
          id: 'manual-buy',
          date: '2026-06-13',
          ticker: 'AAPL',
          type: 'buy',
          quantity: 2,
          price: 320,
          fees: 1.4,
          amount: null,
          currency: 'USD',
        },
      ],
    });

    renderWithProviders(<TransactionsRoute />);

    expect((await screen.findAllByText('$641.40'))[0]).toBeInTheDocument();
  });

  it('keeps the ticker smart-search dropdown closed after selecting a result', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TransactionsRoute />);
    await user.click(screen.getByTestId('fab'));

    await user.type(screen.getByTestId('transaction-ticker-search'), 'AAP');
    const result = await screen.findByTestId('transaction-ticker-result');
    await user.click(result);

    await waitFor(() => {
      expect(screen.getByTestId('transaction-ticker-search')).toHaveValue('AAPL');
      expect(screen.queryByTestId('transaction-ticker-result')).not.toBeInTheDocument();
    });

    await new Promise((resolve) => setTimeout(resolve, 350));
    expect(screen.queryByTestId('transaction-ticker-result')).not.toBeInTheDocument();
  });

  it('renders FAB for recording a transaction', () => {
    renderWithProviders(<TransactionsRoute />);
    expect(screen.getByTestId('fab')).toBeInTheDocument();
    expect(screen.getByTestId('fab')).toHaveAttribute('aria-label', 'New transaction');
  });

  it('has no critical accessibility violations', async () => {
    setMockApiState({ transactions: seeded });
    const { container } = renderWithProviders(<TransactionsRoute />);
    await screen.findAllByText('AAPL');
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
