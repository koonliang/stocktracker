import type { PortfolioSummary } from '@/lib/types';
import {
  formatCompactCurrencyCode,
  formatCurrencyCode,
  formatFxStatus,
  formatSignedCompactCurrencyCode,
  formatSignedCurrencyCode,
  formatSignedPercent,
} from '@/lib/format';
import { cn } from '@/lib/cn';
import type { ConversionMetadata } from '@/api/types';

type Props = { summary: PortfolioSummary };

function Tile({
  eyebrow,
  value,
  mobileValue,
  delta,
  deltaPct,
  conversion,
  tone,
}: {
  eyebrow: string;
  value: string;
  mobileValue?: string;
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
      className="grid min-w-0 grid-rows-[auto_1fr_auto] gap-1 overflow-hidden rounded-lg border border-border bg-surface p-3 shadow-card sm:gap-3 sm:p-6"
    >
      <div className="eyebrow">{eyebrow}</div>
      <div className="flex min-w-0 items-end justify-end">
        <div className="flex min-w-0 flex-col items-end gap-1">
          <div className="min-w-0 font-display font-semibold leading-none tracking-tight text-text tabular">
            <span className="block whitespace-nowrap text-[clamp(1.55rem,6vw,2.1rem)] sm:hidden">
              {mobileValue ?? value}
            </span>
            <span className="hidden whitespace-nowrap text-display-lg sm:block">{value}</span>
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
    <div data-testid="summary-tiles">
      <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-4">
        <Tile
          eyebrow="Market Value"
          value={formatCurrencyCode(summary.totalMarketValue, base, { cents: false })}
          mobileValue={formatCompactCurrencyCode(summary.totalMarketValue, base)}
          conversion={summary.marketValueConversion}
        />
        <Tile
          eyebrow="Cost Basis"
          value={formatCurrencyCode(summary.totalCostBasis, base, { cents: false })}
          mobileValue={formatCompactCurrencyCode(summary.totalCostBasis, base)}
          conversion={summary.costBasisConversion}
        />
        <Tile
          eyebrow="Unrealised P&L"
          value={formatSignedCurrencyCode(summary.totalUnrealizedPnL, base, { cents: false })}
          mobileValue={formatSignedCompactCurrencyCode(summary.totalUnrealizedPnL, base)}
          deltaPct={formatSignedPercent(summary.totalUnrealizedPnLPct)}
          tone={tone(summary.totalUnrealizedPnL)}
        />
        <Tile
          eyebrow="Today"
          value={formatSignedCurrencyCode(summary.totalDayChange, base, { cents: false })}
          mobileValue={formatSignedCompactCurrencyCode(summary.totalDayChange, base)}
          deltaPct={formatSignedPercent(summary.totalDayChangePct)}
          conversion={summary.dayChangeConversion}
          tone={tone(summary.totalDayChange)}
        />
      </div>
    </div>
  );
}
