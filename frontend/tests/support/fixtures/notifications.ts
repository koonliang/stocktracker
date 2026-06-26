type NotificationItem = {
  id: string;
  alertId: string;
  symbol: string;
  conditionType: string;
  threshold: number;
  thresholdCurrency: string;
  observedValue: number;
  observedCurrency: string;
  triggeredAt: string;
  read: boolean;
  message: string;
};

let nextId = 1;

export function resetNotificationIds() {
  nextId = 1;
}

export function buildNotification(overrides: Partial<NotificationItem> = {}): NotificationItem {
  const id = overrides.id ?? `n${nextId++}`;
  return {
    id,
    alertId: `a${nextId}`,
    symbol: 'AAPL',
    conditionType: 'price_above',
    threshold: 300.0,
    thresholdCurrency: 'USD',
    observedValue: 301.25,
    observedCurrency: 'USD',
    triggeredAt: new Date().toISOString(),
    read: false,
    message: 'AAPL crossed above 300.00 USD',
    ...overrides,
  };
}

export function buildUnreadNotification(
  overrides: Partial<NotificationItem> = {},
): NotificationItem {
  return buildNotification({ ...overrides, read: false });
}

export function buildReadNotification(overrides: Partial<NotificationItem> = {}): NotificationItem {
  return buildNotification({ ...overrides, read: true });
}

export function buildNotificationList(
  count: number,
  overrides: Partial<NotificationItem> = {},
): NotificationItem[] {
  return Array.from({ length: count }, (_, i) =>
    buildNotification({ ...overrides, id: `n${i + 1}` }),
  );
}

export function buildMixedNotificationList(
  unreadCount: number,
  readCount: number,
): NotificationItem[] {
  return [
    ...buildNotificationList(unreadCount, { read: false }),
    ...buildNotificationList(readCount, { read: true }),
  ];
}
