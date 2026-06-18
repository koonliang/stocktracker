import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, within } from '@/test/utils';
import { NotificationDialogTrigger } from '@/features/alerts/NotificationDialogTrigger';

describe('NotificationDialog', () => {
  it('shows filled notification rows, unread count, and row actions', async () => {
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

  it('previews empty and error states', async () => {
    const user = userEvent.setup();
    renderWithProviders(<NotificationDialogTrigger />);

    await user.click(screen.getByTestId('notification-dialog-trigger'));
    await user.click(screen.getByRole('button', { name: 'Empty' }));
    expect(screen.getByTestId('notification-empty')).toHaveTextContent('No triggered alerts');

    await user.click(screen.getByRole('button', { name: 'Error' }));
    expect(screen.getByText('Could not load triggered alerts')).toBeInTheDocument();
  });
});
