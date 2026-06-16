import { useMemo, useRef, useState } from 'react';
import { Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { loadTickers } from '@/lib/seed';
import { cn } from '@/lib/cn';

export function TickerSearch() {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const tickers = useMemo(() => loadTickers(), []);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  const results = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.trim().toLowerCase();
    return tickers
      .filter((t) => t.symbol.toLowerCase().includes(q) || t.name.toLowerCase().includes(q))
      .slice(0, 8);
  }, [query, tickers]);

  function select(symbol: string) {
    setQuery('');
    setOpen(false);
    navigate(`/analysis/${symbol}`, {
      state: { backTo: '/', backLabel: 'Back to dashboard' },
    });
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
      if (pick) select(pick.symbol);
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
          ref={inputRef}
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
                select(t.symbol);
              }}
              onMouseEnter={() => setActiveIndex(i)}
              className={cn(
                'flex cursor-pointer items-center justify-between gap-3 px-3 py-2 text-small',
                i === activeIndex ? 'bg-surface-alt text-text' : 'text-text-muted',
              )}
            >
              <span className="font-mono font-semibold text-text">{t.symbol}</span>
              <span className="truncate text-right text-text-muted">{t.name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
