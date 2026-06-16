import { apiRequest } from './client';
import type { AddInstrumentResponse, InstrumentSearchResponse } from './types';

/** Search provider symbols by name or ticker (incl. global symbols like D05.SI). */
export async function searchInstruments(query: string) {
  const response = await apiRequest<InstrumentSearchResponse>(
    `/instruments/search?q=${encodeURIComponent(query)}`,
  );
  return response.results;
}

/** Add a searched symbol to the tracked universe (creates instrument + immediate quote). */
export function addInstrument(symbol: string) {
  return apiRequest<AddInstrumentResponse>('/instruments', {
    method: 'POST',
    body: JSON.stringify({ symbol }),
  });
}
