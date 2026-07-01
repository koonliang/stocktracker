import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Routes, Route } from 'react-router-dom';
import { WatchlistRow } from '@/features/watchlist/WatchlistRow';
import { renderWithProviders } from '@tests/utils';

function renderRow(overrides: Partial<React.ComponentProps<typeof WatchlistRow>> = {}) {
  const onRemove = overrides.onRemove ?? vi.fn();
  const utils = renderWithProviders(
    <Routes>
      <Route
        path="/"
        element={
          <ul>
            <WatchlistRow
              symbol="AAPL"
              name="Apple Inc."
              currentPrice={200}
              dayChange={3.5}
              dayChangePct={0.0175}
              onRemove={onRemove}
              {...overrides}
            />
          </ul>
        }
      />
      <Route path="/analysis/:ticker" element={<div>analysis page for ticker</div>} />
    </Routes>,
  );
  return { ...utils, onRemove };
}

describe('WatchlistRow', () => {
  it('renders symbol, name, price, and day change', () => {
    renderRow();
    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('Apple Inc.')).toBeInTheDocument();
    expect(screen.getByText(/\$200\.00/)).toBeInTheDocument();
    expect(screen.getByText(/\+\$3\.50/)).toBeInTheDocument();
  });

  it('navigates to analysis when the row body is clicked', async () => {
    const user = userEvent.setup();
    renderRow();
    await user.click(screen.getByRole('button', { name: /Open analysis for AAPL/i }));
    expect(await screen.findByText(/analysis page for ticker/i)).toBeInTheDocument();
  });

  it('invokes onRemove when the remove button is clicked', async () => {
    const user = userEvent.setup();
    const { onRemove } = renderRow();
    await user.click(screen.getByRole('button', { name: /Remove AAPL from watchlist/i }));
    expect(onRemove).toHaveBeenCalledTimes(1);
  });

  it('disables move buttons at list boundaries', () => {
    renderRow({ canMoveUp: false, canMoveDown: true, onMoveUp: vi.fn(), onMoveDown: vi.fn() });
    expect(screen.getByRole('button', { name: /Move AAPL up/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Move AAPL down/i })).toBeEnabled();
  });

  it('renders em dash for missing price', () => {
    renderRow({ currentPrice: null, dayChange: null, dayChangePct: null });
    // At least one em dash for price/day change
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });
});
