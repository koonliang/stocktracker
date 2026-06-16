import { describe, it, expect, beforeEach } from 'vitest';
import { useQuotesStore, selectAnyStale } from './quotesStore';

describe('quotesStore', () => {
  beforeEach(() => useQuotesStore.getState().reset());

  it('polls and stores quotes with a last-updated time', async () => {
    useQuotesStore.getState().setSymbols(['NVDA']);
    await useQuotesStore.getState().pollOnce();

    const state = useQuotesStore.getState();
    expect(state.quotes.NVDA).toBeDefined();
    expect(state.quotes.NVDA!.price).toBeGreaterThan(0);
    expect(state.lastUpdated).not.toBeNull();
    expect(selectAnyStale(state)).toBe(false);
  });

  it('marks unknown symbols stale with no price', async () => {
    useQuotesStore.getState().setSymbols(['ZZZZ']);
    await useQuotesStore.getState().pollOnce();

    const state = useQuotesStore.getState();
    expect(state.quotes.ZZZZ!.price).toBeNull();
    expect(selectAnyStale(state)).toBe(true);
  });

  it('does nothing when no symbols are tracked', async () => {
    await useQuotesStore.getState().pollOnce();
    expect(Object.keys(useQuotesStore.getState().quotes)).toHaveLength(0);
  });
});
