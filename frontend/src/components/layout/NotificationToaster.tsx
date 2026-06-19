import { useEffect, useRef } from 'react';
import { useNotificationsStore } from '@/stores/notificationsStore';
import { useToastStore, type Toast } from '@/stores/toastStore';
import { cn } from '@/lib/cn';

export function NotificationToaster() {
  const toasts = useToastStore((state) => state.toasts);
  const pushToast = useToastStore((state) => state.pushToast);
  const dismissToast = useToastStore((state) => state.dismissToast);
  const fetch = useNotificationsStore((state) => state.fetch);
  const notifications = useNotificationsStore((state) => state.notifications);
  const seenIds = useRef(new Set(notifications.map((n) => n.id)));

  useEffect(() => {
    let active = true;
    const poll = () => {
      fetch().then(() => {
        if (!active) return;
        const latestNotifications = useNotificationsStore.getState().notifications;
        for (const notification of latestNotifications) {
          if (!notification.read && !seenIds.current.has(notification.id)) {
            seenIds.current.add(notification.id);
            pushToast({
              id: `notification_${notification.id}`,
              tone: 'info',
              title: 'Alert triggered',
              message: notification.message,
            });
          }
        }
      });
    };
    poll();
    const id = window.setInterval(poll, 30_000);
    return () => {
      active = false;
      window.clearInterval(id);
    };
  }, [fetch, pushToast]);

  useEffect(() => {
    if (toasts.length === 0) return;
    const timers = toasts.map((toast) => window.setTimeout(() => dismissToast(toast.id), 6_000));
    return () => {
      timers.forEach(window.clearTimeout);
    };
  }, [dismissToast, toasts]);

  if (toasts.length === 0) return null;

  return (
    <div className="pointer-events-none fixed right-4 top-20 z-[80] flex w-[min(360px,calc(100vw-2rem))] flex-col gap-2">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} onDismiss={() => dismissToast(toast.id)} />
      ))}
    </div>
  );
}

function ToastCard({ toast, onDismiss }: { toast: Toast; onDismiss: () => void }) {
  return (
    <div
      role="alert"
      className={cn(
        'animate-slide-up relative overflow-hidden rounded-xl border p-4 shadow-lg backdrop-blur-md transition-all',
        toast.tone === 'info' && 'border-accent/30 bg-accent/10 text-text',
        toast.tone === 'success' && 'border-success/30 bg-success/10 text-text',
        toast.tone === 'error' && 'border-danger/30 bg-danger/10 text-text',
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <p className="font-medium text-small text-text">{toast.title}</p>
          {toast.message ? <p className="text-small text-text-muted">{toast.message}</p> : null}
        </div>
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Dismiss notification"
          className="pointer-events-auto shrink-0 rounded-md p-1 text-text-muted hover:bg-surface-alt hover:text-text transition-colors"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>
  );
}
