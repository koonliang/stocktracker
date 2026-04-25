import { useMemo } from 'react';
import { usePortfolioStore } from '@/stores/portfolioStore';
import { buildPriceLookup, computeHoldings } from '@/lib/portfolio';
import { loadPrices, loadTickers } from '@/lib/seed';
import {
  formatCurrency,
  formatShares,
  formatSignedCurrency,
  formatSignedPercent,
} from '@/lib/format';
import { cn } from '@/lib/cn';

type Props = { symbol: string };

export function PositionSummary({ symbol }: Props) {
  const transactions = usePortfolioStore((s) => s.transactions);
  const holding = useMemo(() => {
    const tickerMap = new Map(loadTickers().map((t) => [t.symbol, t]));
    const lookup = buildPriceLookup(loadPrices());
    const all = computeHoldings(transactions, lookup, tickerMap);
    return all.find((h) => h.ticker === symbol) ?? null;
  }, [transactions, symbol]);

  if (!holding || holding.shares <= 0) return null;

  const tone = (n: number) =>
    n > 0 ? 'delta-positive' : n < 0 ? 'delta-negative' : 'text-text-muted';

  return (
    <dl
      aria-label="Your position"
      className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border sm:grid-cols-4"
    >
      <Cell label="Shares" value={formatShares(holding.shares)} />
      <Cell label="Avg Cost" value={formatCurrency(holding.averageCost)} />
      <Cell label="Market Value" value={formatCurrency(holding.marketValue, { cents: false })} />
      <Cell
        label="Unrealised P&L"
        value={formatSignedCurrency(holding.unrealizedPnL)}
        sub={formatSignedPercent(holding.unrealizedPnLPct)}
        valueClassName={tone(holding.unrealizedPnL)}
      />
    </dl>
  );
}

function Cell({
  label,
  value,
  sub,
  valueClassName,
}: {
  label: string;
  value: string;
  sub?: string;
  valueClassName?: string;
}) {
  return (
    <div className="flex flex-col gap-1 bg-surface p-4">
      <dt className="eyebrow">{label}</dt>
      <dd className={cn('font-mono tabular text-body', valueClassName ?? 'text-text')}>
        {value}
        {sub && <span className="ml-2 text-small opacity-80">{sub}</span>}
      </dd>
    </div>
  );
}
