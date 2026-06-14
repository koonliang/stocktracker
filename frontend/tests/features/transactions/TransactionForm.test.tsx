import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TransactionForm } from '@/features/transactions/TransactionForm';
import type { TransactionImportNormalizedRow, TransactionType } from '@/lib/types';

vi.mock('@/api/searchApi', () => ({
  addInstrument: vi.fn(),
  searchInstruments: vi.fn().mockResolvedValue([]),
}));

async function submitForm(
  type: TransactionType,
  fill: (user: ReturnType<typeof userEvent.setup>) => Promise<void>,
) {
  const user = userEvent.setup();
  const onSubmit = vi.fn();
  render(<TransactionForm onSubmit={onSubmit} />);

  await user.clear(screen.getByLabelText('Date'));
  await user.type(screen.getByLabelText('Date'), '2026-06-13');
  await user.selectOptions(screen.getByLabelText('Type'), type);
  await fill(user);
  await user.click(screen.getByRole('button', { name: /add/i }));

  expect(onSubmit).toHaveBeenCalledTimes(1);
  return onSubmit.mock.calls[0]![0] as TransactionImportNormalizedRow;
}

describe('TransactionForm', () => {
  it.each([
    {
      type: 'buy' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Ticker'), 'AAPL');
        await user.type(screen.getByLabelText('Quantity'), '2');
        await user.type(screen.getByLabelText('Price'), '320');
        await user.clear(screen.getByLabelText('Fees'));
        await user.type(screen.getByLabelText('Fees'), '1.4');
      },
      expected: {
        ticker: 'AAPL',
        quantity: 2,
        price: 320,
        fees: 1.4,
        amount: null,
        currency: 'USD',
      },
    },
    {
      type: 'sell' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Ticker'), 'AAPL');
        await user.type(screen.getByLabelText('Quantity'), '1');
        await user.type(screen.getByLabelText('Price'), '325');
      },
      expected: {
        ticker: 'AAPL',
        quantity: 1,
        price: 325,
        fees: 0,
        amount: null,
        currency: 'USD',
      },
    },
    {
      type: 'dividend' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Ticker'), 'AAPL');
        await user.clear(screen.getByLabelText('Fees'));
        await user.type(screen.getByLabelText('Fees'), '0');
        await user.type(screen.getByLabelText('Amount'), '102');
      },
      expected: {
        ticker: 'AAPL',
        quantity: null,
        price: null,
        fees: 0,
        amount: 102,
        currency: 'USD',
      },
    },
    {
      type: 'split' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Ticker'), 'AAPL');
        await user.type(screen.getByLabelText('Ratio'), '2');
      },
      expected: {
        ticker: 'AAPL',
        quantity: 2,
        price: null,
        fees: null,
        amount: null,
        currency: 'USD',
      },
    },
    {
      type: 'deposit' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Amount'), '1000');
      },
      expected: {
        ticker: null,
        quantity: null,
        price: null,
        fees: null,
        amount: 1000,
        currency: 'USD',
      },
    },
    {
      type: 'withdrawal' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Amount'), '250');
      },
      expected: {
        ticker: null,
        quantity: null,
        price: null,
        fees: null,
        amount: 250,
        currency: 'USD',
      },
    },
    {
      type: 'fee' as const,
      fill: async (user: ReturnType<typeof userEvent.setup>) => {
        await user.type(screen.getByLabelText('Amount'), '12');
      },
      expected: {
        ticker: null,
        quantity: null,
        price: null,
        fees: null,
        amount: 12,
        currency: 'USD',
      },
    },
  ])('submits a type-specific $type payload', async ({ type, fill, expected }) => {
    await expect(submitForm(type, fill)).resolves.toMatchObject({
      date: '2026-06-13',
      type,
      ...expected,
    });
  });
});
