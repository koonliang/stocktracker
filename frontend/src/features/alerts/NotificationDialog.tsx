import { useMemo, useState } from 'react';
import { BellRing, RotateCcw } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { mockNotifications, type MockNotification } from './NotificationDialog.mock';
import { NotificationRow } from './NotificationRow';

type Mode = 'filled' | 'empty' | 'error';

type Props = {
  open: boolean;
  onClose: () => void;
};

export function NotificationDialog({ open, onClose }: Props) {
  const [mode, setMode] = useState<Mode>('filled');
  const [notifications, setNotifications] = useState<MockNotification[]>(mockNotifications);
  const unreadCount = useMemo(
    () => notifications.filter((notification) => !notification.read).length,
    [notifications],
  );

  const visibleNotifications = mode === 'empty' ? [] : notifications;

  const reset = () => {
    setMode('filled');
    setNotifications(mockNotifications);
  };

  const markRead = (id: string) => {
    setNotifications((current) =>
      current.map((notification) =>
        notification.id === id ? { ...notification, read: true } : notification,
      ),
    );
  };

  const markAllRead = () => {
    setNotifications((current) => current.map((notification) => ({ ...notification, read: true })));
  };

  const deleteNotification = (id: string) => {
    setNotifications((current) => current.filter((notification) => notification.id !== id));
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Triggered Alerts"
      description="Frontend MVP mockup for alert notification review."
      footer={
        <div className="flex w-full flex-wrap items-center justify-between gap-2">
          <div className="flex gap-2">
            <Button type="button" variant="ghost" size="sm" onClick={() => setMode('filled')}>
              Filled
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={() => setMode('empty')}>
              Empty
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={() => setMode('error')}>
              Error
            </Button>
          </div>
          <Button type="button" variant="secondary" size="sm" onClick={reset}>
            <RotateCcw size={14} aria-hidden />
            Reset mock
          </Button>
        </div>
      }
    >
      <div data-testid="notification-dialog" className="space-y-4">
        <div className="rounded-2xl border border-border bg-surface-alt/60 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="eyebrow">Review queue</p>
              <p className="mt-1 text-body text-text-muted">
                Newest triggered alerts stay here until reviewed or deleted.
              </p>
            </div>
            <Badge
              tone={unreadCount > 0 ? 'accent' : 'neutral'}
              size="md"
              data-testid="notification-unread-count"
            >
              {unreadCount} unread
            </Badge>
          </div>
        </div>

        {mode === 'error' ? (
          <EmptyState
            title="Could not load triggered alerts"
            description="This mock state previews the retry/error treatment before the API is wired."
            actions={
              <Button type="button" variant="secondary" onClick={reset}>
                Retry mock
              </Button>
            }
          />
        ) : visibleNotifications.length === 0 ? (
          <div data-testid="notification-empty">
            <EmptyState
              title="No triggered alerts"
              description="Alerts that cross their threshold will appear here with read and delete actions."
              icon={<BellRing size={22} />}
              className="py-12"
            />
          </div>
        ) : (
          <>
            <div className="flex justify-end">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={markAllRead}
                disabled={unreadCount === 0}
                data-testid="notification-mark-all-read"
              >
                Mark all read
              </Button>
            </div>
            <ul className="max-h-[54vh] space-y-3 overflow-y-auto pr-1">
              {visibleNotifications.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  onMarkRead={markRead}
                  onDelete={deleteNotification}
                />
              ))}
            </ul>
          </>
        )}
      </div>
    </Dialog>
  );
}
