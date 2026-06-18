import { create } from 'zustand';
import {
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
} from '@/api/notificationsApi';
import type { Notification, ReadAllResponse } from '@/api/types';

type State = {
  notifications: Notification[];
  unreadCount: number;
  loading: boolean;
  error: string | null;
};

type Actions = {
  fetch: () => Promise<void>;
  markRead: (id: string) => Promise<void>;
  markAllRead: () => Promise<ReadAllResponse>;
  remove: (id: string) => Promise<void>;
  reset: () => void;
};

export const useNotificationsStore = create<State & Actions>()((set, get) => ({
  notifications: [],
  unreadCount: 0,
  loading: false,
  error: null,

  async fetch() {
    set({ loading: true, error: null });
    try {
      const response = await listNotifications();
      set({
        notifications: response.notifications,
        unreadCount: response.unreadCount,
        loading: false,
      });
    } catch {
      set({ error: 'Failed to load notifications', loading: false });
    }
  },

  async markRead(id: string) {
    set({
      notifications: get().notifications.map((n) => (n.id === id ? { ...n, read: true } : n)),
      unreadCount: Math.max(0, get().unreadCount - 1),
    });
    try {
      await markNotificationRead(id);
    } catch {
      // Optimistic update stays visible; next fetch reconciles.
    }
  },

  async markAllRead() {
    set({
      notifications: get().notifications.map((n) => ({ ...n, read: true })),
      unreadCount: 0,
    });
    try {
      return await markAllNotificationsRead();
    } catch {
      return { updated: 0, unreadCount: 0 };
    }
  },

  async remove(id: string) {
    const wasUnread = get().notifications.find((n) => n.id === id)?.read === false;
    set({
      notifications: get().notifications.filter((n) => n.id !== id),
      unreadCount: wasUnread ? Math.max(0, get().unreadCount - 1) : get().unreadCount,
    });
    try {
      await deleteNotification(id);
    } catch {
      // Optimistic update stays visible; next fetch reconciles.
    }
  },

  reset() {
    set({ notifications: [], unreadCount: 0, loading: false, error: null });
  },
}));
