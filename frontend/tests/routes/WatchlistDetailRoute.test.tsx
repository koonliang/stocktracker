import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { WatchlistDetailRoute } from '@/routes/WatchlistDetailRoute';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { loadTickers } from '@/lib/seed';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [], status: 'idle', error: null });
}

function setup(id: string) {
  return render(
    <MemoryRouter initialEntries={[`/watchlists/${id}`]}>
      <Routes>
        <Route path="/watchlists" element={<div>watchlists index</div>} />
        <Route path="/watchlists/:id" element={<WatchlistDetailRoute />} />
        <Route path="/analysis/:ticker" element={<div>analysis page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

const known = loadTickers()[0]!.symbol;
const known2 = loadTickers()[1]!.symbol;

describe('WatchlistDetailRoute', () => {
  beforeEach(reset);
  afterEach(reset);

  it('renders not-found when the id does not exist', async () => {
    setup('does-not-exist');
    expect(await screen.findByText(/This watchlist no longer exists/i)).toBeInTheDocument();
  });

  it('renders header and add input for an existing watchlist', async () => {
    const res = await useWatchlistStore.getState().create('Tech');
    if (!res.ok) throw new Error('create failed');
    setup(res.id);
    expect(await screen.findByRole('heading', { name: /Tech/i, level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText(/Search symbols/i)).toBeInTheDocument();
  });

  it('removes a ticker when its remove button is clicked', async () => {
    const user = userEvent.setup();
    const res = await useWatchlistStore.getState().create('Tech');
    if (!res.ok) throw new Error('create failed');
    await useWatchlistStore.getState().addTicker(res.id, known);
    await useWatchlistStore.getState().addTicker(res.id, known2);

    setup(res.id);
    await user.click(
      await screen.findByRole('button', {
        name: new RegExp(`Remove ${known} from watchlist`, 'i'),
      }),
    );
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2]);
  });

  it('reorders via the move-up control', async () => {
    const user = userEvent.setup();
    const res = await useWatchlistStore.getState().create('Tech');
    if (!res.ok) throw new Error('create failed');
    await useWatchlistStore.getState().addTicker(res.id, known);
    await useWatchlistStore.getState().addTicker(res.id, known2);

    setup(res.id);
    await user.click(
      await screen.findByRole('button', { name: new RegExp(`Move ${known2} up`, 'i') }),
    );
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2, known]);
  });

  it('has no critical accessibility violations', async () => {
    const res = await useWatchlistStore.getState().create('Tech');
    if (!res.ok) throw new Error('create failed');
    await useWatchlistStore.getState().addTicker(res.id, known);
    const { container } = setup(res.id);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
