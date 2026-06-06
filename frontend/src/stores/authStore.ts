import { create } from 'zustand';
import { setAuthToken, setUnauthorizedHandler } from '@/api/client';
import type { AuthUser } from '@/api/types';

export type AuthStatus = 'anonymous' | 'authenticated' | 'loading';

const TOKEN_KEY = 'st_auth_token';
const USER_KEY = 'st_auth_user';

type State = {
  token: string | null;
  user: AuthUser | null;
  status: AuthStatus;
};

type Actions = {
  setSession: (token: string, user: AuthUser) => void;
  setUser: (user: AuthUser) => void;
  setStatus: (status: AuthStatus) => void;
  clearSession: () => void;
};

function readStoredUser(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

const initialToken = sessionStorage.getItem(TOKEN_KEY);
const initialUser = readStoredUser();
// Mirror any persisted token into the API client so the first request is authorized.
setAuthToken(initialToken);

export const useAuthStore = create<State & Actions>()((set) => ({
  token: initialToken,
  user: initialUser,
  status: initialToken && initialUser ? 'authenticated' : 'anonymous',

  setSession(token, user) {
    sessionStorage.setItem(TOKEN_KEY, token);
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    setAuthToken(token);
    set({ token, user, status: 'authenticated' });
  },

  setUser(user) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    set({ user });
  },

  setStatus(status) {
    set({ status });
  },

  clearSession() {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    setAuthToken(null);
    set({ token: null, user: null, status: 'anonymous' });
  },
}));

// A 401 on any protected request clears the session; ProtectedRoute then redirects.
setUnauthorizedHandler(() => useAuthStore.getState().clearSession());
