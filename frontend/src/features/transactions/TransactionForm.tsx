import { useState } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import type { TransactionImportNormalizedRow, TransactionType } from '@/lib/types';
import { todayISO } from '@/lib/format';

type Props = {
  pending?: boolean;
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

export function TransactionForm({ pending = false, onSubmit }: Props) {
  const [type, setType] = useState<TransactionType>('buy');
  const [date, setDate] = useState(todayISO());
  const [ticker, setTicker] = useState('');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [fees, setFees] = useState('0');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');

  const securityType = ['buy', 'sell', 'dividend', 'split'].includes(type);
  const tradeType = type === 'buy' || type === 'sell';
  const amountType = ['dividend', 'deposit', 'withdrawal', 'fee'].includes(type);
  const cashType = ['deposit', 'withdrawal', 'fee'].includes(type);

  function numberOrZero(value: string) {
    return value.trim() === '' ? 0 : Number(value);
  }

  return (
    <form
      className="grid gap-4 md:grid-cols-4"
      data-testid="transaction-form"
      onSubmit={(event) => {
        event.preventDefault();
        void onSubmit({
          date,
          ticker: securityType ? ticker.trim().toUpperCase() : null,
          type,
          quantity: numberOrZero(quantity),
          price: numberOrZero(price),
          fees: numberOrZero(fees),
          amount: amountType ? numberOrZero(amount) : null,
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
        <div>
          <Label htmlFor="transaction-ticker">Ticker</Label>
          <Input
            id="transaction-ticker"
            value={ticker}
            onChange={(e) => setTicker(e.target.value)}
            required
          />
        </div>
      ) : null}
      {(tradeType || type === 'split') && (
        <div>
          <Label htmlFor="transaction-quantity">{type === 'split' ? 'Ratio' : 'Quantity'}</Label>
          <Input
            id="transaction-quantity"
            type="number"
            step="any"
            min="0"
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
            min="0"
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
            min="0"
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
      <div className="flex items-end">
        <Button type="submit" loading={pending} className="w-full">
          <Plus size={16} aria-hidden />
          Add
        </Button>
      </div>
    </form>
  );
}
