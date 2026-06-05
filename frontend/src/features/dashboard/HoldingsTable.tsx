import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import type { Holding } from '@/lib/types';
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/Table';
import {
  formatCurrency,
  formatPercent,
  formatShares,
  formatSignedCurrency,
  formatSignedPercent,
} from '@/lib/format';
import { cn } from '@/lib/cn';

type SortKey =
  | 'ticker'
  | 'shares'
  | 'averageCost'
  | 'currentPrice'
  | 'marketValue'
  | 'weight'
  | 'unrealizedPnL'
  | 'dayChange';

type Direction = 'asc' | 'desc';

type Column = {
  key: SortKey;
  label: string;
  align: 'left' | 'right';
  sortable: boolean;
  mono?: boolean;
  /** Tailwind class fragment that hides this column below a breakpoint. */
  hideClass?: string;
  render: (h: Holding) => React.ReactNode;
};

const columns: Column[] = [
  {
    key: 'ticker',
    label: 'Ticker',
    align: 'left',
    sortable: true,
    render: (h) => (
      <div className="flex min-w-0 flex-col gap-0.5">
        <span className="font-mono text-title font-semibold leading-none text-text">
          {h.ticker}
        </span>
        <span className="truncate text-small text-text-muted">{h.name}</span>
      </div>
    ),
  },
  {
    key: 'shares',
    label: 'Shares',
    align: 'right',
    sortable: true,
    mono: true,
    hideClass: 'hidden sm:table-cell',
    render: (h) => formatShares(h.shares),
  },
  {
    key: 'averageCost',
    label: 'Avg Cost',
    align: 'right',
    sortable: true,
    mono: true,
    hideClass: 'hidden lg:table-cell',
    render: (h) => formatCurrency(h.averageCost),
  },
  {
    key: 'currentPrice',
    label: 'Price',
    align: 'right',
    sortable: true,
    mono: true,
    hideClass: 'hidden md:table-cell',
    render: (h) => formatCurrency(h.currentPrice),
  },
  {
    key: 'marketValue',
    label: 'Market Value',
    align: 'right',
    sortable: true,
    mono: true,
    render: (h) => formatCurrency(h.marketValue, { cents: false }),
  },
  {
    key: 'weight',
    label: 'Weight',
    align: 'right',
    sortable: true,
    mono: true,
    hideClass: 'hidden xl:table-cell',
    render: (h) => formatPercent(h.weight),
  },
  {
    key: 'unrealizedPnL',
    label: 'P&L',
    align: 'right',
    sortable: true,
    mono: true,
    render: (h) => (
      <div
        className={cn(
          'inline-flex flex-col items-end leading-tight',
          h.unrealizedPnL > 0 && 'delta-positive',
          h.unrealizedPnL < 0 && 'delta-negative',
        )}
      >
        <span>{formatSignedCurrency(h.unrealizedPnL)}</span>
        <span className="text-small opacity-80">{formatSignedPercent(h.unrealizedPnLPct)}</span>
      </div>
    ),
  },
  {
    key: 'dayChange',
    label: 'Today',
    align: 'right',
    sortable: true,
    mono: true,
    render: (h) => (
      <div
        className={cn(
          'inline-flex flex-col items-end leading-tight',
          h.dayChange > 0 && 'delta-positive',
          h.dayChange < 0 && 'delta-negative',
        )}
      >
        <span>{formatSignedCurrency(h.dayChange)}</span>
        <span className="text-small opacity-80">{formatSignedPercent(h.dayChangePct)}</span>
      </div>
    ),
  },
];

export function HoldingsTable({ holdings }: { holdings: Holding[] }) {
  const [sortKey, setSortKey] = useState<SortKey>('marketValue');
  const [direction, setDirection] = useState<Direction>('desc');
  const navigate = useNavigate();

  const sorted = useMemo(() => {
    const list = [...holdings];
    const factor = direction === 'asc' ? 1 : -1;
    list.sort((a, b) => {
      const av = a[sortKey];
      const bv = b[sortKey];
      if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * factor;
      return ((av as number) - (bv as number)) * factor;
    });
    return list;
  }, [holdings, sortKey, direction]);

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      setDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setDirection(key === 'ticker' ? 'asc' : 'desc');
    }
  }

  return (
    <Table data-testid="holdings-table">
      <THead>
        <TR className="hover:bg-transparent">
          {columns.map((col) => (
            <TH
              key={col.key}
              align={col.align}
              scope="col"
              className={col.hideClass}
              aria-sort={
                sortKey === col.key
                  ? direction === 'asc'
                    ? 'ascending'
                    : 'descending'
                  : col.sortable
                    ? 'none'
                    : undefined
              }
            >
              {col.sortable ? (
                <button
                  type="button"
                  onClick={() => toggleSort(col.key)}
                  className={cn(
                    'inline-flex items-center gap-1 transition-colors hover:text-text',
                    col.align === 'right' && 'flex-row-reverse',
                    sortKey === col.key ? 'text-text' : 'text-text-muted',
                  )}
                >
                  <span>{col.label}</span>
                  {sortKey === col.key ? (
                    direction === 'asc' ? (
                      <ArrowUp size={10} aria-hidden />
                    ) : (
                      <ArrowDown size={10} aria-hidden />
                    )
                  ) : (
                    <ArrowUpDown size={10} aria-hidden className="opacity-40" />
                  )}
                </button>
              ) : (
                col.label
              )}
            </TH>
          ))}
        </TR>
      </THead>
      <TBody>
        {sorted.map((h) => (
          <TR
            key={h.ticker}
            onClick={() => navigate(`/analysis/${h.ticker}`)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                navigate(`/analysis/${h.ticker}`);
              }
            }}
            tabIndex={0}
            role="link"
            aria-label={`Open analysis for ${h.ticker}`}
            className="cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-focus-ring"
          >
            {columns.map((col) => (
              <TD key={col.key} align={col.align} mono={col.mono} className={col.hideClass}>
                {col.render(h)}
              </TD>
            ))}
          </TR>
        ))}
      </TBody>
    </Table>
  );
}
