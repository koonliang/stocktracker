import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AlertsRoute } from '@/routes/AlertsRoute';
import { useToastStore } from '@/stores/toastStore';

const listAlertsMock = vi.hoisted(() => vi.fn());
const createAlertMock = vi.hoisted(() => vi.fn());
const updateAlertMock = vi.hoisted(() => vi.fn());
const deleteAlertMock = vi.hoisted(() => vi.fn());
const searchInstrumentsMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/alertsApi', () => ({
  listAlerts: listAlertsMock,
  createAlert: createAlertMock,
  updateAlert: updateAlertMock,
  deleteAlert: deleteAlertMock,
}));

vi.mock('@/api/searchApi', () => ({
  searchInstruments: searchInstrumentsMock,
}));

describe('AlertsRoute', () => {
  beforeEach(() => {
    listAlertsMock.mockReset();
    createAlertMock.mockReset();
    updateAlertMock.mockReset();
    deleteAlertMock.mockReset();
    searchInstrumentsMock.mockReset();
    listAlertsMock.mockResolvedValue({ alerts: [] });
    searchInstrumentsMock.mockResolvedValue([]);
    useToastStore.getState().clearToasts();
  });

  it('renders the Price Alerts page heading', async () => {
    render(<AlertsRoute />);
    expect(
      await screen.findByRole('heading', { name: /Price Alerts/i, level: 1 }),
    ).toBeInTheDocument();
  });

  it('renders the empty state when no alerts exist', async () => {
    render(<AlertsRoute />);
    expect(await screen.findByText(/No alerts yet/i)).toBeInTheDocument();
  });

  it('renders existing alerts', async () => {
    listAlertsMock.mockResolvedValue({
      alerts: [
        {
          id: 'a1',
          symbol: 'AAPL',
          conditionType: 'price_above',
          threshold: 200,
          armed: true,
          createdAt: '2026-01-01T00:00:00.000Z',
        },
      ],
    });
    render(<AlertsRoute />);
    expect(await screen.findByText('AAPL')).toBeInTheDocument();
  });

  it('pushes success toasts for create, update, and delete', async () => {
    const user = userEvent.setup();
    listAlertsMock.mockResolvedValue({
      alerts: [
        {
          id: 'a1',
          symbol: 'AAPL',
          conditionType: 'price_above',
          threshold: 200,
          armed: true,
          createdAt: '2026-01-01T00:00:00.000Z',
        },
      ],
    });
    createAlertMock.mockResolvedValue({ id: 'a2' });
    updateAlertMock.mockResolvedValue({ id: 'a1' });
    deleteAlertMock.mockResolvedValue(undefined);

    render(<AlertsRoute />);
    await screen.findByText('AAPL');

    await user.type(screen.getByTestId('alert-symbol'), 'MSFT');
    await user.clear(screen.getByTestId('alert-threshold'));
    await user.type(screen.getByTestId('alert-threshold'), '250');
    await user.click(screen.getByTestId('alert-submit'));

    expect(useToastStore.getState().toasts).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ title: 'Alert created', tone: 'success' }),
      ]),
    );

    await user.click(screen.getByTestId('alert-edit'));
    await user.clear(screen.getByTestId('alert-threshold'));
    await user.type(screen.getByTestId('alert-threshold'), '210');
    await user.click(screen.getByTestId('alert-submit'));

    expect(useToastStore.getState().toasts).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ title: 'Alert updated', tone: 'success' }),
      ]),
    );

    await user.click(screen.getByTestId('alert-delete'));

    expect(useToastStore.getState().toasts).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ title: 'Alert deleted', tone: 'success' }),
      ]),
    );
  });

  it('hides the create section by default (mobile collapsed)', async () => {
    render(<AlertsRoute />);
    await screen.findByRole('heading', { name: /Price Alerts/i, level: 1 });
    const section = screen.getByTestId('create-alert-section');
    expect(section).toHaveClass('hidden');
  });

  it('reveals the create section when FAB is clicked', async () => {
    const user = userEvent.setup();
    render(<AlertsRoute />);
    await screen.findByRole('heading', { name: /Price Alerts/i, level: 1 });
    await user.click(screen.getByTestId('fab'));
    expect(screen.getByTestId('create-alert-section')).not.toHaveClass('hidden');
  });

  it('renders FAB for creating an alert', async () => {
    render(<AlertsRoute />);
    await screen.findByRole('heading', { name: /Price Alerts/i, level: 1 });
    expect(screen.getByTestId('fab')).toBeInTheDocument();
    expect(screen.getByTestId('fab')).toHaveAttribute('aria-label', 'Create alert');
  });
});
