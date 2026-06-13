import { create } from 'zustand';
import { ApiError } from '@/api/client';
import { getDashboard } from '@/api/dashboardApi';
import {
  commitTransactionImport,
  createTransaction,
  deleteTransaction as deleteTransactionRequest,
  getTransactions,
  previewTransactionImport,
} from '@/api/transactionsApi';
import type {
  DashboardResponse,
  Holding,
  PortfolioSummary,
  Transaction,
  TransactionImportPreviewResponse,
  TransactionImportNormalizedRow,
} from '@/lib/types';
import { computeHoldings, computePortfolio, buildPriceLookup } from '@/lib/portfolio';
import { loadPrices, loadSeedPortfolio, loadTickers } from '@/lib/seed';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

type State = {
  initialized: boolean;
  transactions: Transaction[];
  holdings: Holding[];
  summary: PortfolioSummary;
  dashboardStatus: LoadStatus;
  transactionsStatus: LoadStatus;
  previewStatus: LoadStatus;
  commitStatus: LoadStatus;
  error: string | null;
  preview: TransactionImportPreviewResponse | null;
};

type Actions = {
  loadDashboard: () => Promise<void>;
  loadTransactions: () => Promise<void>;
  refreshAll: () => Promise<void>;
  deleteTransaction: (id: string) => Promise<void>;
  previewImport: (file: File) => Promise<void>;
  clearPreview: () => void;
  commitPreview: () => Promise<void>;
  createManualTransaction: (row: TransactionImportNormalizedRow) => Promise<void>;
  clearError: () => void;
  hydrateForTests: (
    data: Partial<Pick<State, 'transactions' | 'holdings' | 'summary' | 'preview'>>,
  ) => void;
  addTransaction: (transaction: Omit<Transaction, 'id'> & { id?: string }) => void;
  removeTransaction: (id: string) => void;
  replaceAll: (transactions: Transaction[]) => void;
  appendMany: (transactions: Transaction[]) => void;
  clear: () => void;
  seedFromFixture: () => void;
};

const EMPTY_SUMMARY: PortfolioSummary = {
  totalMarketValue: 0,
  totalCostBasis: 0,
  totalUnrealizedPnL: 0,
  totalUnrealizedPnLPct: 0,
  totalDayChange: 0,
  totalDayChangePct: 0,
};

function messageFromError(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Request failed';
}

function applyDashboard(response: DashboardResponse) {
  // Guard against a malformed body (e.g. an HTML SPA-fallback page served with
  // a 200 when the API URL is misconfigured). Without this the store would set
  // holdings = undefined and the dashboard would crash on `holdings.length`.
  if (!response || !Array.isArray(response.holdings) || response.summary == null) {
    throw new Error('Unexpected dashboard response from the server.');
  }
  return {
    holdings: response.holdings,
    summary: response.summary,
    dashboardStatus: 'success' as const,
  };
}

function buildLocalDashboard(transactions: Transaction[]) {
  const tickerMap = new Map(loadTickers().map((ticker) => [ticker.symbol, ticker]));
  const holdings = computeHoldings(transactions, buildPriceLookup(loadPrices()), tickerMap);
  return {
    holdings,
    summary: computePortfolio(holdings),
  };
}

