import { describe, expect, it, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, within } from '@/test/utils';
import { useNotificationsStore } from '@/stores/notificationsStore';
import { NotificationDialogTrigger } from '@/features/alerts/NotificationDialogTrigger';

const mockNotifications = [
  {
    id: 'n1',
    alertId: 'a1',
    symbol: 'AAPL',
    conditionType: 'price_above',
    threshold: 300.0,
    thresholdCurrency: 'USD',
    observedValue: 301.25,
    observedCurrency: 'USD',
    triggeredAt: new Date().toISOString(),
    read: false,
    message: 'AAPL crossed above 300.00 USD',
  },
  {
    id: 'n2',
    alertId: 'a1',
    symbol: 'AAPL',
    conditionType: 'price_above',
    threshold: 300.0,
    thresholdCurrency: 'USD',
    observedValue: 301.25,
    observedCurrency: 'USD',
    triggeredAt: new Date(Date.now() - 60_000).toISOString(),
    read: true,
    message: 'AAPL crossed above 300.00 USD',
  },
  {
    id: 'n3',
    alertId: 'a2',
    symbol: 'D05.SI',
    conditionType: 'price_below',
    threshold: 44.5,
    thresholdCurrency: 'SGD',
    observedValue: 44.1,
    observedCurrency: 'SGD',
    triggeredAt: new Date(Date.now() - 120_000).toISOString(),
    read: false,
    message: 'D05.SI crossed below 44.50 SGD',
  },
  {
    id: 'n4',
    alertId: 'a2',
    symbol: 'D05.SI',
    conditionType: 'price_below',
    threshold: 44.5,
    thresholdCurrency: 'SGD',
    observedValue: 44.0,
    observedCurrency: 'SGD',
    triggeredAt: new Date(Date.now() - 180_000).toISOString(),
    read: true,
    message: 'D05.SI crossed below 44.50 SGD',
  },
];

function seedStore() {
  useNotificationsStore.setState({
    notifications: mockNotifications,
    unreadCount: 2,
    loading: false,
    error: null,
  });
}

function seedEmpty() {
  useNotificationsStore.setState({
    notifications: [],
    unreadCount: 0,
    loading: false,
    error: null,
  });
}

function seedError() {
  useNotificationsStore.setState({
    notifications: [],
    unreadCount: 0,
    loading: false,
    error: 'Failed to load notifications',
  });
}

describe('NotificationDialog', () => {
  beforeEach(() => {
    useNotificationsStore.setState({
      notifications: [],
      unreadCount: 0,
      loading: true,
      error: null,
    });
  });

  it('shows notification rows, unread count, and row actions', async () => {
    seedStore();
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));

    const dialog = await screen.findByTestId('notification-dialog');
    expect(dialog).toHaveTextContent('AAPL crossed above 300.00 USD');
    expect(dialog).toHaveTextContent('D05.SI crossed below 44.50 SGD');
    expect(screen.getByTestId('notification-unread-count')).toHaveTextContent('2 unread');
    expect(within(dialog).getAllByTestId('notification-row')).toHaveLength(4);
    expect(within(dialog).getAllByTestId('notification-delete')[0]).toBeVisible();
  });

  it('marks all notifications read', async () => {
    seedStore();
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));
    await user.click(screen.getByTestId('notification-mark-all-read'));

    const dialog = screen.getByTestId('notification-dialog');
    expect(screen.getByTestId('notification-unread-count')).toHaveTextContent('0 unread');
    expect(within(dialog).getAllByTestId('notification-row-read-state')[0]).toHaveTextContent(
      'Read',
    );
  });

  it('shows empty state when no notifications', async () => {
    seedEmpty();
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));
    expect(screen.getByTestId('notification-empty')).toHaveTextContent('No triggered alerts');
  });

  it('shows error state', async () => {
    seedError();
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));
    expect(screen.getByText('Could not load triggered alerts')).toBeInTheDocument();
  });
});
