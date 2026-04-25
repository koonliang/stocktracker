import { beforeEach, describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { axe } from 'vitest-axe';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { renderWithProviders } from '@/test/utils';
import { usePortfolioStore } from '@/stores/portfolioStore';

describe('DashboardRoute', () => {
  beforeEach(() => {
    localStorage.clear();
    usePortfolioStore.setState({ transactions: [], initialized: true });
  });

  it('renders the empty state when no transactions', () => {
    const { container } = renderWithProviders(<DashboardRoute />);
    expect(screen.getByText(/Your portfolio is a blank page/i)).toBeInTheDocument();
    // No Recharts SVG rendered in empty state
    expect(container.querySelector('.recharts-surface')).toBeNull();
  });

  it('renders summary and holdings when seeded', async () => {
    usePortfolioStore.getState().seedFromFixture();
    renderWithProviders(<DashboardRoute />);
    expect(screen.getByRole('heading', { name: /^Portfolio$/i, level: 1 })).toBeInTheDocument();
    expect(screen.getAllByText(/Market Value/i).length).toBeGreaterThan(0);
    // At least one holding is rendered
    expect(screen.getAllByRole('link').length).toBeGreaterThan(0);
  });

  it('has no critical accessibility violations (seeded view)', async () => {
    usePortfolioStore.getState().seedFromFixture();
    const { container } = renderWithProviders(<DashboardRoute />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
