import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AddTickerInput } from '@/features/watchlist/AddTickerInput';
import { renderWithProviders } from '@/test/utils';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { loadTickers } from '@/lib/seed';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [] });
}

async function setupList(): Promise<string> {
  const res = await useWatchlistStore.getState().create('Test List');
  if (!res.ok) throw new Error('failed to create list');
  return res.id;
}

const known = loadTickers()[0]!.symbol;

describe('AddTickerInput', () => {
  beforeEach(reset);
  afterEach(reset);

  it('adds a known ticker and clears the input', async () => {
    const user = userEvent.setup();
    const id = await setupList();
    renderWithProviders(<AddTickerInput watchlistId={id} />);

    const input = screen.getByLabelText(/Add ticker/i) as HTMLInputElement;
    await user.type(input, `${known}{Enter}`);

    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known]);
    expect(input.value).toBe('');
  });

  it('shows an inline error for unknown tickers and does not add', async () => {
    const user = userEvent.setup();
    const id = await setupList();
    renderWithProviders(<AddTickerInput watchlistId={id} />);

    const input = screen.getByLabelText(/Add ticker/i);
    await user.type(input, 'ZZZZZ{Enter}');

    expect(await screen.findByRole('alert')).toHaveTextContent(/Not a known ticker/i);
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([]);
  });

  it('shows an inline error for duplicates', async () => {
    const user = userEvent.setup();
    const id = await setupList();
    await useWatchlistStore.getState().addTicker(id, known);
    renderWithProviders(<AddTickerInput watchlistId={id} />);

    const input = screen.getByLabelText(/Add ticker/i);
    await user.type(input, `${known}{Enter}`);

    expect(await screen.findByRole('alert')).toHaveTextContent(/Already in this watchlist/i);
  });
});
