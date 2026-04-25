import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { axe } from 'vitest-axe';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
import { WatchlistDetailRoute } from '@/routes/WatchlistDetailRoute';
import { TransactionsRoute } from '@/routes/TransactionsRoute';
import { AnalysisRoute } from '@/routes/AnalysisRoute';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { loadSeedPortfolio, loadTickers } from '@/lib/seed';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [] });
  usePortfolioStore.setState({ transactions: loadSeedPortfolio(), initialized: true });
}

function renderRoute(path: string, element: JSX.Element, routePath: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={routePath} element={element} />
      </Routes>
    </MemoryRouter>,
  );
}

const known = loadTickers()[0]!.symbol;

describe('axe across primary routes (SC-006)', () => {
  beforeEach(reset);
  afterEach(reset);

  it('Dashboard route has no critical violations', async () => {
    const { container } = renderRoute('/', <DashboardRoute />, '/');
    expect(await axe(container)).toHaveNoViolations();
  });

  it('Watchlists index has no critical violations', async () => {
    useWatchlistStore.getState().create('Tech');
    const { container } = renderRoute('/watchlists', <WatchlistsRoute />, '/watchlists');
    expect(await axe(container)).toHaveNoViolations();
  });

  it('Watchlist detail has no critical violations', async () => {
    const res = useWatchlistStore.getState().create('Tech');
    const id = (res as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known);
    const { container } = renderRoute(
      `/watchlists/${id}`,
      <WatchlistDetailRoute />,
      '/watchlists/:id',
    );
    expect(await axe(container)).toHaveNoViolations();
  });

  it('Transactions route has no critical violations', async () => {
    const { container } = renderRoute('/transactions', <TransactionsRoute />, '/transactions');
    expect(await axe(container)).toHaveNoViolations();
  });

  it('Analysis route has no critical violations', async () => {
    const { container } = renderRoute(`/analysis/${known}`, <AnalysisRoute />, '/analysis/:ticker');
    expect(await axe(container)).toHaveNoViolations();
  });
});
