import { useMemo } from 'react';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { PriceBar, TimeRange } from '@/lib/types';
import { TIME_RANGES } from '@/lib/types';
import { formatCurrency, formatDateISO } from '@/lib/format';
import { cn } from '@/lib/cn';
import { rgbVar } from '@/lib/colors';

const RANGE_BARS: Record<TimeRange, number | null> = {
  '1D': 1,
  '1W': 5,
  '1M': 21,
  '3M': 63,
  '1Y': 252,
  '5Y': 1260,
  ALL: null,
};

export function filterBarsByRange(bars: PriceBar[], range: TimeRange): PriceBar[] {
  const limit = RANGE_BARS[range];
  if (limit == null || bars.length <= limit) return bars;
  return bars.slice(bars.length - limit);
}

type TooltipProps = {
  active?: boolean;
  payload?: Array<{ payload: PriceBar }>;
};

function ChartTooltip({ active, payload }: TooltipProps) {
  if (!active || !payload?.[0]) return null;
  const bar = payload[0].payload;
  return (
    <div className="rounded-md border border-border bg-surface px-3 py-2 text-small shadow-popover">
      <div className="text-text-muted">{formatDateISO(bar.date)}</div>
      <div className="mt-0.5 font-mono tabular text-text">{formatCurrency(bar.close)}</div>
    </div>
  );
}

type Props = {
  bars: PriceBar[];
  range: TimeRange;
  onRangeChange: (r: TimeRange) => void;
};

export function PriceChart({ bars, range, onRangeChange }: Props) {
  const data = useMemo(() => filterBarsByRange(bars, range), [bars, range]);

  const positive = data.length > 1 && data[data.length - 1]!.close >= data[0]!.close;
  const stroke = positive ? rgbVar('--color-positive') : rgbVar('--color-negative');

  const yDomain = useMemo<[number, number]>(() => {
    if (data.length === 0) return [0, 1];
    let lo = data[0]!.close;
    let hi = lo;
    for (const b of data) {
      if (b.close < lo) lo = b.close;
      if (b.close > hi) hi = b.close;
    }
    const pad = (hi - lo) * 0.08 || hi * 0.02 || 1;
    return [lo - pad, hi + pad];
  }, [data]);

  return (
    <div className="flex flex-col gap-4">
      <div
        role="group"
        aria-label="Time range"
        className="inline-flex flex-wrap gap-1 self-start rounded-md border border-border bg-surface p-1"
      >
        {TIME_RANGES.map((r) => {
          const selected = r === range;
          return (
            <button
              key={r}
              type="button"
              aria-pressed={selected}
              onClick={() => onRangeChange(r)}
              className={cn(
                'rounded px-2.5 py-1 font-mono text-small font-medium transition-colors',
                'focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring',
                selected
                  ? 'bg-accent text-accent-fg'
                  : 'text-text-muted hover:bg-surface-alt hover:text-text',
              )}
            >
              {r}
            </button>
          );
        })}
      </div>

      <div
        data-testid="price-chart"
        data-bar-count={data.length}
        aria-label={`Price chart, ${data.length} data points`}
        role="img"
        className="h-[320px] w-full"
      >
        {data.length > 0 ? (
          <ResponsiveContainer>
            <AreaChart data={data} margin={{ top: 10, right: 8, left: 8, bottom: 0 }}>
              <defs>
                <linearGradient id="priceFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={stroke} stopOpacity={0.25} />
                  <stop offset="100%" stopColor={stroke} stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis
                dataKey="date"
                tick={{ fontSize: 11, fill: 'var(--color-text-subtle)' }}
                tickLine={false}
                axisLine={false}
                minTickGap={32}
              />
              <YAxis
                domain={yDomain}
                tick={{ fontSize: 11, fill: 'var(--color-text-subtle)' }}
                tickLine={false}
                axisLine={false}
                width={56}
                tickFormatter={(v: number) => formatCurrency(v, { cents: false })}
              />
              <Tooltip content={<ChartTooltip />} />
              <Area
                type="monotone"
                dataKey="close"
                stroke={stroke}
                strokeWidth={1.75}
                fill="url(#priceFill)"
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex h-full items-center justify-center text-small text-text-muted">
            No price data for this range.
          </div>
        )}
      </div>
    </div>
  );
}
