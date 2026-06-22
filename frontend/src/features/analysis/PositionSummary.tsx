import type { InstrumentAnalysisResponse } from '@/lib/types';
import { formatCurrency, formatSignedCurrency, formatSignedPercent } from '@/lib/format';
import { cn } from '@/lib/cn';

type Props = { summary: InstrumentAnalysisResponse['positionSummary'] };

export function PositionSummary({ summary }: Props) {
  if (!summary || summary.shares <= 0) return null;

  const tone = (n: number) =>
    n > 0 ? 'delta-positive' : n < 0 ? 'delta-negative' : 'text-text-muted';

  return (
    <dl
      aria-label="Your position"
      className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border sm:grid-cols-4"
    >
      <Cell label="Shares" value={summary.shares.toLocaleString()} />
      <Cell label="Avg Cost" value={formatCurrency(summary.averageCost)} />
      <Cell label="Market Value" value={formatCurrency(summary.marketValue, { cents: false })} />
      <Cell
        label="Unrealised P&L"
        value={formatSignedCurrency(summary.unrealizedPnL)}
        sub={formatSignedPercent(summary.unrealizedPnLPct)}
        valueClassName={tone(summary.unrealizedPnL)}
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
        <span className="block">{value}</span>
        {sub && <span className="block text-small opacity-80">{sub}</span>}
      </dd>
    </div>
  );
}
