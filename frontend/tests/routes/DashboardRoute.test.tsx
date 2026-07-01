import { beforeEach, describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { axe } from 'vitest-axe';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { renderWithProviders } from '@tests/utils';
import { seedMockPortfolio, setMockApiState } from '@tests/server';

describe('DashboardRoute', () => {
  beforeEach(() => {
    localStorage.clear();
    setMockApiState({ transactions: [] });
  });

  it('renders the empty state when no transactions', async () => {
    const { container } = renderWithProviders(<DashboardRoute />);
    expect(await screen.findByText(/Your portfolio is empty/i)).toBeInTheDocument();
    expect(container.querySelector('.recharts-surface')).toBeNull();
  });

  it('renders summary and holdings when seeded', async () => {
    seedMockPortfolio();
    renderWithProviders(<DashboardRoute />);
    expect(screen.getByRole('heading', { name: /^Portfolio$/i, level: 1 })).toBeInTheDocument();
    expect((await screen.findAllByText(/Market Value/i)).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link').length).toBeGreaterThan(0);
  });

  it('has no critical accessibility violations (seeded view)', async () => {
    seedMockPortfolio();
    const { container } = renderWithProviders(<DashboardRoute />);
    await screen.findAllByText(/Market Value/i);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
