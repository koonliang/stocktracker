import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ExportButton } from '@/features/transactions/ExportButton';
import type { Transaction } from '@/lib/types';

const tx: Transaction = {
  id: '1',
  date: '2024-01-15',
  ticker: 'AAPL',
  type: 'buy',
  quantity: 10,
  price: 185.25,
  fees: 0,
};

describe('ExportButton', () => {
  it('emits a CSV with the canonical header and LF endings on click', async () => {
    const user = userEvent.setup();
    const onExport = vi.fn();
    render(<ExportButton transactions={[tx]} onExport={onExport} />);
    await user.click(screen.getByRole('button', { name: /Export CSV/i }));
    expect(onExport).toHaveBeenCalledOnce();
    const [csv, filename] = onExport.mock.calls[0]!;
    expect(csv.split('\n')[0]).toBe('date,ticker,type,quantity,price,fees');
    expect(csv).not.toContain('\r');
    expect(filename).toMatch(/^stocktracker-transactions-\d{8}\.csv$/);
  });

  it('is disabled when there are no transactions', () => {
    render(<ExportButton transactions={[]} />);
    expect(screen.getByRole('button', { name: /Export CSV/i })).toBeDisabled();
  });
});
