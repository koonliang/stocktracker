import { useEffect, useState } from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { createAlert, deleteAlert, listAlerts } from '@/api/alertsApi';
import type { Alert, AlertCondition } from '@/api/types';
import { formatDateISO, formatNumber } from '@/lib/format';

export function AlertsRoute() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [symbol, setSymbol] = useState('AAPL');
  const [conditionType, setConditionType] = useState<AlertCondition>('price_above');
  const [threshold, setThreshold] = useState('200');
  const [error, setError] = useState<string | null>(null);

  const load = () =>
    listAlerts()
      .then((response) => setAlerts(response.alerts))
      .catch((err: Error) => setError(err.message));

  useEffect(() => {
    void load();
  }, []);

  return (
    <>
      <PageHeader
        eyebrow="Alerts"
        title="Price Alerts"
        description="Threshold alerts fire once per crossing and appear as in-app notifications."
      />
      <div className="flex flex-col gap-6" data-testid="alerts-page">
        <Card>
          <CardHeader eyebrow="Create" title="New alert" />
          <form
            className="grid gap-4 md:grid-cols-[1fr_1fr_1fr_auto]"
            data-testid="alert-form"
            onSubmit={(event) => {
              event.preventDefault();
              createAlert({ symbol, conditionType, threshold: Number(threshold) })
                .then(() => {
                  setError(null);
                  return load();
                })
                .catch((err: Error) => setError(err.message));
            }}
          >
            <div>
              <Label htmlFor="alert-symbol">Symbol</Label>
              <Input
                id="alert-symbol"
                data-testid="alert-symbol"
                value={symbol}
                onChange={(event) => setSymbol(event.target.value.toUpperCase())}
              />
            </div>
            <div>
              <Label htmlFor="alert-condition">Condition</Label>
              <select
                id="alert-condition"
                className="h-10 w-full rounded-md border border-border bg-surface px-3 text-body text-text hover:border-border-strong focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring"
                data-testid="alert-condition"
                value={conditionType}
                onChange={(event) => setConditionType(event.target.value as AlertCondition)}
              >
                <option value="price_above">Price above</option>
                <option value="price_below">Price below</option>
                <option value="pct_change">% change</option>
              </select>
            </div>
            <div>
              <Label htmlFor="alert-threshold">Threshold</Label>
              <Input
                id="alert-threshold"
                data-testid="alert-threshold"
                inputMode="decimal"
                value={threshold}
                onChange={(event) => setThreshold(event.target.value)}
              />
            </div>
            <div className="flex items-end">
              <Button type="submit" data-testid="alert-submit">
                Save
              </Button>
            </div>
          </form>
          {error ? <p className="mt-3 text-small text-danger">{error}</p> : null}
        </Card>

        <Card padded={false}>
          <div className="p-5 pb-0 sm:p-6 sm:pb-0">
            <CardHeader
              eyebrow="Active rules"
              title={`${alerts.length} alert${alerts.length === 1 ? '' : 's'}`}
            />
          </div>
          <div className="divide-y divide-border">
            {alerts.map((alert) => (
              <div
                key={alert.id}
                className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between"
                data-testid="alert-row"
              >
                <div>
                  <div className="font-medium">{alert.symbol}</div>
                  <div className="text-small text-text-muted">
                    {alert.conditionType.replace('_', ' ')} {formatNumber(alert.threshold, 2)} ·{' '}
                    {alert.armed ? 'armed' : 'waiting to re-arm'} ·{' '}
                    {formatDateISO(alert.createdAt.slice(0, 10))}
                  </div>
                </div>
                <Button
                  variant="secondary"
                  data-testid="alert-delete"
                  onClick={() => deleteAlert(alert.id).then(load)}
                >
                  Delete
                </Button>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </>
  );
}
