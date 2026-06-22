import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
import { renderWithProviders } from '@/test/utils';
import { setMockApiState } from '@/test/server';

function reset() {
  localStorage.clear();
  setMockApiState({ watchlists: [] });
}

describe('WatchlistsRoute', () => {
  beforeEach(reset);
  afterEach(reset);

  it('renders the empty state and allows opening the create dialog', async () => {
    const user = userEvent.setup();
    renderWithProviders(<WatchlistsRoute />);
    expect(await screen.findByText(/Start by creating your first watchlist/i)).toBeInTheDocument();

    const [firstCta] = await screen.findAllByRole('button', { name: /New watchlist/i });
    await user.click(firstCta!);
    expect(screen.getByRole('dialog', { name: /New watchlist/i })).toBeInTheDocument();
  });

  it('creates a watchlist via the dialog', async () => {
    const user = userEvent.setup();
    renderWithProviders(<WatchlistsRoute />);
    const [firstCta] = await screen.findAllByRole('button', { name: /New watchlist/i });
    await user.click(firstCta!);

    const nameInput = screen.getByLabelText('Name') as HTMLInputElement;
    await user.type(nameInput, 'Tech Majors');
    await user.click(screen.getByRole('button', { name: /^Create$/i }));

    expect(await screen.findByText('Tech Majors')).toBeInTheDocument();
  });

  it('lists existing watchlists', async () => {
    setMockApiState({
      watchlists: [
        {
          id: 'wl_1',
          name: 'One',
          tickers: [],
          createdAt: '2024-01-01T00:00:00.000Z',
          updatedAt: '2024-01-01T00:00:00.000Z',
        },
        {
          id: 'wl_2',
          name: 'Two',
          tickers: [],
          createdAt: '2024-01-01T00:00:00.000Z',
          updatedAt: '2024-01-01T00:00:00.000Z',
        },
      ],
    });
    renderWithProviders(<WatchlistsRoute />);
    expect(await screen.findByText('One')).toBeInTheDocument();
    expect(screen.getByText('Two')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Watchlists/i, level: 1 })).toBeInTheDocument();
  });

  it('has no critical accessibility violations', async () => {
    setMockApiState({
      watchlists: [
        {
          id: 'wl_1',
          name: 'Tech',
          tickers: [],
          createdAt: '2024-01-01T00:00:00.000Z',
          updatedAt: '2024-01-01T00:00:00.000Z',
        },
      ],
    });
    const { container } = renderWithProviders(<WatchlistsRoute />);
    await screen.findByText('Tech');
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
