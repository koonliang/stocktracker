import { describe, it, expect, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/utils';
import { SymbolSearch } from './SymbolSearch';

describe('SymbolSearch', () => {
  it('searches and adds a global symbol', async () => {
    const onAdded = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<SymbolSearch onAdded={onAdded} />);

    await user.type(screen.getByTestId('symbol-search'), 'DBS');

    const result = await screen.findByTestId('symbol-search-result');
    expect(result).toHaveTextContent('D05.SI');

    await user.click(screen.getByTestId('symbol-add'));

    await waitFor(() => expect(onAdded).toHaveBeenCalledWith('D05.SI'));
  });

  it('shows an empty state when nothing matches', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SymbolSearch />);

    await user.type(screen.getByTestId('symbol-search'), 'XYZNOPE');

    expect(await screen.findByText('No matches.')).toBeInTheDocument();
  });
});
