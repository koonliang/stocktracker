import { useEffect, useRef, useState } from 'react';
import { addInstrument, searchInstruments } from '@/api/searchApi';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import type {
  SymbolSearchResult,
  TransactionImportNormalizedRow,
  TransactionType,
} from '@/lib/types';
import { todayISO } from '@/lib/format';

type Props = {
  pending?: boolean;
  formId?: string;
  showSubmit?: boolean;
  submitLabel?: string;
  onSubmit: (row: TransactionImportNormalizedRow) => void | Promise<void>;
};

const TYPES: TransactionType[] = [
  'buy',
  'sell',
  'dividend',
  'split',
  'deposit',
  'withdrawal',
  'fee',
];

export function TransactionForm({
  pending = false,
  formId,
  showSubmit = true,
  submitLabel = 'Create',
  onSubmit,
}: Props) {
  const [type, setType] = useState<TransactionType>('buy');
  const [date, setDate] = useState(todayISO());
  const [ticker, setTicker] = useState('');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [fees, setFees] = useState('0');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [tickerResults, setTickerResults] = useState<SymbolSearchResult[]>([]);
  const [tickerSearching, setTickerSearching] = useState(false);
  const [tickerError, setTickerError] = useState<string | null>(null);
  const [selectingTicker, setSelectingTicker] = useState<string | null>(null);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
  const tickerRequestId = useRef(0);

  const securityType = ['buy', 'sell', 'dividend', 'split'].includes(type);
  const tradeType = type === 'buy' || type === 'sell';
  const amountType = ['dividend', 'deposit', 'withdrawal', 'fee'].includes(type);
  const cashType = ['deposit', 'withdrawal', 'fee'].includes(type);

  function numberOrNull(value: string) {
    return value.trim() === '' ? null : Number(value);
  }

  function feeValue() {
    return fees.trim() === '' ? 0 : Number(fees);
  }

  useEffect(() => {
    if (!securityType) {
      setTickerResults([]);
      setTickerError(null);
      return;
    }
    const query = ticker.trim();
    if (query.length === 0) {
      setTickerResults([]);
      setTickerError(null);
      return;
    }
    if (selectedTicker != null && query.toUpperCase() === selectedTicker) {
      setTickerResults([]);
      setTickerError(null);
      setTickerSearching(false);
      return;
    }

    const id = ++tickerRequestId.current;
    setTickerSearching(true);
    const handle = setTimeout(async () => {
      try {
        const matches = await searchInstruments(query);
        if (id === tickerRequestId.current) {
          setTickerResults(matches);
          setTickerError(null);
        }
      } catch {
        if (id === tickerRequestId.current) setTickerError('Search failed.');
      } finally {
        if (id === tickerRequestId.current) setTickerSearching(false);
      }
    }, 250);

    return () => clearTimeout(handle);
  }, [securityType, ticker, selectedTicker]);

  async function selectTicker(result: SymbolSearchResult) {
    tickerRequestId.current++;
    setSelectingTicker(result.symbol);
    setTickerError(null);
    try {
      await addInstrument(result.symbol);
      setSelectedTicker(result.symbol);
      setTicker(result.symbol);
      setCurrency(result.currency);
      setTickerResults([]);
      setTickerSearching(false);
    } catch {
      setTickerError(`Could not select ${result.symbol}.`);
    } finally {
      setSelectingTicker(null);
    }
  }

  return (
    <form
      id={formId}
      className="grid gap-4 md:grid-cols-4"
      data-testid="transaction-form"
      onSubmit={(event) => {
        event.preventDefault();
        void onSubmit({
          date,
          ticker: securityType ? ticker.trim().toUpperCase() : null,
          type,
          quantity: tradeType || type === 'split' ? numberOrNull(quantity) : null,
          price: tradeType ? numberOrNull(price) : null,
          fees: tradeType || type === 'dividend' ? feeValue() : null,
          amount: amountType ? numberOrNull(amount) : null,
          currency: cashType || currency.trim() ? currency.trim().toUpperCase() : null,
        });
      }}
    >
      <div>
        <Label htmlFor="transaction-date">Date</Label>
        <Input
          id="transaction-date"
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          required
        />
      </div>
      <div>
        <Label htmlFor="transaction-type">Type</Label>
        <select
          id="transaction-type"
          value={type}
          onChange={(e) => setType(e.target.value as TransactionType)}
          className="h-10 w-full rounded-md border border-border bg-surface px-3 text-body text-text"
        >
          {TYPES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </div>
      {securityType ? (
        <div className="relative">
          <Label htmlFor="transaction-ticker">Ticker</Label>
          <Input
            id="transaction-ticker"
            type="search"
            value={ticker}
            onChange={(e) => {
              setSelectedTicker(null);
              setTicker(e.target.value);
            }}
            placeholder="Search ticker or company"
            autoComplete="off"
            data-testid="transaction-ticker-search"
            required
          />
          {tickerError ? <p className="mt-1 text-small text-negative">{tickerError}</p> : null}
          {ticker.trim() &&
          selectedTicker == null &&
          !tickerSearching &&
          tickerResults.length === 0 &&
          !tickerError ? (
            <p className="mt-1 text-small text-text-muted">No matches.</p>
          ) : null}
          {tickerResults.length > 0 ? (
            <div className="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-md border border-border bg-surface shadow-card">
              {tickerResults.map((result) => (
                <button
                  key={result.symbol}
                  type="button"
                  onClick={() => void selectTicker(result)}
                  disabled={selectingTicker === result.symbol}
                  data-testid="transaction-ticker-result"
                  className="flex w-full flex-col px-3 py-2 text-left hover:bg-surface-alt disabled:opacity-60"
                >
                  <span className="font-mono text-body font-semibold text-text">
                    {result.symbol}
                  </span>
                  <span className="truncate text-small text-text-muted">
                    {result.name} · {result.exchange} · {result.currency}
                  </span>
                </button>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
      {(tradeType || type === 'split') && (
        <div>
          <Label htmlFor="transaction-quantity">{type === 'split' ? 'Ratio' : 'Quantity'}</Label>
          <Input
            id="transaction-quantity"
            type="number"
            step="any"
            min="0.000001"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            required
          />
        </div>
      )}
      {tradeType && (
        <div>
          <Label htmlFor="transaction-price">Price</Label>
          <Input
            id="transaction-price"
            type="number"
            step="any"
            min="0.0001"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            required
          />
        </div>
      )}
      {(tradeType || type === 'dividend') && (
        <div>
          <Label htmlFor="transaction-fees">Fees</Label>
          <Input
            id="transaction-fees"
            type="number"
            step="any"
            min="0"
            value={fees}
            onChange={(e) => setFees(e.target.value)}
          />
        </div>
      )}
      {amountType && (
        <div>
          <Label htmlFor="transaction-amount">Amount</Label>
          <Input
            id="transaction-amount"
            type="number"
            step="any"
            min="0.0001"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </div>
      )}
      {(cashType || type === 'dividend') && (
        <div>
          <Label htmlFor="transaction-currency">Currency</Label>
          <Input
            id="transaction-currency"
            value={currency}
            maxLength={3}
            onChange={(e) => setCurrency(e.target.value)}
            required={cashType}
          />
        </div>
      )}
      {showSubmit ? (
        <div className="flex items-end">
          <button
            type="submit"
            disabled={pending}
            aria-busy={pending || undefined}
            className="inline-flex h-10 w-full items-center justify-center rounded-md bg-accent px-4 text-body font-medium text-accent-fg transition-all duration-[120ms] ease-out-expo hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-50"
          >
            {pending ? (
              <span
                aria-hidden
                className="inline-block h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent"
              />
            ) : null}
            {submitLabel}
          </button>
        </div>
      ) : null}
    </form>
  );
}
