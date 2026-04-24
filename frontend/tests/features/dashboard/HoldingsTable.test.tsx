import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HoldingsTable } from '@/features/dashboard/HoldingsTable';
import { renderWithProviders } from '@/test/utils';
import type { Holding } from '@/lib/types';

function h(overrides: Partial<Holding>): Holding {
  return {
    ticker: 'AAPL',
    name: 'Apple Inc.',
    shares: 10,
    averageCost: 100,
    costBasis: 1000,
    currentPrice: 150,
    marketValue: 1500,
    unrealizedPnL: 500,
    unrealizedPnLPct: 0.5,
    dayChange: 10,
    dayChangePct: 0.01,
    weight: 0.5,
    ...overrides,
  };
}

describe('HoldingsTable', () => {
  const holdings = [
    h({ ticker: 'AAPL', name: 'Apple Inc.', marketValue: 1500, weight: 0.3 }),
    h({ ticker: 'MSFT', name: 'Microsoft', marketValue: 3500, weight: 0.7 }),
  ];

  it('renders headers for the always-visible columns and a row for each holding', () => {
    renderWithProviders(<HoldingsTable holdings={holdings} />);
    // Only the non-responsive-hidden columns are asserted. Shares, Avg Cost, Price,
    // and Weight are hidden under various breakpoints and may be display:none at
    // jsdom's default viewport.
    for (const label of ['Ticker', 'Market Value', 'P&L', 'Today']) {
      expect(screen.getByRole('button', { name: new RegExp(label, 'i') })).toBeInTheDocument();
    }
    expect(screen.getAllByRole('link').length).toBe(2);
  });

  it('sorts by market value descending by default', () => {
    renderWithProviders(<HoldingsTable holdings={holdings} />);
    const rows = screen.getAllByRole('link');
    expect(within(rows[0]!).getByText('MSFT')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('AAPL')).toBeInTheDocument();
  });

  it('toggles sort direction on repeated header click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<HoldingsTable holdings={holdings} />);
    const mvHeader = screen.getByRole('button', { name: /Market Value/i });
    await user.click(mvHeader); // now ascending
    const rows = screen.getAllByRole('link');
    expect(within(rows[0]!).getByText('AAPL')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('MSFT')).toBeInTheDocument();
  });

  it('switches sort column', async () => {
    const user = userEvent.setup();
    renderWithProviders(<HoldingsTable holdings={holdings} />);
    await user.click(screen.getByRole('button', { name: /Ticker/i })); // sort by ticker asc
    const rows = screen.getAllByRole('link');
    expect(within(rows[0]!).getByText('AAPL')).toBeInTheDocument();
  });

  it('row is keyboard-operable (Enter activates link role)', async () => {
    const user = userEvent.setup();
    renderWithProviders(<HoldingsTable holdings={holdings} />);
    const row = screen.getAllByRole('link')[0]!;
    row.focus();
    expect(row).toHaveFocus();
    // Pressing Enter should not throw; navigation target is '/analysis/:ticker'
    await user.keyboard('{Enter}');
  });
});
