import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { useWatchlistStore } from '@/stores/watchlistStore';
import { loadTickers } from '@/lib/seed';

function reset() {
  localStorage.clear();
  useWatchlistStore.setState({ watchlists: [], status: 'idle', error: null });
}

const knownTickers = loadTickers().map((ticker) => ticker.symbol);
const known1 = knownTickers[0]!;
const known2 = knownTickers[1]!;
const known3 = knownTickers[2]!;

describe('watchlistStore', () => {
  beforeEach(reset);
  afterEach(reset);

  it('create adds a new watchlist and returns its id', async () => {
    const res = await useWatchlistStore.getState().create('Tech Majors');
    expect(res.ok).toBe(true);
    const lists = useWatchlistStore.getState().watchlists;
    expect(lists).toHaveLength(1);
    expect(lists[0]!.name).toBe('Tech Majors');
    expect(lists[0]!.tickers).toEqual([]);
  });

  it('create rejects empty names', async () => {
    expect(await useWatchlistStore.getState().create('')).toEqual({ ok: false, reason: 'empty' });
    expect(await useWatchlistStore.getState().create('   ')).toEqual({
      ok: false,
      reason: 'empty',
    });
  });

  it('create rejects duplicate names', async () => {
    await useWatchlistStore.getState().create('Tech Majors');
    expect(await useWatchlistStore.getState().create('tech majors')).toEqual({
      ok: false,
      reason: 'duplicate-name',
    });
  });

  it('rename updates the name', async () => {
    const created = await useWatchlistStore.getState().create('Old');
    if (!created.ok) throw new Error('create failed');
    const originalUpdatedAt = useWatchlistStore.getState().watchlists[0]!.updatedAt;
    await new Promise((resolve) => setTimeout(resolve, 5));
    const res = await useWatchlistStore.getState().rename(created.id, 'New Name');
    expect(res.ok).toBe(true);
    const after = useWatchlistStore.getState().watchlists[0]!;
    expect(after.name).toBe('New Name');
    expect(after.updatedAt).not.toBe(originalUpdatedAt);
  });

  it('remove deletes a watchlist', async () => {
    const res = await useWatchlistStore.getState().create('List');
    if (!res.ok) throw new Error('create failed');
    await useWatchlistStore.getState().remove(res.id);
    expect(useWatchlistStore.getState().watchlists).toHaveLength(0);
  });

  it('addTicker rejects unknown tickers', async () => {
    const created = await useWatchlistStore.getState().create('L');
    if (!created.ok) throw new Error('create failed');
    expect(await useWatchlistStore.getState().addTicker(created.id, 'ZZZZZ')).toEqual({
      ok: false,
      reason: 'unknown',
    });
  });

  it('addTicker rejects duplicates and normalizes case', async () => {
    const created = await useWatchlistStore.getState().create('L');
    if (!created.ok) throw new Error('create failed');
    expect((await useWatchlistStore.getState().addTicker(created.id, known1)).ok).toBe(true);
    expect(await useWatchlistStore.getState().addTicker(created.id, known1.toLowerCase())).toEqual({
      ok: false,
      reason: 'duplicate',
    });
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known1]);
  });

  it('removeTicker removes by symbol', async () => {
    const created = await useWatchlistStore.getState().create('L');
    if (!created.ok) throw new Error('create failed');
    await useWatchlistStore.getState().addTicker(created.id, known1);
    await useWatchlistStore.getState().addTicker(created.id, known2);
    await useWatchlistStore.getState().removeTicker(created.id, known1);
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2]);
  });

  it('reorderTickers moves a ticker from one index to another', async () => {
    const created = await useWatchlistStore.getState().create('L');
    if (!created.ok) throw new Error('create failed');
    await useWatchlistStore.getState().addTicker(created.id, known1);
    await useWatchlistStore.getState().addTicker(created.id, known2);
    await useWatchlistStore.getState().addTicker(created.id, known3);
    await useWatchlistStore.getState().reorderTickers(created.id, 0, 2);
    expect(useWatchlistStore.getState().watchlists[0]!.tickers).toEqual([known2, known3, known1]);
  });
});
