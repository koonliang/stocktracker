import { apiRequest } from './client';
import type { Watchlist, WatchlistMutationRequest, WatchlistResponse } from './types';

export async function getWatchlists() {
  const response = await apiRequest<WatchlistResponse>('/watchlists');
  return response.watchlists;
}

export function createWatchlist(name: string) {
  return apiRequest<Watchlist>('/watchlists', {
    method: 'POST',
    body: JSON.stringify({ name } satisfies WatchlistMutationRequest),
  });
}

export function renameWatchlist(id: string, name: string) {
  return apiRequest<Watchlist>(`/watchlists/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ name } satisfies WatchlistMutationRequest),
  });
}

export function deleteWatchlist(id: string) {
  return apiRequest<void>(`/watchlists/${id}`, { method: 'DELETE' });
}

export function addTickerToWatchlist(id: string, ticker: string) {
  return apiRequest<Watchlist>(`/watchlists/${id}/tickers`, {
    method: 'POST',
    body: JSON.stringify({ ticker } satisfies WatchlistMutationRequest),
  });
}

export function removeTickerFromWatchlist(id: string, ticker: string) {
  return apiRequest<Watchlist>(`/watchlists/${id}/tickers/${encodeURIComponent(ticker)}`, {
    method: 'DELETE',
  });
}

export function reorderWatchlistTickers(id: string, tickers: string[]) {
  return apiRequest<Watchlist>(`/watchlists/${id}/ticker-order`, {
    method: 'PUT',
    body: JSON.stringify({ tickers } satisfies WatchlistMutationRequest),
  });
}
