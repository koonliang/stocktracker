import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TransactionsTable } from '@/features/transactions/TransactionsTable';
import type { Transaction } from '@/lib/types';

const transactions: Transaction[] = [
  {
    id: '1',
    date: '2026-06-20',
    ticker: 'AAPL',
    type: 'buy',
    quantity: 2,
    price: 210,
    fees: 1,
    amount: null,
    currency: 'USD',
  },
  {
    id: '2',
    date: '2026-06-21',
    ticker: 'MSFT',
    type: 'buy',
    quantity: 1,
    price: 430,
    fees: 1,
    amount: null,
    currency: 'USD',
  },
];

describe('TransactionsTable', () => {
  it('prompts before bulk deleting selected desktop rows', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn();
    const onDeleteMany = vi.fn();

    render(
      <TransactionsTable
        transactions={transactions}
        onDelete={onDelete}
        onDeleteMany={onDeleteMany}
      />,
    );

    await user.click(screen.getByLabelText('Select transaction 1'));
    await user.click(screen.getByLabelText('Select transaction 2'));
    await user.click(screen.getByRole('button', { name: /Delete selected/i }));

    expect(screen.getByText(/Delete selected transactions\?/i)).toBeInTheDocument();
    expect(screen.getByText(/2 transactions selected\./i)).toBeInTheDocument();

    await user.click(screen.getAllByRole('button', { name: /Delete 2/i })[1]!);

    expect(onDeleteMany).toHaveBeenCalledWith(['1', '2']);
    expect(onDelete).not.toHaveBeenCalled();
  });

  it('supports mobile selection mode before bulk deleting', async () => {
    const user = userEvent.setup();
    const onDeleteMany = vi.fn();

    render(
      <TransactionsTable
        transactions={transactions}
        onDelete={vi.fn()}
        onDeleteMany={onDeleteMany}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Select' }));

    const mobileCheckboxes = screen.getAllByLabelText(/Select transaction /i).slice(0, 2);
    await user.click(mobileCheckboxes[0]!);
    await user.click(mobileCheckboxes[1]!);
    await user.click(screen.getByRole('button', { name: /Delete selected/i }));

    expect(screen.getByText(/Delete selected transactions\?/i)).toBeInTheDocument();

    await user.click(screen.getAllByRole('button', { name: /Delete 2/i })[1]!);

    expect(onDeleteMany).toHaveBeenCalledWith(['2', '1']);
  });
});