export const usePortfolioStore = create<State & Actions>()((set, get) => ({
  initialized: false,
  transactions: [],
  holdings: [],
  summary: EMPTY_SUMMARY,
  dashboardStatus: 'idle',
  transactionsStatus: 'idle',
  previewStatus: 'idle',
  commitStatus: 'idle',
  error: null,
  preview: null,

  async loadDashboard() {
    set({ dashboardStatus: 'loading', error: null });
    try {
      const response = await getDashboard();
      set(applyDashboard(response));
    } catch (error) {
      set({ dashboardStatus: 'error', error: messageFromError(error) });
    }
  },

  async loadTransactions() {
    set({ transactionsStatus: 'loading', error: null });
    try {
      const transactions = await getTransactions();
      set({ transactions, transactionsStatus: 'success' });
    } catch (error) {
      set({ transactionsStatus: 'error', error: messageFromError(error) });
    }
  },

  async refreshAll() {
    await Promise.all([get().loadDashboard(), get().loadTransactions()]);
  },

  async deleteTransaction(id) {
    set({ commitStatus: 'loading', error: null });
    try {
      const dashboard = await deleteTransactionRequest(id);
      const transactions = await getTransactions();
      set({
        ...applyDashboard(dashboard),
        transactions,
        transactionsStatus: 'success',
        commitStatus: 'success',
      });
    } catch (error) {
      set({ commitStatus: 'error', error: messageFromError(error) });
    }
  },

  async previewImport(file) {
    set({ previewStatus: 'loading', error: null, preview: null });
    try {
      const preview = await previewTransactionImport(file);
      set({ preview, previewStatus: 'success' });
    } catch (error) {
      set({ previewStatus: 'error', error: messageFromError(error) });
    }
  },

  clearPreview() {
    set({ preview: null, previewStatus: 'idle' });
  },

  async commitPreview() {
    const preview = get().preview;
    if (!preview || preview.validRows.length === 0) return;

    set({ commitStatus: 'loading', error: null });
    try {
      const dashboard = await commitTransactionImport(
        preview.validRows.map((row) => row.normalized),
      );
      const transactions = await getTransactions();
      set({
        ...applyDashboard(dashboard),
        transactions,
        transactionsStatus: 'success',
        preview: null,
        previewStatus: 'idle',
        commitStatus: 'success',
      });
    } catch (error) {
      set({ commitStatus: 'error', error: messageFromError(error) });
    }
  },

  async createManualTransaction(row) {
    set({ commitStatus: 'loading', error: null });
    try {
      const dashboard = await createTransaction(row);
      const transactions = await getTransactions();
      set({
        ...applyDashboard(dashboard),
        transactions,
        transactionsStatus: 'success',
        commitStatus: 'success',
      });
    } catch (error) {
      set({ commitStatus: 'error', error: messageFromError(error) });
    }
  },

  clearError() {
    set({ error: null });
  },

  hydrateForTests(data) {
    set({
      initialized: true,
      transactions: data.transactions ?? [],
      holdings: data.holdings ?? [],
      summary: data.summary ?? EMPTY_SUMMARY,
      preview: data.preview ?? null,
      dashboardStatus: 'success',
      transactionsStatus: 'success',
      previewStatus: data.preview ? 'success' : 'idle',
      commitStatus: 'idle',
      error: null,
    });
  },

  addTransaction(transaction) {
    const next = [
      ...get().transactions,
      { ...transaction, id: transaction.id ?? crypto.randomUUID() } as Transaction,
    ];
    const dashboard = buildLocalDashboard(next);
    set({ initialized: true, transactions: next, ...dashboard });
  },

  removeTransaction(id) {
    const next = get().transactions.filter((transaction) => transaction.id !== id);
    const dashboard = buildLocalDashboard(next);
    set({ initialized: true, transactions: next, ...dashboard });
  },

  replaceAll(transactions) {
    const dashboard = buildLocalDashboard(transactions);
    set({ initialized: true, transactions, ...dashboard });
  },

  appendMany(transactions) {
    const next = [...get().transactions, ...transactions];
    const dashboard = buildLocalDashboard(next);
    set({ initialized: true, transactions: next, ...dashboard });
  },

  clear() {
    set({
      initialized: true,
      transactions: [],
      holdings: [],
      summary: EMPTY_SUMMARY,
    });
  },

  seedFromFixture() {
    const transactions = loadSeedPortfolio();
    const dashboard = buildLocalDashboard(transactions);
    set({ initialized: true, transactions, ...dashboard });
  },
}));
