import { beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NotificationToaster } from '@/components/layout/NotificationToaster';
import { useToastStore } from '@/stores/toastStore';

describe('NotificationToaster', () => {
  beforeEach(() => {
    useToastStore.getState().clearToasts();
  });

  it('renders local error toasts immediately', () => {
    useToastStore.getState().pushToast({
      tone: 'error',
      title: 'Transaction not saved',
      message: 'sell quantity exceeds held shares',
    });

    render(<NotificationToaster />);

    expect(screen.getByRole('alert')).toHaveTextContent('Transaction not saved');
    expect(screen.getByRole('alert')).toHaveTextContent('sell quantity exceeds held shares');
  });

  it('positions toasts at the top center below the top bar controls', () => {
    useToastStore.getState().pushToast({
      tone: 'info',
      title: 'Alert triggered',
      message: 'AAPL crossed 200',
    });

    const { container } = render(<NotificationToaster />);

    expect(container.firstChild).toHaveClass('top-20');
    expect(container.firstChild).toHaveClass('left-1/2');
    expect(container.firstChild).toHaveClass('-translate-x-1/2');
  });
});
