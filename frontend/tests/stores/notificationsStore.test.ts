import { afterEach, describe, expect, it } from 'vitest';
import { useNotificationsStore } from '@/stores/notificationsStore';

const mockList = [
  {
    id: 'n1',
    alertId: 'a1',
    symbol: 'AAPL',
    conditionType: 'price_above',
    threshold: 300,
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
    threshold: 300,
    thresholdCurrency: 'USD',
    observedValue: 305.0,
    observedCurrency: 'USD',
    triggeredAt: new Date(Date.now() - 60_000).toISOString(),
    read: true,
    message: 'AAPL crossed above 300.00 USD',
  },
];

function reset() {
  useNotificationsStore.setState({
    notifications: [],
    unreadCount: 0,
    loading: false,
    error: null,
  });
}

describe('notificationsStore', () => {
  afterEach(reset);

  it('markRead optimistically marks a notification read', () => {
    useNotificationsStore.setState({
      notifications: [...mockList],
      unreadCount: 1,
    });
    useNotificationsStore.getState().markRead('n1');
    const state = useNotificationsStore.getState();
    expect(state.notifications.find((n) => n.id === 'n1')?.read).toBe(true);
    expect(state.unreadCount).toBe(0);
  });

  it('markAllRead optimistically marks all notifications read', () => {
    useNotificationsStore.setState({
      notifications: [...mockList],
      unreadCount: 1,
    });
    useNotificationsStore.getState().markAllRead();
    const state = useNotificationsStore.getState();
    expect(state.notifications.every((n) => n.read)).toBe(true);
    expect(state.unreadCount).toBe(0);
  });

  it('remove optimistically removes a notification and decrements unread count', () => {
    useNotificationsStore.setState({
      notifications: [...mockList],
      unreadCount: 1,
    });
    useNotificationsStore.getState().remove('n1');
    const state = useNotificationsStore.getState();
    expect(state.notifications).toHaveLength(1);
    expect(state.unreadCount).toBe(0);
  });

  it('remove does not decrement unread count for read notifications', () => {
    useNotificationsStore.setState({
      notifications: [...mockList],
      unreadCount: 0,
    });
    useNotificationsStore.getState().remove('n2');
    const state = useNotificationsStore.getState();
    expect(state.notifications).toHaveLength(1);
    expect(state.unreadCount).toBe(0);
  });

  it('reset clears all state', () => {
    useNotificationsStore.setState({
      notifications: [...mockList],
      unreadCount: 1,
      loading: true,
      error: 'test',
    });
    useNotificationsStore.getState().reset();
    const state = useNotificationsStore.getState();
    expect(state.notifications).toHaveLength(0);
    expect(state.unreadCount).toBe(0);
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });
});
