import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ImportPreview } from '@/features/transactions/ImportPreview';
import type { TransactionImportPreviewResponse } from '@/lib/types';

const result: TransactionImportPreviewResponse = {
  validRows: [
    {
      row: 2,
      normalized: {
        date: '2024-01-15',
        ticker: 'AAPL',
        type: 'buy',
        quantity: 10,
        price: 185.25,
        fees: 0,
      },
    },
  ],
  invalidRows: [
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
  detectedVersion: 'v2',
};

describe('ImportPreview', () => {
  it('shows valid and invalid counts', () => {
    render(<ImportPreview result={result} onConfirm={() => {}} onCancel={() => {}} />);
    expect(screen.getByText('1 valid')).toBeInTheDocument();
    expect(screen.getByText('1 invalid')).toBeInTheDocument();
    expect(screen.getByText(/unknown ticker: ZZZZ/i)).toBeInTheDocument();
  });

  it('fires confirm when the confirm button is clicked', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(<ImportPreview result={result} onConfirm={onConfirm} onCancel={() => {}} />);
    await user.click(screen.getByRole('button', { name: /Confirm import/i }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('disables confirm when there are no valid rows', () => {
    render(
      <ImportPreview
        result={{
          validRows: [],
          invalidRows: result.invalidRows,
          headerErrors: [],
          detectedVersion: 'v2',
        }}
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: /Confirm import/i })).toBeDisabled();
  });

  it('renders header errors instead of the table when malformed', () => {
    render(
      <ImportPreview
        result={{
          validRows: [],
          invalidRows: [],
          headerErrors: ['missing required column: date'],
          detectedVersion: 'unknown',
        }}
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByText(/missing required column: date/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Confirm import/i })).not.toBeInTheDocument();
  });
});
