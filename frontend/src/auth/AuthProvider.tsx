import { createContext, useContext, useEffect, type ReactNode } from 'react';
import { ApiError, setAuthToken } from '@/api/client';
import {
  createDemoUser,
  exchangeSocialCode,
  fetchMe,
  listDemoUsers,
  login as loginRequest,
  loginDemoUser,
  logout as logoutRequest,
} from '@/api/authApi';
import type { DemoUserCatalog } from '@/api/types';
import { useAuthStore } from '@/stores/authStore';
import {
  authMode,
  cognitoConfig,
  decodeAuthState,
  isDevMode,
  nonProdAuthConfig,
  redirectToNonProdProvider,
  redirectToHostedLogout,
  redirectToHostedUi,
  type AuthMode,
} from './authConfig';

export type LoginResult = { ok: true } | { ok: false; reason: 'invalid' | 'unverified' | 'server' };

export type SocialProvider = 'google' | 'facebook';

type AuthContextValue = {
  mode: AuthMode;
  login: (email: string, password: string) => Promise<LoginResult>;
  loginWithProvider: (provider: SocialProvider, from?: string) => void;
  completeDevSocialCallback: () => Promise<string>;
  fetchDemoUsers: () => Promise<DemoUserCatalog>;
  createDemoUserSession: () => Promise<void>;
  loginAsDemoUser: (slot: number) => Promise<void>;
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
  // Works in both modes: the bearer token is the dev RS256 JWT or the Cognito
  // id_token, and /api/auth/me validates either. The hosted-UI auth-code
  // exchange itself is handled by the dedicated /auth/callback route.
  useEffect(() => {
    const { token, status } = useAuthStore.getState();
    if (token && status === 'authenticated') {
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

  function loginWithProvider(provider: SocialProvider, from = '/'): void {
    if (authMode === 'cognito') {
      redirectToHostedUi({ provider });
      return;
    }
    redirectToNonProdProvider(provider, from);
  }

  async function completeDevSocialCallback(): Promise<string> {
    const url = new URL(window.location.href);
    const code = url.searchParams.get('code');
    const { from, provider } = decodeAuthState(url.searchParams.get('state'));
    if (!code || !provider) {
      throw new Error('Missing provider callback context');
    }
    url.searchParams.delete('code');
    url.searchParams.delete('state');
    window.history.replaceState({}, '', url.toString());
    const { token, user } = await exchangeSocialCode(provider, {
      code,
      redirectUri: nonProdAuthConfig.redirectUri,
    });
    setSession(token, user);
    return from;
  }

  async function fetchDemoUsers(): Promise<DemoUserCatalog> {
    return listDemoUsers();
  }

  async function createDemoUserSession(): Promise<void> {
    const { token, user } = await createDemoUser();
    setSession(token, user);
  }

  async function loginAsDemoUser(slot: number): Promise<void> {
    const { token, user } = await loginDemoUser(slot);
    setSession(token, user);
  }

  async function logout(): Promise<void> {
    if (authMode === 'dev') {
      await logoutRequest().catch(() => undefined);
      clearSession();
      return;
    }
    if (authMode === 'cognito') {
      redirectToHostedLogout();
    }
  }

  return (
    <AuthContext.Provider
      value={{
        mode: authMode,
        login,
        loginWithProvider,
        completeDevSocialCallback,
        fetchDemoUsers,
        createDemoUserSession,
        loginAsDemoUser,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

/**
 * On return from the Hosted UI (`?code=...`), exchanges the auth code for tokens at the Cognito
 * token endpoint, then bootstraps the local session via the JIT-provisioned `/api/auth/me`.
 */
export async function completeCognitoCallback(
  setSession: (token: string, user: { id: number; email: string }) => void,
): Promise<string> {
  const url = new URL(window.location.href);
  const code = url.searchParams.get('code');
  const { from } = decodeAuthState(url.searchParams.get('state'));
  if (!code) {
    return from;
  }
  // Auth codes are single-use — strip it from the URL before exchanging so a remount
  // (e.g. StrictMode) or refresh can't replay the same code and fail.
  url.searchParams.delete('code');
  url.searchParams.delete('state');
  window.history.replaceState({}, '', url.toString());
  const { domain, clientId, redirectUri } = cognitoConfig;
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: clientId,
    code,
    redirect_uri: redirectUri,
  });
  const response = await fetch(`https://${domain}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });
  if (!response.ok) {
    throw new Error('Token exchange failed');
  }
  const tokens = (await response.json()) as { id_token: string };
  // The id_token carries email + identities claims that CurrentUser resolves/links.
  setAuthToken(tokens.id_token);
  const user = await fetchMe();
  setSession(tokens.id_token, user);
  return from;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}

export function shouldUseDevCallback(): boolean {
  return isDevMode;
}
