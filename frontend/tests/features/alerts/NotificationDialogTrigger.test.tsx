import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, within } from '@/test/utils';
import { NotificationDialogTrigger } from '@/features/alerts/NotificationDialogTrigger';

describe('NotificationDialogTrigger', () => {
  it('opens the mock notification dialog and marks all notifications read', async () => {
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));

    const dialog = await screen.findByTestId('notification-dialog');
    expect(dialog).toHaveTextContent('AAPL crossed above 300.00 USD');
    expect(screen.getByTestId('notification-unread-count')).toHaveTextContent('2 unread');

    await user.click(screen.getByTestId('notification-mark-all-read'));

    expect(screen.getByTestId('notification-unread-count')).toHaveTextContent('0 unread');
    expect(within(dialog).getAllByTestId('notification-row-read-state')[0]).toHaveTextContent(
      'Read',
    );
  });

  it('previews the empty state', async () => {
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));
    await user.click(screen.getByRole('button', { name: 'Empty' }));

    expect(screen.getByTestId('notification-empty')).toHaveTextContent('No triggered alerts');
  });
});
