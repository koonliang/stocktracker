import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { useToastStore } from '@/stores/toastStore';
import { getDashboard } from '@/api/dashboardApi';

vi.mock('@/api/dashboardApi', () => ({
  getDashboard: vi.fn(),
}));

const getDashboardMock = vi.mocked(getDashboard);

function reset() {
  localStorage.clear();
  usePortfolioStore.setState({ transactions: [], initialized: true });
  useToastStore.getState().clearToasts();
}

describe('portfolioStore', () => {
  beforeEach(reset);
  afterEach(reset);

  describe('loadDashboard', () => {
    afterEach(() => getDashboardMock.mockReset());

    it('sets success state for a well-formed response', async () => {
      const summary = {
        totalMarketValue: 100,
        totalCostBasis: 90,
        totalUnrealizedPnL: 10,
        totalUnrealizedPnLPct: 0.11,
        totalDayChange: 1,
        totalDayChangePct: 0.01,
      };
      getDashboardMock.mockResolvedValue({ holdings: [], summary });
      await usePortfolioStore.getState().loadDashboard();
      const state = usePortfolioStore.getState();
      expect(state.dashboardStatus).toBe('success');
      expect(state.summary).toEqual(summary);
    });

    it('sets error state (not a crash) for a malformed response', async () => {
      // e.g. an HTML SPA-fallback page returned with a 200 when the API URL
      // is misconfigured — holdings is not an array.
      getDashboardMock.mockResolvedValue('<!doctype html>' as never);
      await usePortfolioStore.getState().loadDashboard();
      const state = usePortfolioStore.getState();
      expect(state.dashboardStatus).toBe('error');
      expect(state.error).toBeTruthy();
    });
  });

  it('addTransaction appends with a generated id', () => {
    const store = usePortfolioStore.getState();
    store.addTransaction({
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    const transactions = usePortfolioStore.getState().transactions;
    expect(transactions).toHaveLength(1);
    expect(transactions[0]!.id).toBeTruthy();
    expect(transactions[0]!.ticker).toBe('AAPL');
  });

  it('removeTransaction removes by id', () => {
    const store = usePortfolioStore.getState();
    store.addTransaction({
      id: 'a',
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    store.addTransaction({
      id: 'b',
      date: '2024-02-01',
      ticker: 'MSFT',
      type: 'buy',
      quantity: 5,
      price: 300,
      fees: 0,
    });
    usePortfolioStore.getState().removeTransaction('a');
    expect(usePortfolioStore.getState().transactions.map((transaction) => transaction.id)).toEqual([
      'b',
    ]);
  });

  it('replaceAll replaces the transaction list', () => {
    usePortfolioStore.getState().replaceAll([
      {
        id: 'x',
        date: '2024-03-01',
        ticker: 'NVDA',
        type: 'buy',
        quantity: 1,
        price: 500,
        fees: 0,
      },
    ]);
    expect(usePortfolioStore.getState().transactions).toHaveLength(1);
    expect(usePortfolioStore.getState().transactions[0]!.ticker).toBe('NVDA');
  });

  it('clear empties the list', () => {
    usePortfolioStore.getState().addTransaction({
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    usePortfolioStore.getState().clear();
    expect(usePortfolioStore.getState().transactions).toEqual([]);
  });

  it('seedFromFixture populates from bundled demo data', () => {
    usePortfolioStore.getState().seedFromFixture();
    expect(usePortfolioStore.getState().transactions.length).toBeGreaterThan(0);
  });

  it('pushes a toast when manual transaction creation fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'validation_error',
            message: 'sell quantity exceeds held shares',
            details: null,
          }),
          { status: 422, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    await usePortfolioStore.getState().createManualTransaction({
      date: '2026-06-14',
      ticker: 'V',
      type: 'sell',
      quantity: 30,
      price: 348,
      fees: 2,
      amount: null,
      currency: 'USD',
    });

    expect(usePortfolioStore.getState().commitStatus).toBe('error');
    expect(useToastStore.getState().toasts).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          title: 'Transaction not saved',
          message: 'sell quantity exceeds held shares',
          tone: 'error',
        }),
      ]),
    );
  });

  it('keeps added transactions in memory for local test helpers', () => {
    usePortfolioStore.getState().addTransaction({
      date: '2024-01-01',
      ticker: 'AAPL',
      type: 'buy',
      quantity: 10,
      price: 100,
      fees: 0,
    });
    expect(usePortfolioStore.getState().transactions[0]!.ticker).toBe('AAPL');
  });
});
