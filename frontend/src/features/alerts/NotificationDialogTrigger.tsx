import { useCallback, useState } from 'react';
import { BellRing } from 'lucide-react';
import { NotificationDialog } from '@/features/alerts/NotificationDialog';
import { useNotificationsStore } from '@/stores/notificationsStore';

export function NotificationDialogTrigger() {
  const [open, setOpen] = useState(false);
  const unreadCount = useNotificationsStore((state) => state.unreadCount);
  const fetch = useNotificationsStore((state) => state.fetch);

  const handleOpen = useCallback(() => {
    const s = useNotificationsStore.getState();
    if (s.notifications.length === 0 && !s.error && !s.loading) {
      fetch();
    }
    setOpen(true);
  }, [fetch]);

  return (
    <>
      <button
        type="button"
        onClick={handleOpen}
        data-testid="notification-dialog-trigger"
        aria-label="Triggered alerts"
        className="relative inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-surface text-text-muted transition-colors hover:border-border-strong hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
      >
        <BellRing size={16} aria-hidden />
        {unreadCount > 0 ? (
          <span className="absolute -right-1 -top-1 min-w-5 rounded-full border border-bg bg-accent px-1 text-center text-[10px] font-bold leading-5 text-accent-fg">
            {unreadCount}
          </span>
        ) : null}
      </button>
      <NotificationDialog open={open} onClose={() => setOpen(false)} />
    </>
  );
}
