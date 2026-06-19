import { BellRing } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { useNotificationsStore } from '@/stores/notificationsStore';
import { NotificationRow } from './NotificationRow';

type Props = {
  open: boolean;
  onClose: () => void;
};

export function NotificationDialog({ open, onClose }: Props) {
  const notifications = useNotificationsStore((state) => state.notifications);
  const unreadCount = useNotificationsStore((state) => state.unreadCount);
  const loading = useNotificationsStore((state) => state.loading);
  const error = useNotificationsStore((state) => state.error);
  const fetch = useNotificationsStore((state) => state.fetch);
  const markRead = useNotificationsStore((state) => state.markRead);
  const markAllRead = useNotificationsStore((state) => state.markAllRead);
  const remove = useNotificationsStore((state) => state.remove);

  return (
    <Dialog open={open} onClose={onClose} title="Triggered Alerts">
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

        {error && notifications.length === 0 ? (
          <EmptyState
            title="Could not load triggered alerts"
            actions={
              <Button type="button" variant="secondary" onClick={fetch}>
                Retry
              </Button>
            }
          />
        ) : loading && notifications.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <p className="text-body text-text-muted">Loading...</p>
          </div>
        ) : notifications.length === 0 ? (
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
              {notifications.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  onMarkRead={markRead}
                  onDelete={remove}
                />
              ))}
            </ul>
          </>
        )}
      </div>
    </Dialog>
  );
}
