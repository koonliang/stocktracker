import { create } from 'zustand';

export type ToastTone = 'info' | 'success' | 'error';

export type Toast = {
  id: string;
  title: string;
  message?: string;
  tone: ToastTone;
};

type State = {
  toasts: Toast[];
};

type Actions = {
  pushToast: (toast: Omit<Toast, 'id'> & { id?: string }) => string;
  dismissToast: (id: string) => void;
  clearToasts: () => void;
};

function nextId() {
  return `toast_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export const useToastStore = create<State & Actions>()((set) => ({
  toasts: [],

  pushToast(toast) {
    const id = toast.id ?? nextId();
    set((state) => ({
      toasts: [...state.toasts.filter((item) => item.id !== id), { ...toast, id }].slice(-5),
    }));
    return id;
  },

  dismissToast(id) {
    set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) }));
  },

  clearToasts() {
    set({ toasts: [] });
  },
}));
