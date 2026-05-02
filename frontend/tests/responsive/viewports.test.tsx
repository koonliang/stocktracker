import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
import { WatchlistDetailRoute } from '@/routes/WatchlistDetailRoute';
import { TransactionsRoute } from '@/routes/TransactionsRoute';
import { AnalysisRoute } from '@/routes/AnalysisRoute';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { loadTickers } from '@/lib/seed';

const VIEWPORTS = [375, 768, 1280, 1920] as const;

function setViewport(width: number) {
  Object.defineProperty(window, 'innerWidth', { configurable: true, value: width });
  Object.defineProperty(window, 'innerHeight', { configurable: true, value: 800 });
  window.dispatchEvent(new Event('resize'));
}

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({
    watchlists: [],
    status: 'success',
    error: null,
    load: async () => {},
  });
  usePortfolioStore.getState().seedFromFixture();
  usePortfolioStore.setState({ loadDashboard: async () => {}, loadTransactions: async () => {} });
}

const known = loadTickers()[0]!.symbol;

function renderRoute(path: string, element: JSX.Element, routePath: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={routePath} element={element} />
      </Routes>
    </MemoryRouter>,
  );
}

/**
 * jsdom does not perform layout, so true overflow testing cannot run here. These
 * smoke tests verify that every primary route renders without throwing across the
 * four target widths and that no element declares an explicit pixel width greater
 * than the viewport — a coarse but useful guard against accidentally hard-coding
 * a width that would break the smallest target (SC-007).
 */
function assertNoFixedWidthOverflow(container: HTMLElement, viewport: number) {
  const elements = container.querySelectorAll<HTMLElement>('[style*="width"]');
  for (const el of Array.from(elements)) {
    const match = el.style.width.match(/^(\d+)px$/);
    if (match) {
      const px = Number(match[1]);
      expect(px, `inline width ${px}px on ${el.tagName}`).toBeLessThanOrEqual(viewport);
    }
  }
}

describe('responsive viewports (SC-007)', () => {
  beforeEach(reset);
  afterEach(() => {
    reset();
    setViewport(1024);
  });

  for (const width of VIEWPORTS) {
    describe(`@${width}px`, () => {
      beforeEach(() => setViewport(width));

      it('dashboard renders without overflow', () => {
        const { container } = renderRoute('/', <DashboardRoute />, '/');
        assertNoFixedWidthOverflow(container, width);
      });

      it('watchlists index renders without overflow', () => {
        const { container } = renderRoute('/watchlists', <WatchlistsRoute />, '/watchlists');
        assertNoFixedWidthOverflow(container, width);
      });

      it('watchlist detail renders without overflow', async () => {
        const res = await useWatchlistStore.getState().create('Tech');
        if (!res.ok) throw new Error('create failed');
        await useWatchlistStore.getState().addTicker(res.id, known);
        const { container } = renderRoute(
          `/watchlists/${res.id}`,
          <WatchlistDetailRoute />,
          '/watchlists/:id',
        );
        assertNoFixedWidthOverflow(container, width);
      });

      it('transactions route renders without overflow', () => {
        const { container } = renderRoute('/transactions', <TransactionsRoute />, '/transactions');
        assertNoFixedWidthOverflow(container, width);
      });

      it('analysis route renders without overflow', () => {
        const { container } = renderRoute(
          `/analysis/${known}`,
          <AnalysisRoute />,
          '/analysis/:ticker',
        );
        assertNoFixedWidthOverflow(container, width);
      });
    });
  }
});
