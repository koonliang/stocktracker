import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BaseCurrencySelect } from '@/components/layout/BaseCurrencySelect';

const getBaseCurrencyMock = vi.hoisted(() => vi.fn());
const updateBaseCurrencyMock = vi.hoisted(() => vi.fn());
const loadDashboardMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/settingsApi', () => ({
  getBaseCurrency: getBaseCurrencyMock,
  updateBaseCurrency: updateBaseCurrencyMock,
}));

vi.mock('@/stores/portfolioStore', () => ({
  usePortfolioStore: (selector: (state: { loadDashboard: () => Promise<void> }) => unknown) =>
    selector({ loadDashboard: loadDashboardMock }),
}));

describe('BaseCurrencySelect', () => {
  beforeEach(() => {
    getBaseCurrencyMock.mockReset();
    updateBaseCurrencyMock.mockReset();
    loadDashboardMock.mockReset();
    getBaseCurrencyMock.mockResolvedValue({ baseCurrency: 'USD', supported: ['USD', 'SGD'] });
    updateBaseCurrencyMock.mockResolvedValue({ baseCurrency: 'SGD', supported: ['USD', 'SGD'] });
    loadDashboardMock.mockResolvedValue(undefined);
  });

  it('persists selection and refetches dashboard/performance consumers', async () => {
    const user = userEvent.setup();
    const listener = vi.fn();
    window.addEventListener('stocktracker:base-currency-changed', listener);

    render(<BaseCurrencySelect />);

    await user.selectOptions(await screen.findByLabelText('Base currency'), 'SGD');

    await waitFor(() => expect(updateBaseCurrencyMock).toHaveBeenCalledWith('SGD'));
    expect(loadDashboardMock).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenCalledTimes(1);

    window.removeEventListener('stocktracker:base-currency-changed', listener);
  });
});
