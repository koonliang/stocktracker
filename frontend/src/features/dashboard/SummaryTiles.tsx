import type { PortfolioSummary } from '@/lib/types';
import {
  formatCurrencyCode,
  formatFxStatus,
  formatSignedCurrencyCode,
  formatSignedPercent,
} from '@/lib/format';
import { cn } from '@/lib/cn';
import type { ConversionMetadata } from '@/api/types';

type Props = { summary: PortfolioSummary };

function Tile({
  eyebrow,
  value,
  delta,
  deltaPct,
  conversion,
  tone,
}: {
  eyebrow: string;
  value: string;
  delta?: string;
  deltaPct?: string;
  conversion?: ConversionMetadata;
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
        <div className="flex min-w-0 flex-col items-end gap-1">
          <div className="min-w-0 truncate font-display text-display-lg font-semibold leading-none tracking-tight text-text tabular">
            {value}
          </div>
          <FxStatus conversion={conversion} />
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

function FxStatus({ conversion }: { conversion?: ConversionMetadata }) {
  const label = formatFxStatus(conversion?.fxStatus);
  if (!label) return null;
  return (
    <span className="rounded border border-warning/40 px-1.5 py-0.5 text-[0.6875rem] uppercase leading-none text-warning">
      {label}
    </span>
  );
}

function tone(n: number): 'positive' | 'negative' | 'neutral' {
  if (n > 0) return 'positive';
  if (n < 0) return 'negative';
  return 'neutral';
}

export function SummaryTiles({ summary }: Props) {
  const base = summary.baseCurrency;
  return (
    <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-4" data-testid="summary-tiles">
      <Tile
        eyebrow="Market Value"
        value={formatCurrencyCode(summary.totalMarketValue, base, { cents: false })}
        conversion={summary.marketValueConversion}
      />
      <Tile
        eyebrow="Cost Basis"
        value={formatCurrencyCode(summary.totalCostBasis, base, { cents: false })}
        conversion={summary.costBasisConversion}
      />
      <Tile
        eyebrow="Unrealised P&L"
        value={formatSignedCurrencyCode(summary.totalUnrealizedPnL, base, { cents: false })}
        deltaPct={formatSignedPercent(summary.totalUnrealizedPnLPct)}
        tone={tone(summary.totalUnrealizedPnL)}
      />
      <Tile
        eyebrow="Today"
        value={formatSignedCurrencyCode(summary.totalDayChange, base, { cents: false })}
        deltaPct={formatSignedPercent(summary.totalDayChangePct)}
        conversion={summary.dayChangeConversion}
        tone={tone(summary.totalDayChange)}
      />
    </div>
  );
}
