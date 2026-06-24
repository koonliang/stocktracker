import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import { PerformanceRoute } from '@/routes/PerformanceRoute';

const getPerformanceMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/performanceApi', () => ({
  getPerformance: getPerformanceMock,
}));

describe('PerformanceRoute', () => {
  beforeEach(() => {
    getPerformanceMock.mockReset();
    getPerformanceMock.mockResolvedValue({
      window: '1Y',
      method: 'fifo',
      baseCurrency: 'USD',
      realizedPnL: 388.65,
      unrealizedPnL: 27084.44,
      timeWeightedReturnPct: 13.78,
      closedLots: [
        {
          symbol: 'GOOGL',
          currency: 'USD',
          openDate: '2024-01-01',
          closeDate: '2025-03-10',
          quantity: 10,
          costBasisNative: 1253.88,
          proceedsNative: 1442.53,
          realizedPnLNative: 188.65,
          realizedPnLBase: 188.65,
          realizedPnlConversion: {
            baseCurrency: 'USD',
            amountBase: 188.65,
            fxDate: '2025-03-10',
            fxStatus: 'current',
          },
        },
      ],
      incomeEvents: [
        {
          symbol: 'GOOGL',
          currency: 'USD',
          date: '2026-06-03',
          type: 'dividend',
          amountNative: 200,
          amountBase: 200,
          amountConversion: {
            baseCurrency: 'USD',
            amountBase: 200,
            fxDate: '2026-06-03',
            fxStatus: 'stale',
          },
        },
      ],
      returnSeries: [
        { date: '2025-06-14', cumulativeReturnPct: 0 },
        { date: '2026-06-14', cumulativeReturnPct: 13.78 },
      ],
      contributions: [
        {
          symbol: 'AAPL',
          contributionPct: 12.4,
          contributionBase: 27084.44,
          contributionConversion: {
            baseCurrency: 'USD',
            amountBase: 27084.44,
            fxDate: null,
            fxStatus: 'unavailable',
          },
        },
      ],
    });
  });

  it('renders the Returns page heading', async () => {
    render(<PerformanceRoute />);
    expect(await screen.findByRole('heading', { name: /Returns/i, level: 1 })).toBeInTheDocument();
  });

  it('renders the empty state when no positions exist', async () => {
    getPerformanceMock.mockResolvedValue({
      window: '1Y',
      method: 'fifo',
      baseCurrency: 'USD',
      realizedPnL: 0,
      unrealizedPnL: 0,
      timeWeightedReturnPct: 0,
      closedLots: [],
      incomeEvents: [],
      returnSeries: [],
      contributions: [],
    });
    render(<PerformanceRoute />);
    expect(await screen.findByText(/No performance data yet/i)).toBeInTheDocument();
  });

  it('reconciles realized P&L across closed lots and income events', async () => {
    render(<PerformanceRoute />);

    expect(await screen.findByText('Realized P&L details')).toBeInTheDocument();

    expect(screen.getByText('Closed lot gains')).toBeInTheDocument();
    expect(screen.getByText('Dividend income')).toBeInTheDocument();
    expect(screen.getByText('Income events')).toBeInTheDocument();
    expect(screen.getAllByText('+$188.65').length).toBeGreaterThan(0);
    expect(screen.getAllByText('+$200.00').length).toBeGreaterThan(0);
    expect(screen.getAllByText('+$388.65').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Dividend')[0]).toBeInTheDocument();
    expect(screen.getAllByText('Stale rate').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Rate unavailable').length).toBeGreaterThan(0);
  });

  it('hides the FIFO/LIFO toggle on mobile (hidden sm:flex class)', async () => {
    render(<PerformanceRoute />);
    await screen.findByRole('heading', { name: /Returns/i, level: 1 });
    const toggle = screen.getByTestId('lot-method-toggle');
    expect(toggle).toHaveClass('hidden');
    expect(toggle).toHaveClass('sm:flex');
  });

  it('keeps the window selector visible', async () => {
    render(<PerformanceRoute />);
    expect(await screen.findByTestId('perf-window-select')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '1Y' })).toBeInTheDocument();
  });

  it('refetches when base currency changes', async () => {
    render(<PerformanceRoute />);

    expect(await screen.findByText('Realized P&L details')).toBeInTheDocument();
    act(() => {
      window.dispatchEvent(new CustomEvent('stocktracker:base-currency-changed'));
    });

    await waitFor(() => expect(getPerformanceMock).toHaveBeenCalledTimes(2));
  });
});
