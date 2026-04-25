import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { loadTickers } from '@/lib/seed';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [] });
}

const knownTickers = loadTickers().map((t) => t.symbol);
const known1 = knownTickers[0]!;
const known2 = knownTickers[1]!;
const known3 = knownTickers[2]!;

describe('watchlistStore', () => {
  beforeEach(reset);
  afterEach(reset);

  it('create adds a new watchlist and returns its id', () => {
    const res = useWatchlistStore.getState().create('Tech Majors');
    expect(res.ok).toBe(true);
    const lists = useWatchlistStore.getState().watchlists;
    expect(lists).toHaveLength(1);
    expect(lists[0]!.name).toBe('Tech Majors');
    expect(lists[0]!.tickers).toEqual([]);
  });

  it('create rejects empty and whitespace-only names', () => {
    expect(useWatchlistStore.getState().create('')).toEqual({ ok: false, reason: 'empty' });
    expect(useWatchlistStore.getState().create('   ')).toEqual({ ok: false, reason: 'empty' });
  });

  it('create rejects duplicate names (case-insensitive)', () => {
    useWatchlistStore.getState().create('Tech Majors');
    const res = useWatchlistStore.getState().create('tech majors');
    expect(res).toEqual({ ok: false, reason: 'duplicate-name' });
    expect(useWatchlistStore.getState().watchlists).toHaveLength(1);
  });

  it('rename updates the name and bumps updatedAt', async () => {
    const created = useWatchlistStore.getState().create('Old');
    expect(created.ok).toBe(true);
    const id = (created as { ok: true; id: string }).id;
    const originalUpdatedAt = useWatchlistStore.getState().watchlists[0]!.updatedAt;
    // wait a tick so updatedAt differs
    await new Promise((r) => setTimeout(r, 5));
    const res = useWatchlistStore.getState().rename(id, 'New Name');
    expect(res.ok).toBe(true);
    const after = useWatchlistStore.getState().watchlists[0]!;
    expect(after.name).toBe('New Name');
    expect(after.updatedAt).not.toBe(originalUpdatedAt);
  });

  it('rename rejects duplicate names but allows renaming to the same name case-insensitively on self', () => {
    const a = useWatchlistStore.getState().create('Alpha');
    useWatchlistStore.getState().create('Beta');
    const id = (a as { ok: true; id: string }).id;

    expect(useWatchlistStore.getState().rename(id, 'Beta')).toEqual({
      ok: false,
      reason: 'duplicate-name',
    });
    // renaming list to its own name (different case) should succeed — no conflict
    expect(useWatchlistStore.getState().rename(id, 'alpha').ok).toBe(true);
  });

  it('remove deletes a watchlist', () => {
    const res = useWatchlistStore.getState().create('List');
    const id = (res as { ok: true; id: string }).id;
    useWatchlistStore.getState().remove(id);
    expect(useWatchlistStore.getState().watchlists).toHaveLength(0);
  });

  it('addTicker rejects unknown tickers', () => {
    const id = (useWatchlistStore.getState().create('L') as { ok: true; id: string }).id;
    expect(useWatchlistStore.getState().addTicker(id, 'ZZZZZ')).toEqual({
      ok: false,
      reason: 'unknown',
    });
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([]);
  });

  it('addTicker rejects duplicates and normalizes case', () => {
    const id = (useWatchlistStore.getState().create('L') as { ok: true; id: string }).id;
    expect(useWatchlistStore.getState().addTicker(id, known1).ok).toBe(true);
    expect(useWatchlistStore.getState().addTicker(id, known1.toLowerCase())).toEqual({
      ok: false,
      reason: 'duplicate',
    });
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known1]);
  });

  it('removeTicker removes by symbol', () => {
    const id = (useWatchlistStore.getState().create('L') as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known1);
    useWatchlistStore.getState().addTicker(id, known2);
    useWatchlistStore.getState().removeTicker(id, known1);
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2]);
  });

  it('reorderTickers moves a ticker from one index to another', () => {
    const id = (useWatchlistStore.getState().create('L') as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known1);
    useWatchlistStore.getState().addTicker(id, known2);
    useWatchlistStore.getState().addTicker(id, known3);
    useWatchlistStore.getState().reorderTickers(id, 0, 2);
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2, known3, known1]);
  });

  it('reorderTickers is a no-op for out-of-range indices', () => {
    const id = (useWatchlistStore.getState().create('L') as { ok: true; id: string }).id;
    useWatchlistStore.getState().addTicker(id, known1);
    useWatchlistStore.getState().addTicker(id, known2);
    useWatchlistStore.getState().reorderTickers(id, 5, 0);
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known1, known2]);
  });

  it('persists to localStorage under the configured key', () => {
    useWatchlistStore.getState().create('Persisted');
    const stored = localStorage.getItem('stocktracker.watchlists');
    expect(stored).toBeTruthy();
    expect(stored).toContain('Persisted');
  });
});
