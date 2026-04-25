import { formatCurrency, formatSignedCurrency, formatSignedPercent } from '@/lib/format';
import { cn } from '@/lib/cn';

type Props = {
  symbol: string;
  name: string;
  sector?: string;
  exchange?: string;
  currentPrice: number | null;
  dayChange: number | null;
  dayChangePct: number | null;
};

export function AnalysisHeader({
  symbol,
  name,
  sector,
  exchange,
  currentPrice,
  dayChange,
  dayChangePct,
}: Props) {
  const positive = (dayChange ?? 0) > 0;
  const negative = (dayChange ?? 0) < 0;
  return (
    <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <div className="eyebrow flex items-center gap-2">
          <span>{exchange ?? 'Listed'}</span>
          {sector && <span aria-hidden>·</span>}
          {sector && <span>{sector}</span>}
        </div>
        <div className="mt-1 flex items-baseline gap-3">
          <h1 className="font-display text-display font-mono tracking-tight text-text">{symbol}</h1>
          <span className="truncate text-body text-text-muted">{name}</span>
        </div>
        <div className="page-title-rule" aria-hidden />
      </div>
      <div className="flex shrink-0 items-baseline gap-3">
        <span className="font-mono tabular text-display-lg text-text">
          {formatCurrency(currentPrice)}
        </span>
        <span
          className={cn(
            'font-mono tabular text-body',
            positive && 'delta-positive',
            negative && 'delta-negative',
            !positive && !negative && 'text-text-muted',
          )}
        >
          <span>{formatSignedCurrency(dayChange)}</span>
          <span className="ml-2 opacity-80">{formatSignedPercent(dayChangePct)}</span>
        </span>
      </div>
    </div>
  );
}
