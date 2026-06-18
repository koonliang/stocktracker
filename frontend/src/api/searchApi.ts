import { apiRequest } from './client';
import type { AddInstrumentResponse, InstrumentSearchResponse } from './types';

export const SUPPORTED_CURRENCIES_CHANGED_EVENT = 'stocktracker:supported-currencies-changed';

/** Search provider symbols by name or ticker (incl. global symbols like D05.SI). */
export async function searchInstruments(query: string) {
  const response = await apiRequest<InstrumentSearchResponse>(
    `/instruments/search?q=${encodeURIComponent(query)}`,
  );
  return response.results;
}

/** Add a searched symbol to the tracked universe (creates instrument + immediate quote). */
export async function addInstrument(symbol: string) {
  const response = await apiRequest<AddInstrumentResponse>('/instruments', {
    method: 'POST',
    body: JSON.stringify({ symbol }),
  });
  globalThis.window.dispatchEvent(new CustomEvent(SUPPORTED_CURRENCIES_CHANGED_EVENT));
  return response;
}
