import { useEffect, useRef, useState } from 'react';
import { Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { addInstrument, searchInstruments } from '@/api/searchApi';
import { cn } from '@/lib/cn';
import type { SymbolSearchResult } from '@/api/types';

export function TickerSearch() {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const [results, setResults] = useState<SymbolSearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [selectingSymbol, setSelectingSymbol] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);
  const navigate = useNavigate();

  useEffect(() => {
    const trimmed = query.trim();
    if (!trimmed) {
      setResults([]);
      setSearching(false);
      setError(null);
      return;
    }

    const id = ++requestId.current;
    setSearching(true);
    const handle = setTimeout(async () => {
      try {
        const matches = await searchInstruments(trimmed);
        if (id === requestId.current) {
          setResults(matches.slice(0, 8));
          setError(null);
          setActiveIndex(0);
        }
      } catch {
        if (id === requestId.current) {
          setResults([]);
          setError('Search failed. Try again.');
        }
      } finally {
        if (id === requestId.current) {
          setSearching(false);
        }
      }
    }, 300);

    return () => clearTimeout(handle);
  }, [query]);

  async function select(result: SymbolSearchResult) {
    setSelectingSymbol(result.symbol);
    setError(null);
    try {
      await addInstrument(result.symbol);
      setQuery('');
      setResults([]);
      setOpen(false);
      navigate(`/analysis/${result.symbol}`, {
        state: { backTo: '/', backLabel: 'Back to dashboard' },
      });
    } catch {
      setError(`Could not open ${result.symbol}.`);
    } finally {
      setSelectingSymbol(null);
    }
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!open || results.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex((i) => Math.min(i + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const pick = results[activeIndex];
      if (pick) void select(pick);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  }

  return (
    <div className="relative">
      <div className="relative">
        <Search
          size={14}
          aria-hidden
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-text-subtle"
        />
        <input
          type="search"
          role="combobox"
          aria-expanded={open && results.length > 0}
          aria-controls="ticker-search-listbox"
          aria-autocomplete="list"
          placeholder="Search tickers or companies…"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
            setActiveIndex(0);
            setError(null);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 120)}
          onKeyDown={onKeyDown}
          className="h-9 w-full rounded-md border border-border bg-surface pl-8 pr-3 text-small text-text placeholder:text-text-subtle focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
        />
      </div>
      {open && results.length > 0 && (
        <ul
          id="ticker-search-listbox"
          role="listbox"
          className="absolute left-0 right-0 top-[calc(100%+4px)] z-40 overflow-hidden rounded-md border border-border bg-surface shadow-popover"
        >
          {results.map((t, i) => (
            <li
              key={t.symbol}
              role="option"
              aria-selected={i === activeIndex}
              onMouseDown={(e) => {
                e.preventDefault();
                void select(t);
              }}
              onMouseEnter={() => setActiveIndex(i)}
              className={cn(
                'flex cursor-pointer items-center justify-between gap-3 px-3 py-2 text-small',
                i === activeIndex ? 'bg-surface-alt text-text' : 'text-text-muted',
              )}
            >
              <span className="font-mono font-semibold text-text">
                {selectingSymbol === t.symbol ? 'Opening…' : t.symbol}
              </span>
              <span className="truncate text-right text-text-muted">
                {t.name} · {t.exchange}
              </span>
            </li>
          ))}
        </ul>
      )}
      {open && query.trim() && !searching && results.length === 0 && !error ? (
        <div className="absolute left-0 right-0 top-[calc(100%+4px)] z-40 rounded-md border border-border bg-surface px-3 py-2 text-small text-text-muted shadow-popover">
          No matches.
        </div>
      ) : null}
      {error ? (
        <p className="absolute left-0 right-0 top-[calc(100%+4px)] z-40 rounded-md border border-border bg-surface px-3 py-2 text-small text-negative shadow-popover">
          {error}
        </p>
      ) : null}
    </div>
  );
}
