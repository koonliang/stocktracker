import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { DashboardEmptyState } from '@/features/dashboard/DashboardEmptyState';
import { renderWithProviders } from '@tests/utils';

describe('DashboardEmptyState', () => {
  it('renders CTAs linking to transactions and watchlists', () => {
    renderWithProviders(<DashboardEmptyState />);
    const importLink = screen.getByRole('link', { name: /Import transactions/i });
    const watchlistLink = screen.getByRole('link', { name: /Create a watchlist/i });
    expect(importLink).toHaveAttribute('href', '/transactions');
    expect(watchlistLink).toHaveAttribute('href', '/watchlists');
  });
});
