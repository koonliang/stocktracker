import type { KeyStats } from '@/lib/types';
import { formatCompactNumber, formatCurrencyCode, formatNumber } from '@/lib/format';

type Props = { stats: KeyStats | null | undefined; currency?: string };

type Stat = { label: string; value: string };

function buildStats(stats: KeyStats | null | undefined, currency?: string): Stat[] {
  return [
    { label: 'Open', value: formatCurrencyCode(stats?.open, currency) },
    { label: 'Day High', value: formatCurrencyCode(stats?.high, currency) },
    { label: 'Day Low', value: formatCurrencyCode(stats?.low, currency) },
    { label: 'Previous Close', value: formatCurrencyCode(stats?.previousClose, currency) },
    { label: 'Volume', value: formatCompactNumber(stats?.volume) },
    { label: '52W High', value: formatCurrencyCode(stats?.week52High, currency) },
    { label: '52W Low', value: formatCurrencyCode(stats?.week52Low, currency) },
    { label: 'Market Cap', value: formatCompactNumber(stats?.marketCap) },
    { label: 'P/E Ratio', value: formatNumber(stats?.peRatio ?? null) },
  ];
}

export function KeyStatsGrid({ stats, currency }: Props) {
  const items = buildStats(stats, currency);
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
