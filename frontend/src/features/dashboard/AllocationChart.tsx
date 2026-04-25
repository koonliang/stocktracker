import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import type { Holding } from '@/lib/types';
import { formatCurrency, formatPercent } from '@/lib/format';

// Restrained, editorial palette — not neon. Cycles through 8 accent-adjacent hues.
const PALETTE = [
  '#1E2A5E', // indigo-ink (accent)
  '#4A5A9C',
  '#8A6A3D',
  '#1D6B4A', // dark green
  '#9B2C2C', // burgundy
  '#3D6B86', // slate blue
  '#7B6F5E',
  '#2C5E5E',
];

type Datum = { ticker: string; name: string; value: number; weight: number; fill: string };

type TooltipProps = {
  active?: boolean;
  payload?: Array<{ payload: Datum }>;
};

function ChartTooltip({ active, payload }: TooltipProps) {
  if (!active || !payload?.[0]) return null;
  const d = payload[0].payload;
  return (
    <div className="rounded-md border border-border bg-surface px-3 py-2 text-small shadow-popover">
      <div className="font-mono text-small font-semibold text-text">{d.ticker}</div>
      <div className="mt-0.5 text-text-muted">{d.name}</div>
      <div className="mt-1 flex gap-3 tabular">
        <span className="text-text">{formatCurrency(d.value, { cents: false })}</span>
        <span className="text-text-muted">{formatPercent(d.weight)}</span>
      </div>
    </div>
  );
}

export function AllocationChart({ holdings }: { holdings: Holding[] }) {
  const data: Datum[] = holdings.map((h, i) => ({
    ticker: h.ticker,
    name: h.name,
    value: h.marketValue,
    weight: h.weight,
    fill: PALETTE[i % PALETTE.length]!,
  }));

  if (data.length === 0) return null;

  return (
    <div className="flex min-w-0 flex-col gap-6 sm:flex-row sm:items-center">
      <div className="mx-auto h-[220px] w-full max-w-[260px] flex-shrink-0" aria-hidden>
        <ResponsiveContainer>
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              nameKey="ticker"
              innerRadius="62%"
              outerRadius="96%"
              paddingAngle={1}
              strokeWidth={0}
            >
              {data.map((d) => (
                <Cell key={d.ticker} fill={d.fill} />
              ))}
            </Pie>
            <Tooltip content={<ChartTooltip />} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      {/* Accessible legend / screen-reader fallback; also rendered visually */}
      <ul className="min-w-0 flex-1 space-y-1.5">
        {data.map((d) => (
          <li
            key={d.ticker}
            className="flex min-w-0 items-center justify-between gap-3 text-small tabular"
          >
            <span className="flex min-w-0 items-center gap-2">
              <span
                aria-hidden
                className="h-2 w-2 flex-shrink-0 rounded-full"
                style={{ background: d.fill }}
              />
              <span className="flex-shrink-0 font-mono font-semibold text-text">{d.ticker}</span>
              <span className="min-w-0 truncate text-text-muted">{d.name}</span>
            </span>
            <span className="flex-shrink-0 text-text-muted">{formatPercent(d.weight)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
