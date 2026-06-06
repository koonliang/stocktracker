import { createContext, useContext, useEffect, type ReactNode } from 'react';
import { ApiError } from '@/api/client';
import { fetchMe, login as loginRequest, logout as logoutRequest } from '@/api/authApi';
import { useAuthStore } from '@/stores/authStore';
import { authMode, type AuthMode } from './authConfig';

export type LoginResult = { ok: true } | { ok: false; reason: 'invalid' | 'unverified' | 'server' };

export type SocialProvider = 'google' | 'facebook';

type AuthContextValue = {
  mode: AuthMode;
  login: (email: string, password: string) => Promise<LoginResult>;
  loginWithProvider: (provider: SocialProvider) => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function loginFailure(error: unknown): LoginResult {
  if (error instanceof ApiError) {
    if (error.status === 403) return { ok: false, reason: 'unverified' };
    if (error.status === 401) return { ok: false, reason: 'invalid' };
  }
  return { ok: false, reason: 'server' };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const setSession = useAuthStore((s) => s.setSession);
  const setUser = useAuthStore((s) => s.setUser);
  const clearSession = useAuthStore((s) => s.clearSession);

  // Revalidate a persisted token on load so a stale session can't grant access.
  useEffect(() => {
    const { token, status } = useAuthStore.getState();
    if (authMode === 'dev' && token && status === 'authenticated') {
      fetchMe()
        .then(setUser)
        .catch(() => clearSession());
    }
  }, [setUser, clearSession]);

  async function login(email: string, password: string): Promise<LoginResult> {
    if (authMode === 'cognito') {
      redirectToHostedUi();
      return { ok: true };
    }
    try {
      const { token, user } = await loginRequest(email, password);
      setSession(token, user);
      return { ok: true };
    } catch (error) {
      return loginFailure(error);
    }
  }

  function loginWithProvider(provider: SocialProvider): void {
    if (authMode === 'cognito') {
      redirectToHostedUi(provider);
      return;
    }
    // Social login is delegated to Cognito federation; it is not available in dev mode.
    throw new Error('Social login is only available in cognito mode');
  }

  async function logout(): Promise<void> {
    if (authMode === 'dev') {
      await logoutRequest().catch(() => undefined);
    }
    clearSession();
  }

  return (
    <AuthContext.Provider value={{ mode: authMode, login, loginWithProvider, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// Cognito hosted-UI redirect is wired in User Story 4; dev mode never calls this.
function redirectToHostedUi(_provider?: SocialProvider): void {
  throw new Error('Cognito hosted UI is not configured yet');
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
