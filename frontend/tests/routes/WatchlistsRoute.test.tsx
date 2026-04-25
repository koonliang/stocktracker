import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
import { renderWithProviders } from '@/test/utils';
import { useWatchlistStore } from '@/stores/watchlistStore';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [] });
}

describe('WatchlistsRoute', () => {
  beforeEach(reset);
  afterEach(reset);

  it('renders the empty state and allows opening the create dialog', async () => {
    const user = userEvent.setup();
    renderWithProviders(<WatchlistsRoute />);
    expect(screen.getByText(/Start by creating your first watchlist/i)).toBeInTheDocument();

    // Two "New watchlist" CTAs (header + empty state) — either opens the dialog
    const [firstCta] = screen.getAllByRole('button', { name: /New watchlist/i });
    await user.click(firstCta!);
    expect(screen.getByRole('dialog', { name: /New watchlist/i })).toBeInTheDocument();
  });

  it('creates a watchlist via the dialog', async () => {
    const user = userEvent.setup();
    renderWithProviders(<WatchlistsRoute />);
    const [firstCta] = screen.getAllByRole('button', { name: /New watchlist/i });
    await user.click(firstCta!);

    const nameInput = screen.getByLabelText('Name') as HTMLInputElement;
    await user.type(nameInput, 'Tech Majors');
    await user.click(screen.getByRole('button', { name: /^Create$/i }));

    expect(useWatchlistStore.getState().watchlists).toHaveLength(1);
    expect(useWatchlistStore.getState().watchlists[0]!.name).toBe('Tech Majors');
  });

  it('lists existing watchlists', () => {
    useWatchlistStore.getState().create('One');
    useWatchlistStore.getState().create('Two');
    renderWithProviders(<WatchlistsRoute />);
    expect(screen.getByText('One')).toBeInTheDocument();
    expect(screen.getByText('Two')).toBeInTheDocument();
  });

  it('has no critical accessibility violations', async () => {
    useWatchlistStore.getState().create('Tech');
    const { container } = renderWithProviders(<WatchlistsRoute />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
