export type MockNotification = {
  id: string;
  alertId: string;
  symbol: string;
  conditionType: 'price_above' | 'price_below' | 'pct_change';
  threshold: number;
  thresholdCurrency: string;
  observedValue: number;
  observedCurrency: string;
  triggeredAt: string;
  read: boolean;
  message: string;
};

export const mockNotifications: MockNotification[] = [
  {
    id: 'mock-1',
    alertId: 'alert-aapl-breakout',
    symbol: 'AAPL',
    conditionType: 'price_above',
    threshold: 300,
    thresholdCurrency: 'USD',
    observedValue: 301.25,
    observedCurrency: 'USD',
    triggeredAt: '2026-06-17T10:42:00Z',
    read: false,
    message: 'AAPL crossed above 300.00 USD',
  },
  {
    id: 'mock-2',
    alertId: 'alert-dbs-dip',
    symbol: 'D05.SI',
    conditionType: 'price_below',
    threshold: 44.5,
    thresholdCurrency: 'SGD',
    observedValue: 44.2,
    observedCurrency: 'SGD',
    triggeredAt: '2026-06-17T09:18:00Z',
    read: false,
    message: 'D05.SI crossed below 44.50 SGD',
  },
  {
    id: 'mock-3',
    alertId: 'alert-msft-move',
    symbol: 'MSFT',
    conditionType: 'pct_change',
    threshold: 3,
    thresholdCurrency: 'USD',
    observedValue: 3.4,
    observedCurrency: 'USD',
    triggeredAt: '2026-06-16T15:05:00Z',
    read: true,
    message: 'MSFT moved by 3.40%',
  },
  {
    id: 'mock-4',
    alertId: 'alert-spy-breakout',
    symbol: 'SPY',
    conditionType: 'price_above',
    threshold: 560,
    thresholdCurrency: 'USD',
    observedValue: 562.1,
    observedCurrency: 'USD',
    triggeredAt: '2026-06-15T14:11:00Z',
    read: true,
    message: 'SPY crossed above 560.00 USD',
  },
];

export const emptyMockNotifications: MockNotification[] = [];
