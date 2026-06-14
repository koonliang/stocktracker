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
});
