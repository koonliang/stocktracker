import { create } from 'zustand';
import { getQuotes } from '@/api/quotesApi';
import type { Quote } from '@/lib/types';

const POLL_INTERVAL_MS = 30_000;

type State = {
  quotes: Record<string, Quote>;
  symbols: string[];
  /** Most recent successful fetch instant across tracked symbols (ISO), for "last updated". */
  lastUpdated: string | null;
};

type Actions = {
  setSymbols: (symbols: string[]) => void;
  pollOnce: () => Promise<void>;
  startPolling: (onTick?: () => void) => void;
  stopPolling: () => void;
  reset: () => void;
};

// Timer + listener live outside the store so they are not part of reactive state.
let timer: ReturnType<typeof setInterval> | null = null;
let visibilityListener: (() => void) | null = null;
let tickCallback: (() => void) | undefined;

function isVisible(): boolean {
  return typeof document === 'undefined' || document.visibilityState === 'visible';
}

export const useQuotesStore = create<State & Actions>()((set, get) => ({
  quotes: {},
  symbols: [],
  lastUpdated: null,

  setSymbols(symbols) {
    const unique = Array.from(new Set(symbols.map((s) => s.toUpperCase()))).sort();
    if (unique.join(',') !== get().symbols.join(',')) {
      set({ symbols: unique });
    }
  },

  async pollOnce() {
    const symbols = get().symbols;
    if (symbols.length === 0) return;
    try {
      const { quotes } = await getQuotes(symbols);
      const bySymbol: Record<string, Quote> = {};
      let latest: string | null = get().lastUpdated;
      for (const quote of quotes) {
        bySymbol[quote.symbol.toUpperCase()] = quote;
        if (quote.fetchedAt && (!latest || quote.fetchedAt > latest)) {
          latest = quote.fetchedAt;
        }
      }
      set({ quotes: { ...get().quotes, ...bySymbol }, lastUpdated: latest });
    } catch {
      // A failed poll keeps the last values; the next tick retries.
    }
  },

  startPolling(onTick) {
    get().stopPolling();
    tickCallback = onTick;
    const tick = () => {
      if (!isVisible()) return;
      void get()
        .pollOnce()
        .then(() => tickCallback?.());
    };
    timer = setInterval(tick, POLL_INTERVAL_MS);
    if (typeof document !== 'undefined') {
      visibilityListener = () => {
        if (isVisible()) tick();
      };
      document.addEventListener('visibilitychange', visibilityListener);
    }
  },

  stopPolling() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
    if (visibilityListener && typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', visibilityListener);
      visibilityListener = null;
    }
    tickCallback = undefined;
  },

  reset() {
    get().stopPolling();
    set({ quotes: {}, symbols: [], lastUpdated: null });
  },
}));

/** True when any tracked quote is stale (provider not responding). */
export function selectAnyStale(state: State): boolean {
  return Object.values(state.quotes).some((quote) => quote.stale);
}
