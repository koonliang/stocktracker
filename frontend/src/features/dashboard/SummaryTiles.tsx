import type { PortfolioSummary } from '@/lib/types';
import { formatCurrency, formatSignedCurrency, formatSignedPercent } from '@/lib/format';
import { cn } from '@/lib/cn';

type Props = { summary: PortfolioSummary };

function Tile({
  eyebrow,
  value,
  delta,
  deltaPct,
  tone,
}: {
  eyebrow: string;
  value: string;
  delta?: string;
  deltaPct?: string;
  tone?: 'positive' | 'negative' | 'neutral';
}) {
  return (
    <div
      // Fixed row grid: eyebrow | value | delta — so the value baseline is at
      // the same y-position across all four tiles, even when some tiles have
      // no delta row.
      className="grid min-w-0 grid-rows-[auto_1fr_auto] gap-3 overflow-hidden rounded-lg border border-border bg-surface p-5 shadow-card sm:p-6"
    >
      <div className="eyebrow">{eyebrow}</div>
      <div className="flex min-w-0 items-end justify-end">
        <div className="min-w-0 truncate font-display text-display-lg font-semibold leading-none tracking-tight text-text tabular">
          {value}
        </div>
      </div>
      <div
        className={cn(
          'flex min-h-[1.25rem] items-baseline justify-end gap-2 text-small tabular',
          tone === 'positive' && 'text-positive',
          tone === 'negative' && 'text-negative',
          tone === 'neutral' && 'text-text-muted',
        )}
      >
        {delta && <span>{delta}</span>}
        {deltaPct && <span>{deltaPct}</span>}
      </div>
    </div>
  );
}

function tone(n: number): 'positive' | 'negative' | 'neutral' {
  if (n > 0) return 'positive';
  if (n < 0) return 'negative';
  return 'neutral';
}

export function SummaryTiles({ summary }: Props) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-4">
      <Tile
        eyebrow="Market Value"
        value={formatCurrency(summary.totalMarketValue, { cents: false })}
      />
      <Tile eyebrow="Cost Basis" value={formatCurrency(summary.totalCostBasis, { cents: false })} />
      <Tile
        eyebrow="Unrealised P&L"
        value={formatSignedCurrency(summary.totalUnrealizedPnL, { cents: false })}
        deltaPct={formatSignedPercent(summary.totalUnrealizedPnLPct)}
        tone={tone(summary.totalUnrealizedPnL)}
      />
      <Tile
        eyebrow="Today"
        value={formatSignedCurrency(summary.totalDayChange, { cents: false })}
        deltaPct={formatSignedPercent(summary.totalDayChangePct)}
        tone={tone(summary.totalDayChange)}
      />
    </div>
  );
}
