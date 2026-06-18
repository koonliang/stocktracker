import { useEffect, useState } from 'react';
import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  CartesianGrid,
} from 'recharts';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { getPerformance, type LotMethod, type PerformanceWindow } from '@/api/performanceApi';
import type { PerformanceResponse } from '@/api/types';
import {
  formatCurrencyCode,
  formatDateISO,
  formatFxStatus,
  formatNumber,
  formatPercent,
  formatShares,
  formatSignedCurrencyCode,
} from '@/lib/format';
import { rgbVar } from '@/lib/colors';
import type { ConversionMetadata } from '@/api/types';

const windows: PerformanceWindow[] = ['1M', '3M', '6M', '1Y', 'YTD', 'ALL'];

export function PerformanceRoute() {
  const [window, setWindow] = useState<PerformanceWindow>('1Y');
  const [method, setMethod] = useState<LotMethod>('fifo');
  const [data, setData] = useState<PerformanceResponse | null>(null);
  const [status, setStatus] = useState<'idle' | 'loading' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);

  useEffect(() => {
    const refresh = () => setRefreshToken((value) => value + 1);
    globalThis.window.addEventListener('stocktracker:base-currency-changed', refresh);
    return () =>
      globalThis.window.removeEventListener('stocktracker:base-currency-changed', refresh);
  }, []);

  useEffect(() => {
    setStatus('loading');
    getPerformance(window, method)
      .then((response) => {
        setData(response);
        setStatus('idle');
        setError(null);
      })
      .catch((err: Error) => {
        setStatus('error');
        setError(err.message);
      });
  }, [window, method, refreshToken]);

  return (
    <>
      <PageHeader
        eyebrow="Performance"
        title="Returns"
        description="Realized lots, open-position P&L, and contribution by holding."
      />

      <div className="flex flex-col gap-6" data-testid="performance-page">
        <Card>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap gap-2" data-testid="perf-window-select">
              {windows.map((value) => (
                <Button
                  key={value}
                  variant={window === value ? 'primary' : 'secondary'}
                  size="sm"
                  onClick={() => setWindow(value)}
                  type="button"
                >
                  {value}
                </Button>
              ))}
            </div>
            <div className="flex gap-2" data-testid="lot-method-toggle">
              {(['fifo', 'lifo'] as LotMethod[]).map((value) => (
                <Button
                  key={value}
                  variant={method === value ? 'primary' : 'secondary'}
                  size="sm"
                  className="uppercase"
                  onClick={() => setMethod(value)}
                  type="button"
                >
                  {value}
                </Button>
              ))}
            </div>
          </div>
        </Card>

        {status === 'error' ? (
          <EmptyState
            eyebrow="Performance error"
            title="Performance could not be loaded."
            description={error ?? 'Try again after the backend is available.'}
          />
        ) : !data ? (
          <Card>
            <CardHeader eyebrow="Loading" title="Calculating performance" />
          </Card>
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-3">
              <Metric
                label="Realized P&L"
                value={formatSignedCurrencyCode(data.realizedPnL, data.baseCurrency)}
                conversions={[
                  ...data.closedLots.map((lot) => lot.realizedPnlConversion),
                  ...data.incomeEvents.map((event) => event.amountConversion),
                ]}
              />
              <Metric
                label="Unrealized P&L"
                value={formatSignedCurrencyCode(data.unrealizedPnL, data.baseCurrency)}
                conversions={data.contributions.map((row) => row.contributionConversion)}
              />
              <Metric label="TWR" value={formatPercent(data.timeWeightedReturnPct / 100)} />
            </div>

            <Card>
              <CardHeader eyebrow={data.window} title="Cumulative return" />
              <div className="h-72" data-testid="return-chart">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={data.returnSeries}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" tickFormatter={formatDateISO} minTickGap={32} />
                    <YAxis tickFormatter={(v) => `${formatNumber(v, 1)}%`} width={56} />
                    <Tooltip
                      formatter={(v) => [`${formatNumber(Number(v), 2)}%`, 'Return']}
                      labelFormatter={(label) => formatDateISO(String(label))}
                    />
                    <Line
                      type="monotone"
                      dataKey="cumulativeReturnPct"
                      stroke={rgbVar('--color-accent')}
                      dot={false}
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Card>

            <RealizedDetails data={data} />

            <Card padded={false}>
              <div className="p-5 pb-0 sm:p-6 sm:pb-0">
                <CardHeader eyebrow="Open positions" title="Contribution" />
              </div>
              <div className="overflow-x-auto" data-testid="contribution-table">
                <table className="min-w-full text-left text-body">
                  <tbody>
                    {data.contributions.map((row) => (
                      <tr key={row.symbol} className="border-t border-border">
                        <td className="px-5 py-3 font-medium">{row.symbol}</td>
                        <td className="px-5 py-3">
                          <div className="inline-flex flex-col gap-1">
                            <span>{formatNumber(row.contributionPct, 2)}%</span>
                            <FxStatus conversion={row.contributionConversion} />
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </>
        )}
      </div>
    </>
  );
}

function Metric({
  label,
  value,
  conversions = [],
}: {
  label: string;
  value: string;
  conversions?: Array<ConversionMetadata | undefined>;
}) {
  return (
    <Card>
      <div className="text-small uppercase text-text-subtle">{label}</div>
      <div className="mt-2 font-display text-title text-text">{value}</div>
      <FxStatus conversion={worstConversion(conversions)} />
    </Card>
  );
}

function RealizedDetails({ data }: { data: PerformanceResponse }) {
  const incomeEvents = data.incomeEvents ?? [];
  const closedLotTotal = data.closedLots.reduce((sum, lot) => sum + lot.realizedPnLBase, 0);
  const incomeTotal = incomeEvents.reduce((sum, event) => sum + event.amountBase, 0);

  return (
    <Card padded={false}>
      <div className="p-5 pb-0 sm:p-6 sm:pb-0">
        <CardHeader eyebrow={data.method.toUpperCase()} title="Realized P&L details" />
      </div>

      <div className="grid gap-3 border-b border-border px-5 pb-5 pt-3 text-body sm:grid-cols-3 sm:px-6">
        <BreakdownRow
          label="Closed lot gains"
          value={formatSignedCurrencyCode(closedLotTotal, data.baseCurrency)}
        />
        <BreakdownRow
          label="Dividend income"
          value={formatSignedCurrencyCode(incomeTotal, data.baseCurrency)}
        />
        <BreakdownRow
          label="Total realized P&L"
          value={formatSignedCurrencyCode(data.realizedPnL, data.baseCurrency)}
          strong
        />
      </div>

      <div className="px-5 pt-5 sm:px-6">
        <div className="text-small uppercase text-text-subtle">Closed lots</div>
      </div>
      <div className="overflow-x-auto" data-testid="realized-table">
        <table className="min-w-full text-left text-body">
          <thead className="text-small uppercase text-text-subtle">
            <tr>
              <th className="px-5 py-3">Symbol</th>
              <th className="px-5 py-3">Closed</th>
              <th className="px-5 py-3">Qty</th>
              <th className="px-5 py-3">Proceeds</th>
              <th className="px-5 py-3">P&L</th>
            </tr>
          </thead>
          <tbody>
            {data.closedLots.length === 0 ? (
              <tr className="border-t border-border">
                <td className="px-5 py-4 text-text-muted" colSpan={5}>
                  No closed lots in this period.
                </td>
              </tr>
            ) : (
              data.closedLots.map((lot, index) => (
                <tr
                  key={`${lot.symbol}-${lot.closeDate}-${index}`}
                  className="border-t border-border"
                >
                  <td className="px-5 py-3 font-medium">{lot.symbol}</td>
                  <td className="px-5 py-3">{formatDateISO(lot.closeDate)}</td>
                  <td className="px-5 py-3">{formatShares(lot.quantity)}</td>
                  <td className="px-5 py-3">
                    {formatCurrencyCode(lot.proceedsNative, lot.currency)}
                  </td>
                  <td className="px-5 py-3">
                    <div className="inline-flex flex-col gap-1">
                      <span>
                        {formatSignedCurrencyCode(lot.realizedPnLBase, data.baseCurrency)}
                      </span>
                      <FxStatus conversion={lot.realizedPnlConversion} />
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="px-5 pt-5 sm:px-6">
        <div className="text-small uppercase text-text-subtle">Income events</div>
      </div>
      <div className="overflow-x-auto" data-testid="income-events-table">
        <table className="min-w-full text-left text-body">
          <thead className="text-small uppercase text-text-subtle">
            <tr>
              <th className="px-5 py-3">Symbol</th>
              <th className="px-5 py-3">Date</th>
              <th className="px-5 py-3">Type</th>
              <th className="px-5 py-3">Amount</th>
            </tr>
          </thead>
          <tbody>
            {incomeEvents.length === 0 ? (
              <tr className="border-t border-border">
                <td className="px-5 py-4 text-text-muted" colSpan={4}>
                  No income events in this period.
                </td>
              </tr>
            ) : (
              incomeEvents.map((event, index) => (
                <tr
                  key={`${event.symbol}-${event.date}-${index}`}
                  className="border-t border-border"
                >
                  <td className="px-5 py-3 font-medium">{event.symbol}</td>
                  <td className="px-5 py-3">{formatDateISO(event.date)}</td>
                  <td className="px-5 py-3">{formatIncomeType(event.type)}</td>
                  <td className="px-5 py-3">
                    <div className="inline-flex flex-col gap-1">
                      <span>{formatSignedCurrencyCode(event.amountBase, data.baseCurrency)}</span>
                      <FxStatus conversion={event.amountConversion} />
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function formatIncomeType(type: string) {
  return type === 'dividend' ? 'Dividend' : type;
}

function FxStatus({ conversion }: { conversion?: ConversionMetadata }) {
  const label = formatFxStatus(conversion?.fxStatus);
  if (!label) return null;
  return (
    <span className="mt-1 inline-flex w-fit rounded border border-warning/40 px-1.5 py-0.5 text-[0.6875rem] uppercase leading-none text-warning">
      {label}
    </span>
  );
}

function worstConversion(
  conversions: Array<ConversionMetadata | undefined>,
): ConversionMetadata | undefined {
  const present = conversions.filter(Boolean) as ConversionMetadata[];
  return (
    present.find((conversion) => conversion.fxStatus === 'unavailable') ??
    present.find((conversion) => conversion.fxStatus === 'stale')
  );
}

function BreakdownRow({
  label,
  value,
  strong = false,
}: {
  label: string;
  value: string;
  strong?: boolean;
}) {
  return (
    <div>
      <div className="text-small uppercase text-text-subtle">{label}</div>
      <div
        className={strong ? 'mt-1 font-display text-title text-text' : 'mt-1 font-medium text-text'}
      >
        {value}
      </div>
    </div>
  );
}
