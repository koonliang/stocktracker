import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { WatchlistDetailRoute } from '@/routes/WatchlistDetailRoute';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { loadTickers } from '@/lib/seed';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [] });
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

  it('renders not-found when the id does not exist', () => {
    setup('does-not-exist');
    expect(screen.getByText(/This watchlist no longer exists/i)).toBeInTheDocument();
  });

  it('renders header and add input for an existing watchlist', () => {
    const res = useWatchlistStore.getState().create('Tech');
    const id = (res as { ok: true; id: string }).id;
    setup(id);
    expect(screen.getByRole('heading', { name: /Tech/i, level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText(/Add ticker/i)).toBeInTheDocument();
  });

  it('removes a ticker when its remove button is clicked', async () => {
    const user = userEvent.setup();
    const res = useWatchlistStore.getState().create('Tech');
    const id = (res as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known);
    useWatchlistStore.getState().addTicker(id, known2);

    setup(id);
    await user.click(
      screen.getByRole('button', { name: new RegExp(`Remove ${known} from watchlist`, 'i') }),
    );
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2]);
  });

  it('reorders via the move-up control', async () => {
    const user = userEvent.setup();
    const res = useWatchlistStore.getState().create('Tech');
    const id = (res as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known);
    useWatchlistStore.getState().addTicker(id, known2);

    setup(id);
    await user.click(screen.getByRole('button', { name: new RegExp(`Move ${known2} up`, 'i') }));
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2, known]);
  });

  it('has no critical accessibility violations', async () => {
    const res = useWatchlistStore.getState().create('Tech');
    const id = (res as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known);
    const { container } = setup(id);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
