import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '@/test/utils';
import { NotificationDialogTrigger } from '@/features/alerts/NotificationDialogTrigger';

describe('NotificationDialog accessibility', () => {
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
