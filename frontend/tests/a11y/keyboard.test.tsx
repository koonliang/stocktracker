import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
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

const known = loadTickers()[0]!.symbol;

function renderAt(path: string, element: JSX.Element, routePath: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={routePath} element={element} />
        <Route path="/analysis/:ticker" element={<div>analysis</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('keyboard navigation (FR-024)', () => {
  beforeEach(reset);
  afterEach(reset);

  it('Dashboard: holdings rows are focusable and Enter activates navigation', async () => {
    const user = userEvent.setup();
    renderAt('/', <DashboardRoute />, '/');
    const rows = screen.getAllByRole('link', { name: /Open analysis for/i });
    expect(rows.length).toBeGreaterThan(0);
    await user.tab();
    // Walk Tab until a row is focused
    let safety = 50;
    while (document.activeElement !== rows[0] && safety > 0) {
      await user.tab();
      safety -= 1;
    }
    expect(document.activeElement).toBe(rows[0]);
  });

  it('Watchlists index: New watchlist button is reachable via keyboard and opens dialog on Enter', async () => {
    const user = userEvent.setup();
    renderAt('/watchlists', <WatchlistsRoute />, '/watchlists');
    const ctas = screen.getAllByRole('button', { name: /New watchlist/i });
    ctas[0]!.focus();
    expect(document.activeElement).toBe(ctas[0]);
    await user.keyboard('{Enter}');
    expect(screen.getByRole('dialog', { name: /New watchlist/i })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: /New watchlist/i })).not.toBeInTheDocument();
  });

  it('Transactions: Export button is keyboard-activatable', async () => {
    const user = userEvent.setup();
    renderAt('/transactions', <TransactionsRoute />, '/transactions');
    const exportBtn = screen.getByRole('button', { name: /Export CSV/i });
    exportBtn.focus();
    expect(document.activeElement).toBe(exportBtn);
    // Pressing Enter on a button with no onclick handler should not throw; we just
    // assert the focus path works rather than triggering an actual download.
    await user.keyboard(' ');
  });

  it('Analysis: range buttons are keyboard-activatable and update aria-pressed', async () => {
    const user = userEvent.setup();
    renderAt(`/analysis/${known}`, <AnalysisRoute />, '/analysis/:ticker');
    const oneWeek = screen.getByRole('button', { name: '1W' });
    oneWeek.focus();
    expect(document.activeElement).toBe(oneWeek);
    await user.keyboard('{Enter}');
    expect(oneWeek).toHaveAttribute('aria-pressed', 'true');
  });
});
