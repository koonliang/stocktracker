import { useEffect, useRef, useState } from 'react';
import { ApiError } from '@/api/client';
import { addInstrument, searchInstruments } from '@/api/searchApi';
import type { SymbolSearchResult } from '@/lib/types';

type Props = {
  /** Called after a symbol is successfully added, with its symbol. */
  onAdded?: (symbol: string) => void;
};

/**
 * Debounced symbol search + add-on-demand (FR-026/027). Surfaces global symbols (e.g. D05.SI);
 * adding one creates the instrument server-side and returns an immediate quote.
 */
export function SymbolSearch({ onAdded }: Props) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SymbolSearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [adding, setAdding] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);

  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length === 0) {
      setResults([]);
      setError(null);
      return;
    }
    const id = ++requestId.current;
    setSearching(true);
    const handle = setTimeout(async () => {
      try {
        const matches = await searchInstruments(trimmed);
        if (id === requestId.current) {
          setResults(matches);
          setError(null);
        }
      } catch {
        if (id === requestId.current) setError('Search failed. Try again.');
      } finally {
        if (id === requestId.current) setSearching(false);
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [query]);

  async function handleAdd(symbol: string) {
    setAdding(symbol);
    setError(null);
    try {
      await addInstrument(symbol);
      setQuery('');
      setResults([]);
      onAdded?.(symbol);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 422
          ? `"${symbol}" is not a recognized symbol.`
          : 'Could not add the symbol. Try again.',
      );
    } finally {
      setAdding(null);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search a symbol or company (e.g. DBS, D05.SI)"
        aria-label="Search symbols"
        data-testid="symbol-search"
        className="h-9 w-full rounded-md border border-border bg-surface px-3 text-body text-text placeholder:text-text-muted focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
      />

      {error ? (
        <p data-testid="symbol-search-error" className="text-small text-negative">
          {error}
        </p>
      ) : null}

      {query.trim() && !searching && results.length === 0 && !error ? (
        <p className="text-small text-text-muted">No matches.</p>
      ) : null}

      {results.length > 0 ? (
        <ul className="flex flex-col gap-1">
          {results.map((result) => (
            <li
              key={result.symbol}
              data-testid="symbol-search-result"
              className="flex items-center justify-between gap-3 rounded-md border border-border bg-surface px-3 py-2"
            >
              <div className="flex min-w-0 flex-col">
                <span className="font-mono text-body font-semibold text-text">{result.symbol}</span>
                <span className="truncate text-small text-text-muted">
                  {result.name} · {result.exchange} · {result.currency}
                </span>
              </div>
              <button
                type="button"
                onClick={() => void handleAdd(result.symbol)}
                disabled={adding === result.symbol}
                data-testid="symbol-add"
                aria-label={`Add ${result.symbol}`}
                className="shrink-0 rounded-md border border-border bg-surface px-3 py-1 text-small font-medium text-text transition-colors hover:border-border-strong disabled:opacity-50"
              >
                {adding === result.symbol ? 'Adding…' : 'Add'}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
