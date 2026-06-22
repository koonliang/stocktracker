import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AlertsRoute } from '@/routes/AlertsRoute';

const listAlertsMock = vi.hoisted(() => vi.fn());
const createAlertMock = vi.hoisted(() => vi.fn());
const deleteAlertMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/alertsApi', () => ({
  listAlerts: listAlertsMock,
  createAlert: createAlertMock,
  deleteAlert: deleteAlertMock,
}));

describe('AlertsRoute', () => {
  beforeEach(() => {
    listAlertsMock.mockReset();
    createAlertMock.mockReset();
    deleteAlertMock.mockReset();
    listAlertsMock.mockResolvedValue({ alerts: [] });
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
});
