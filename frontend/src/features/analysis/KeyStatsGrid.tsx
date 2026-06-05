import type { KeyStats } from '@/lib/types';
import { formatCompactNumber, formatCurrency, formatNumber } from '@/lib/format';

type Props = { stats: KeyStats | null | undefined };

type Stat = { label: string; value: string };

function buildStats(stats: KeyStats | null | undefined): Stat[] {
  return [
    { label: 'Open', value: formatCurrency(stats?.open) },
    { label: 'Day High', value: formatCurrency(stats?.high) },
    { label: 'Day Low', value: formatCurrency(stats?.low) },
    { label: 'Previous Close', value: formatCurrency(stats?.previousClose) },
    { label: 'Volume', value: formatCompactNumber(stats?.volume) },
    { label: '52W High', value: formatCurrency(stats?.week52High) },
    { label: '52W Low', value: formatCurrency(stats?.week52Low) },
    { label: 'Market Cap', value: formatCompactNumber(stats?.marketCap) },
    { label: 'P/E Ratio', value: formatNumber(stats?.peRatio ?? null) },
  ];
}

export function KeyStatsGrid({ stats }: Props) {
  const items = buildStats(stats);
  return (
    <dl
      data-testid="key-stats-grid"
      className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border sm:grid-cols-3"
    >
      {items.map((s) => (
        <div key={s.label} className="flex flex-col gap-1 bg-surface p-4">
          <dt className="eyebrow">{s.label}</dt>
          <dd className="font-mono tabular text-body text-text">{s.value}</dd>
        </div>
      ))}
    </dl>
  );
}
