import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Transaction } from '@/lib/types';
import { loadSeedPortfolio } from '@/lib/seed';

type State = {
  transactions: Transaction[];
  /** Whether the store has been initialized (used to seed on first run only). */
  initialized: boolean;
};

type Actions = {
  addTransaction: (tx: Omit<Transaction, 'id'> & { id?: string }) => void;
  removeTransaction: (id: string) => void;
  replaceAll: (transactions: Transaction[]) => void;
  appendMany: (transactions: Transaction[]) => void;
  clear: () => void;
  /** Force-seed from bundled demo data (used after clear in tests). */
  seedFromFixture: () => void;
};

function newId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `tx_${Math.random().toString(36).slice(2, 10)}_${Date.now().toString(36)}`;
}

export const usePortfolioStore = create<State & Actions>()(
  persist(
    (set) => ({
      transactions: [],
      initialized: false,
      addTransaction: (tx) =>
        set((s) => ({
          transactions: [...s.transactions, { id: tx.id ?? newId(), ...tx }],
        })),
      removeTransaction: (id) =>
        set((s) => ({ transactions: s.transactions.filter((t) => t.id !== id) })),
      replaceAll: (transactions) => set({ transactions, initialized: true }),
      appendMany: (txs) =>
        set((s) => ({ transactions: [...s.transactions, ...txs], initialized: true })),
      clear: () => set({ transactions: [], initialized: true }),
      seedFromFixture: () => set({ transactions: loadSeedPortfolio(), initialized: true }),
    }),
    {
      name: 'stocktracker.portfolio',
      onRehydrateStorage: () => (state) => {
        // First-run seed: if persisted store is empty/uninitialized, load demo data
        if (state && !state.initialized && state.transactions.length === 0) {
          state.transactions = loadSeedPortfolio();
          state.initialized = true;
        }
      },
    },
  ),
);
