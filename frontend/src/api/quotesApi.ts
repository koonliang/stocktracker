import { apiRequest } from './client';
import type { QuoteResponse } from './types';

/** Latest cached quotes for the given symbols (server reads cache only). */
export async function getQuotes(symbols: string[]) {
  if (symbols.length === 0) {
    return { quotes: [] } satisfies QuoteResponse;
  }
  const query = encodeURIComponent(symbols.join(','));
  return apiRequest<QuoteResponse>(`/quotes?symbols=${query}`);
}
