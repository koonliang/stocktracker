import { axe } from 'vitest-axe';
import { describe, expect, it, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@tests/utils';
import { useNotificationsStore } from '@/stores/notificationsStore';
import { NotificationDialogTrigger } from '@/features/alerts/NotificationDialogTrigger';

describe('NotificationDialog accessibility', () => {
  beforeEach(() => {
    useNotificationsStore.setState({
      notifications: [
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
      ],
      unreadCount: 1,
      loading: false,
      error: null,
    });
  });

  it('has no obvious accessibility violations', async () => {
    const user = userEvent.setup();
    const { container } = renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));

    expect(await axe(container)).toHaveNoViolations();
  });

  it('closes with escape and restores focus to the trigger', async () => {
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    const trigger = screen.getByTestId('notification-dialog-trigger');
    await user.click(trigger);
    expect(await screen.findByRole('dialog')).toBeInTheDocument();

    await user.keyboard('{Escape}');

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });
});
