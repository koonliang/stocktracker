import { useEffect, useRef, useState } from 'react';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { FAB } from '@/components/ui/FAB';
import { createAlert, deleteAlert, listAlerts, updateAlert } from '@/api/alertsApi';
import { searchInstruments } from '@/api/searchApi';
import type { Alert, AlertCondition, SymbolSearchResult } from '@/api/types';
import { messageFromError, notifyActionFeedback } from '@/lib/actionFeedback';
import { formatDateISO, formatNumber } from '@/lib/format';
import { cn } from '@/lib/cn';

export function AlertsRoute() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [symbol, setSymbol] = useState('');
  const [conditionType, setConditionType] = useState<AlertCondition>('price_above');
  const [threshold, setThreshold] = useState('');
  const [editingAlertId, setEditingAlertId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [tickerResults, setTickerResults] = useState<SymbolSearchResult[]>([]);
  const [selectingTicker, setSelectingTicker] = useState<string | null>(null);
  const tickerRequestId = useRef(0);

  const load = () =>
    listAlerts()
      .then((response) => setAlerts(response.alerts))
      .catch((err: Error) => setError(err.message));

  function resetForm() {
    setEditingAlertId(null);
    setIsCreateOpen(false);
    setSymbol('');
    setConditionType('price_above');
    setThreshold('');
    setTickerResults([]);
    setSelectingTicker(null);
  }

  async function handleSubmit() {
    const request = {
      symbol: symbol.toUpperCase(),
      conditionType,
      threshold: Number(threshold),
    };
    try {
      if (editingAlertId) {
        await updateAlert(editingAlertId, request);
        notifyActionFeedback({ scope: 'alert', operation: 'update', outcome: 'success' });
      } else {
        await createAlert(request);
        notifyActionFeedback({ scope: 'alert', operation: 'add', outcome: 'success' });
      }
      setError(null);
      resetForm();
      await load();
    } catch (err) {
      setError(messageFromError(err));
      notifyActionFeedback({
        scope: 'alert',
        operation: editingAlertId ? 'update' : 'add',
        outcome: 'failure',
        message: messageFromError(err),
      });
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <>
      <PageHeader
        eyebrow="Alerts"
        title="Price Alerts"
        description="Threshold alerts fire once per crossing and appear as in-app notifications."
        actions={
          <Button size="sm" className="hidden md:inline-flex" onClick={() => setIsCreateOpen(true)}>
            <Plus size={14} aria-hidden />
            New Alert
          </Button>
        }
      />
      <div className="flex flex-col gap-6" data-testid="alerts-page">
        <Card padded={false}>
          <div className="p-5 pb-0 sm:p-6 sm:pb-0">
            <CardHeader
              eyebrow="Active rules"
              title={`${alerts.length} alert${alerts.length === 1 ? '' : 's'}`}
            />
          </div>
          {alerts.length === 0 ? (
            <div className="p-5 sm:p-6">
              <EmptyState title="No alerts yet" description="Tap + to set a price alert." />
            </div>
          ) : null}
          <div className="divide-y divide-border">
            {alerts.map((alert) => (
              <div
                key={alert.id}
                className="flex items-center justify-between gap-3 p-5"
                data-testid="alert-row"
              >
                <div className="min-w-0">
                  <div className="font-medium">{alert.symbol}</div>
                  <div className="text-small text-text-muted">
                    {alert.conditionType.replace('_', ' ')} {formatNumber(alert.threshold, 2)} ·{' '}
                    {alert.armed ? 'armed' : 'waiting to re-arm'} ·{' '}
                    {formatDateISO(alert.createdAt.slice(0, 10))}
                  </div>
                </div>
                <div className="flex shrink-0 gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="w-9 px-0"
                    title="Edit"
                    data-testid="alert-edit"
                    onClick={() => {
                      setEditingAlertId(alert.id);
                      setIsCreateOpen(true);
                      setSymbol(alert.symbol);
                      setConditionType(alert.conditionType);
                      setThreshold(String(alert.threshold));
                      setError(null);
                    }}
                  >
                    <Pencil size={16} aria-hidden />
                    <span className="sr-only">Edit</span>
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="w-9 px-0 text-negative hover:text-negative"
                    title="Delete"
                    data-testid="alert-delete"
                    onClick={() =>
                      deleteAlert(alert.id)
                        .then(async () => {
                          notifyActionFeedback({
                            scope: 'alert',
                            operation: 'delete',
                            outcome: 'success',
                          });
                          if (editingAlertId === alert.id) {
                            resetForm();
                          }
                          await load();
                        })
                        .catch((err: Error) => {
                          setError(messageFromError(err));
                          notifyActionFeedback({
                            scope: 'alert',
                            operation: 'delete',
                            outcome: 'failure',
                            message: messageFromError(err),
                          });
                        })
                    }
                  >
                    <Trash2 size={16} aria-hidden />
                    <span className="sr-only">Delete</span>
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Dialog
        open={isCreateOpen}
        onClose={resetForm}
        title={editingAlertId ? 'Edit alert' : 'New alert'}
        description="Create a price alert and review it later from triggered notifications."
        className="max-w-4xl"
        footer={
          <>
            <Button type="button" variant="ghost" onClick={resetForm}>
              Cancel
            </Button>
            <Button type="submit" form="alert-form-dialog" data-testid="alert-submit">
              {editingAlertId ? 'Update' : 'Create'}
            </Button>
          </>
        }
      >
        <form
          id="alert-form-dialog"
          className="grid gap-4 overflow-visible md:grid-cols-3"
          data-testid="alert-form"
          onSubmit={(event) => {
            event.preventDefault();
            void handleSubmit();
          }}
        >
          <div className="relative">
            <Label htmlFor="alert-symbol">Symbol</Label>
            <Input
              id="alert-symbol"
              data-testid="alert-symbol"
              value={symbol}
              onChange={(event) => {
                const raw = event.target.value;
                setSymbol(raw);
                setSelectingTicker(null);
                if (raw.length < 1) {
                  setTickerResults([]);
                  return;
                }
                const id = ++tickerRequestId.current;
                if (raw.length >= 1) {
                  void searchInstruments(raw).then((matches) => {
                    if (id !== tickerRequestId.current) return;
                    setTickerResults(matches);
                  });
                } else {
                  setTickerResults([]);
                }
              }}
            />
            {tickerResults.length > 0 && !selectingTicker ? (
              <ul className="absolute z-10 mt-1 w-full rounded-md border border-border bg-surface text-body shadow-lg">
                {tickerResults.map((result) => (
                  <li key={result.symbol}>
                    <button
                      type="button"
                      className={cn(
                        'w-full px-3 py-2 text-left hover:bg-surface-hover',
                        selectingTicker === result.symbol && 'bg-surface-hover',
                      )}
                      data-testid="ticker-option"
                      onClick={() => {
                        setSelectingTicker(result.symbol);
                        setSymbol(result.symbol.toUpperCase());
                        setTickerResults([]);
                      }}
                    >
                      <span className="font-medium">{result.symbol}</span>
                      <span className="ml-2 text-text-muted">{result.name}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
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
              required
              value={threshold}
              onChange={(event) => setThreshold(event.target.value)}
            />
          </div>
        </form>
        {error ? <p className="mt-3 text-small text-danger">{error}</p> : null}
      </Dialog>

      <FAB label="New alert" onClick={() => setIsCreateOpen(true)} />
    </>
  );
}
