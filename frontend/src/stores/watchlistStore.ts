import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Watchlist } from '@/lib/types';
import { isKnownTicker } from '@/lib/seed';

export type AddTickerResult =
  | { ok: true }
  | { ok: false; reason: 'unknown' | 'duplicate' | 'not-found' };

export type CreateResult =
  | { ok: true; id: string }
  | { ok: false; reason: 'empty' | 'too-long' | 'duplicate-name' };

export type RenameResult =
  | { ok: true }
  | { ok: false; reason: 'empty' | 'too-long' | 'duplicate-name' | 'not-found' };

type State = {
  watchlists: Watchlist[];
};

type Actions = {
  create: (name: string) => CreateResult;
  rename: (id: string, name: string) => RenameResult;
  remove: (id: string) => void;
  addTicker: (id: string, symbol: string) => AddTickerResult;
  removeTicker: (id: string, symbol: string) => void;
  reorderTickers: (id: string, from: number, to: number) => void;
};

const MAX_NAME_LENGTH = 40;

function newId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `wl_${Math.random().toString(36).slice(2, 10)}_${Date.now().toString(36)}`;
}

function nowISO(): string {
  return new Date().toISOString();
}

function normalizeName(raw: string): string {
  return raw.trim();
}

function nameExists(lists: Watchlist[], name: string, excludeId?: string): boolean {
  const lower = name.toLowerCase();
  return lists.some((w) => w.id !== excludeId && w.name.toLowerCase() === lower);
}

export const useWatchlistStore = create<State & Actions>()(
  persist(
    (set, get) => ({
      watchlists: [],

      create: (rawName) => {
        const name = normalizeName(rawName);
        if (!name) return { ok: false, reason: 'empty' };
        if (name.length > MAX_NAME_LENGTH) return { ok: false, reason: 'too-long' };
        if (nameExists(get().watchlists, name)) return { ok: false, reason: 'duplicate-name' };

        const id = newId();
        const now = nowISO();
        set((s) => ({
          watchlists: [...s.watchlists, { id, name, tickers: [], createdAt: now, updatedAt: now }],
        }));
        return { ok: true, id };
      },

      rename: (id, rawName) => {
        const name = normalizeName(rawName);
        if (!name) return { ok: false, reason: 'empty' };
        if (name.length > MAX_NAME_LENGTH) return { ok: false, reason: 'too-long' };
        const lists = get().watchlists;
        if (!lists.some((w) => w.id === id)) return { ok: false, reason: 'not-found' };
        if (nameExists(lists, name, id)) return { ok: false, reason: 'duplicate-name' };

        set((s) => ({
          watchlists: s.watchlists.map((w) =>
            w.id === id ? { ...w, name, updatedAt: nowISO() } : w,
          ),
        }));
        return { ok: true };
      },

      remove: (id) => set((s) => ({ watchlists: s.watchlists.filter((w) => w.id !== id) })),

      addTicker: (id, rawSymbol) => {
        const symbol = rawSymbol.trim().toUpperCase();
        if (!isKnownTicker(symbol)) return { ok: false, reason: 'unknown' };
        const list = get().watchlists.find((w) => w.id === id);
        if (!list) return { ok: false, reason: 'not-found' };
        if (list.tickers.includes(symbol)) return { ok: false, reason: 'duplicate' };

        set((s) => ({
          watchlists: s.watchlists.map((w) =>
            w.id === id ? { ...w, tickers: [...w.tickers, symbol], updatedAt: nowISO() } : w,
          ),
        }));
        return { ok: true };
      },

      removeTicker: (id, rawSymbol) => {
        const symbol = rawSymbol.toUpperCase();
        set((s) => ({
          watchlists: s.watchlists.map((w) =>
            w.id === id
              ? { ...w, tickers: w.tickers.filter((t) => t !== symbol), updatedAt: nowISO() }
              : w,
          ),
        }));
      },

      reorderTickers: (id, from, to) => {
        set((s) => ({
          watchlists: s.watchlists.map((w) => {
            if (w.id !== id) return w;
            if (from < 0 || from >= w.tickers.length) return w;
            if (to < 0 || to >= w.tickers.length) return w;
            if (from === to) return w;
            const next = [...w.tickers];
            const [moved] = next.splice(from, 1);
            next.splice(to, 0, moved!);
            return { ...w, tickers: next, updatedAt: nowISO() };
          }),
        }));
      },
    }),
    { name: 'stocktracker.watchlists' },
  ),
);
