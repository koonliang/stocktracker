import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ImportPreview } from '@/features/transactions/ImportPreview';
import type { ParseResult } from '@/lib/csv';
import type { Transaction } from '@/lib/types';

const validTx: Transaction = {
  id: 'tx_1',
  date: '2024-01-15',
  ticker: 'AAPL',
  type: 'buy',
  quantity: 10,
  price: 185.25,
  fees: 0,
};

const result: ParseResult = {
  valid: [validTx],
  invalid: [
    {
      row: 3,
      reason: 'unknown ticker: ZZZZ',
      raw: {
        date: '2024-02-01',
        ticker: 'ZZZZ',
        type: 'buy',
        quantity: '5',
        price: '100',
        fees: '0',
      },
    },
  ],
  headerErrors: [],
};

describe('ImportPreview', () => {
  it('shows valid/invalid counts and renders the reason for invalid rows', () => {
    render(<ImportPreview result={result} onConfirm={() => {}} onCancel={() => {}} />);
    expect(screen.getByText('1 valid')).toBeInTheDocument();
    expect(screen.getByText('1 invalid')).toBeInTheDocument();
    expect(screen.getByText(/unknown ticker: ZZZZ/i)).toBeInTheDocument();
  });

  it('confirms with only the valid rows when the confirm button is clicked', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(<ImportPreview result={result} onConfirm={onConfirm} onCancel={() => {}} />);
    await user.click(screen.getByRole('button', { name: /Confirm import/i }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('disables the confirm button when there are no valid rows', () => {
    const empty: ParseResult = { valid: [], invalid: result.invalid, headerErrors: [] };
    render(<ImportPreview result={empty} onConfirm={() => {}} onCancel={() => {}} />);
    expect(screen.getByRole('button', { name: /Confirm import/i })).toBeDisabled();
  });

  it('renders header errors instead of the table when the file is malformed', () => {
    const broken: ParseResult = {
      valid: [],
      invalid: [],
      headerErrors: ['missing required column: date'],
    };
    render(<ImportPreview result={broken} onConfirm={() => {}} onCancel={() => {}} />);
    expect(screen.getByText(/missing required column: date/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Confirm import/i })).not.toBeInTheDocument();
  });
});
