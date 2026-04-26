import { create } from 'zustand';
import { ApiError } from '@/api/client';
import {
  addTickerToWatchlist,
  createWatchlist,
  deleteWatchlist,
  getWatchlists,
  removeTickerFromWatchlist,
  renameWatchlist,
  reorderWatchlistTickers,
} from '@/api/watchlistsApi';
import type { Watchlist } from '@/lib/types';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export type AddTickerResult =
  | { ok: true }
  | { ok: false; reason: 'unknown' | 'duplicate' | 'not-found' | 'server' };

export type CreateResult =
  | { ok: true; id: string }
  | { ok: false; reason: 'empty' | 'too-long' | 'duplicate-name' | 'server' };

export type RenameResult =
  | { ok: true }
  | { ok: false; reason: 'empty' | 'too-long' | 'duplicate-name' | 'not-found' | 'server' };

type State = {
  watchlists: Watchlist[];
  status: LoadStatus;
  error: string | null;
};

type Actions = {
  load: () => Promise<void>;
  create: (name: string) => Promise<CreateResult>;
  rename: (id: string, name: string) => Promise<RenameResult>;
  remove: (id: string) => Promise<void>;
  addTicker: (id: string, symbol: string) => Promise<AddTickerResult>;
  removeTicker: (id: string, symbol: string) => Promise<void>;
  reorderTickers: (id: string, from: number, to: number) => Promise<void>;
  hydrateForTests: (watchlists: Watchlist[]) => void;
};

function errorReason(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Request failed';
}

function createFailure(error: unknown): CreateResult {
  if (error instanceof ApiError) {
    if (error.status === 409) return { ok: false, reason: 'duplicate-name' };
    if (/long/i.test(error.message)) return { ok: false, reason: 'too-long' };
    if (/required/i.test(error.message)) return { ok: false, reason: 'empty' };
  }
  return { ok: false, reason: 'server' };
}

function renameFailure(error: unknown): RenameResult {
  if (error instanceof ApiError) {
    if (error.status === 404) return { ok: false, reason: 'not-found' };
    if (error.status === 409) return { ok: false, reason: 'duplicate-name' };
    if (/long/i.test(error.message)) return { ok: false, reason: 'too-long' };
    if (/required/i.test(error.message)) return { ok: false, reason: 'empty' };
  }
  return { ok: false, reason: 'server' };
}

function addTickerFailure(error: unknown): AddTickerResult {
  if (error instanceof ApiError) {
    if (error.status === 404) return { ok: false, reason: 'not-found' };
    if (error.status === 409) return { ok: false, reason: 'duplicate' };
    if (error.status === 422) return { ok: false, reason: 'unknown' };
  }
  return { ok: false, reason: 'server' };
}

export const useWatchlistStore = create<State & Actions>()((set, get) => ({
  watchlists: [],
  status: 'idle',
  error: null,

  async load() {
    set({ status: 'loading', error: null });
    try {
      const watchlists = await getWatchlists();
      set({ watchlists, status: 'success' });
    } catch (error) {
      set({ status: 'error', error: errorReason(error) });
    }
  },

  async create(name) {
    try {
      const watchlist = await createWatchlist(name);
      set({ watchlists: [watchlist, ...get().watchlists], error: null });
      return { ok: true, id: watchlist.id };
    } catch (error) {
      set({ error: errorReason(error) });
      return createFailure(error);
    }
  },

  async rename(id, name) {
    try {
      const updated = await renameWatchlist(id, name);
      set({
        watchlists: get().watchlists.map((watchlist) => (watchlist.id === id ? updated : watchlist)),
        error: null,
      });
      return { ok: true };
    } catch (error) {
      set({ error: errorReason(error) });
      return renameFailure(error);
    }
  },

  async remove(id) {
    try {
      await deleteWatchlist(id);
      set({
        watchlists: get().watchlists.filter((watchlist) => watchlist.id !== id),
        error: null,
      });
    } catch (error) {
      set({ error: errorReason(error) });
    }
  },

  async addTicker(id, symbol) {
    try {
      const updated = await addTickerToWatchlist(id, symbol.trim().toUpperCase());
      set({
        watchlists: get().watchlists.map((watchlist) => (watchlist.id === id ? updated : watchlist)),
        error: null,
      });
      return { ok: true };
    } catch (error) {
      set({ error: errorReason(error) });
      return addTickerFailure(error);
    }
  },

  async removeTicker(id, symbol) {
    try {
      const updated = await removeTickerFromWatchlist(id, symbol);
      set({
        watchlists: get().watchlists.map((watchlist) => (watchlist.id === id ? updated : watchlist)),
        error: null,
      });
    } catch (error) {
      set({ error: errorReason(error) });
    }
  },

  async reorderTickers(id, from, to) {
    const watchlist = get().watchlists.find((entry) => entry.id === id);
    if (!watchlist) return;
    if (from < 0 || to < 0 || from >= watchlist.tickers.length || to >= watchlist.tickers.length) return;
    const tickers = [...watchlist.tickers];
    const [moved] = tickers.splice(from, 1);
    tickers.splice(to, 0, moved!);
    try {
      const updated = await reorderWatchlistTickers(id, tickers);
      set({
        watchlists: get().watchlists.map((entry) => (entry.id === id ? updated : entry)),
        error: null,
      });
    } catch (error) {
      set({ error: errorReason(error) });
    }
  },

  hydrateForTests(watchlists) {
    set({ watchlists, status: 'success', error: null });
  },
}));
