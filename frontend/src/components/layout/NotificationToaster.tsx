import { useEffect } from 'react';
import { listNotifications, markNotificationRead } from '@/api/notificationsApi';
import { useToastStore, type Toast } from '@/stores/toastStore';
import { cn } from '@/lib/cn';

export function NotificationToaster() {
  const toasts = useToastStore((state) => state.toasts);
  const pushToast = useToastStore((state) => state.pushToast);
  const dismissToast = useToastStore((state) => state.dismissToast);

  useEffect(() => {
    let active = true;
    const poll = () => {
      listNotifications(true)
        .then((response) => {
          if (!active) return;
          response.notifications.forEach((notification) => {
            pushToast({
              id: `notification_${notification.id}`,
              tone: 'info',
              title: 'Alert triggered',
              message: notification.message,
            });
            void markNotificationRead(notification.id);
          });
        })
        .catch(() => {});
    };
    poll();
    const id = window.setInterval(poll, 30_000);
    return () => {
      active = false;
      window.clearInterval(id);
    };
  }, [pushToast]);

  useEffect(() => {
    if (toasts.length === 0) return;
    const timers = toasts.map((toast) => window.setTimeout(() => dismissToast(toast.id), 6_000));
    return () => {
      timers.forEach(window.clearTimeout);
    };
  }, [dismissToast, toasts]);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed right-4 top-4 z-[80] flex w-[min(360px,calc(100vw-2rem))] flex-col gap-2">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} onDismiss={() => dismissToast(toast.id)} />
      ))}
    </div>
  );
}

function ToastCard({ toast, onDismiss }: { toast: Toast; onDismiss: () => void }) {
  return (
    <div
      className={cn(
        'rounded-md border bg-surface p-4 shadow-card',
        toast.tone === 'error' && 'border-negative/40',
        toast.tone === 'success' && 'border-positive/40',
        toast.tone === 'info' && 'border-border',
      )}
      data-testid="notification-toast"
      role={toast.tone === 'error' ? 'alert' : 'status'}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="font-medium text-text">{toast.title}</div>
          {toast.message ? (
            <div className="mt-1 text-body text-text-muted">{toast.message}</div>
          ) : null}
        </div>
        <button
          type="button"
          onClick={onDismiss}
          className="rounded px-1 text-small text-text-muted hover:text-text"
          aria-label="Dismiss notification"
        >
          Dismiss
        </button>
      </div>
    </div>
  );
}
