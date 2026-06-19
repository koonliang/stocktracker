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
];

function seedStore() {
  useNotificationsStore.setState({
    notifications: mockNotifications,
    unreadCount: 1,
    loading: false,
    error: null,
  });
}

describe('NotificationDialogTrigger', () => {
  beforeEach(() => {
    useNotificationsStore.setState({
      notifications: [],
      unreadCount: 0,
      loading: true,
      error: null,
    });
  });

  it('opens the notification dialog and marks all notifications read', async () => {
    seedStore();
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));

    const dialog = await screen.findByTestId('notification-dialog');
    expect(dialog).toHaveTextContent('AAPL crossed above 300.00 USD');
    expect(screen.getByTestId('notification-unread-count')).toHaveTextContent('1 unread');

    await user.click(screen.getByTestId('notification-mark-all-read'));

    expect(screen.getByTestId('notification-unread-count')).toHaveTextContent('0 unread');
    expect(within(dialog).getAllByTestId('notification-row-read-state')[0]).toHaveTextContent(
      'Read',
    );
  });

  it('shows the empty state', async () => {
    useNotificationsStore.setState({
      notifications: [],
      unreadCount: 0,
      loading: false,
      error: null,
    });
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));
    expect(screen.getByTestId('notification-empty')).toHaveTextContent('No triggered alerts');
  });
});
