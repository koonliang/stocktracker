import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
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
        },
      ],
      returnSeries: [
        { date: '2025-06-14', cumulativeReturnPct: 0 },
        { date: '2026-06-14', cumulativeReturnPct: 13.78 },
      ],
      contributions: [],
    });
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
    expect(screen.getByText('Dividend')).toBeInTheDocument();
  });
});
