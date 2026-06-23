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
    <div className="pointer-events-none fixed bottom-4 right-4 z-[80] flex w-[min(360px,calc(100vw-2rem))] flex-col gap-2">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} onDismiss={() => dismissToast(toast.id)} />
      ))}
    </div>
  );
}

function ToastIcon({ tone }: { tone: Toast['tone'] }) {
  if (tone === 'success') {
    return (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <polyline points="20 6 9 17 4 12" />
      </svg>
    );
  }
  if (tone === 'error') {
    return (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <line x1="18" y1="6" x2="6" y2="18" />
        <line x1="6" y1="6" x2="18" y2="18" />
      </svg>
    );
  }
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
  );
}

function ToastCard({ toast, onDismiss }: { toast: Toast; onDismiss: () => void }) {
  return (
    <div
      role="alert"
      className={cn(
        'animate-slide-up pointer-events-auto flex items-start gap-3 rounded-xl px-4 py-3 shadow-2xl',
        toast.tone === 'info' && 'bg-[#1e2d4a] text-white',
        toast.tone === 'success' && 'bg-[#1a3a2a] text-white',
        toast.tone === 'error' && 'bg-[#3a1a1a] text-white',
      )}
    >
      <span
        className={cn(
          'mt-0.5 shrink-0',
          toast.tone === 'info' && 'text-blue-300',
          toast.tone === 'success' && 'text-green-400',
          toast.tone === 'error' && 'text-red-400',
        )}
      >
        <ToastIcon tone={toast.tone} />
      </span>
      <div className="min-w-0 flex-1 space-y-0.5">
        <p className="text-sm font-semibold leading-snug">{toast.title}</p>
        {toast.message ? (
          <p className="text-xs text-white/70 leading-snug">{toast.message}</p>
        ) : null}
      </div>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss notification"
        className="shrink-0 rounded p-1 text-white/50 hover:text-white/90 transition-colors"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="13"
          height="13"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
}
