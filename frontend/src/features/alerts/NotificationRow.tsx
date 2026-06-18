import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { formatCurrencyCode } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { Notification } from '@/api/types';

function conditionLabel(notification: Notification) {
  if (notification.conditionType === 'pct_change') {
    return `Moved by ${notification.threshold.toFixed(2)}%`;
  }
  const direction = notification.conditionType === 'price_above' ? 'Above' : 'Below';
  return `${direction} ${formatCurrencyCode(notification.threshold, notification.thresholdCurrency)}`;
}

function observedLabel(notification: Notification) {
  if (notification.conditionType === 'pct_change') {
    return `${notification.observedValue.toFixed(2)}%`;
  }
  return formatCurrencyCode(notification.observedValue, notification.observedCurrency);
}

function timeLabel(iso: string) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'UTC',
  }).format(new Date(iso));
}

type Props = {
  notification: Notification;
  onMarkRead: (id: string) => void;
  onDelete: (id: string) => void;
};

export function NotificationRow({ notification, onMarkRead, onDelete }: Props) {
  return (
    <li
      data-testid="notification-row"
      className={cn(
        'group relative overflow-hidden rounded-xl border p-4 transition-colors',
        notification.read
          ? 'border-border bg-surface'
          : 'border-accent/40 bg-gradient-to-br from-accent/10 via-surface to-surface',
      )}
    >
      <div className="absolute left-0 top-0 h-full w-1 bg-accent opacity-0 transition-opacity group-hover:opacity-100" />
      <div className="space-y-3">
        <div className="min-w-0 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-display text-title text-text">{notification.symbol}</span>
            <Badge
              tone={notification.read ? 'neutral' : 'accent'}
              data-testid="notification-row-read-state"
            >
              {notification.read ? 'Read' : 'Unread'}
            </Badge>
          </div>
          <p className="text-body text-text">{notification.message}</p>
          <div className="flex flex-wrap gap-x-3 gap-y-1 text-small text-text-muted">
            <span className="whitespace-nowrap">{conditionLabel(notification)}</span>
            <span className="whitespace-nowrap">Observed {observedLabel(notification)}</span>
            <span className="whitespace-nowrap">
              {notification.triggeredAt ? timeLabel(notification.triggeredAt) : '—'}
            </span>
          </div>
        </div>
        <div className="flex shrink-0 gap-2">
          {!notification.read ? (
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => onMarkRead(notification.id)}
              data-testid="notification-mark-read"
            >
              Mark read
            </Button>
          ) : null}
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onDelete(notification.id)}
            data-testid="notification-delete"
          >
            Delete
          </Button>
        </div>
      </div>
    </li>
  );
}
