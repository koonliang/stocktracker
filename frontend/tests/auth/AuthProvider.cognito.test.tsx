import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

describe('AuthProvider in Cognito mode', () => {
  afterEach(() => {
    vi.doUnmock('@/auth/authConfig');
    vi.resetModules();
    sessionStorage.clear();
  });

  it('starts hosted logout before clearing the local session', async () => {
    const redirectToHostedLogout = vi.fn();
    vi.doMock('@/auth/authConfig', () => ({
      authMode: 'cognito',
      isCognitoMode: true,
      isDevMode: false,
      cognitoConfig: {
        domain: 'auth.example.com',
        clientId: 'client-123',
        redirectUri: 'https://app.example.com/auth/callback',
        logoutUri: 'https://app.example.com/signed-out',
        scopes: 'openid email profile',
      },
      redirectToHostedUi: vi.fn(),
      redirectToHostedLogout,
      decodeAuthState: vi.fn(() => ({ from: '/' })),
    }));
    const { AuthProvider: CognitoAuthProvider, useAuth: useCognitoAuth } =
      await import('@/auth/AuthProvider');
    const { useAuthStore } = await import('@/stores/authStore');

    function LogoutButton() {
      const { logout } = useCognitoAuth();
      return <button onClick={() => void logout()}>Sign out</button>;
    }

    act(() =>
      useAuthStore.getState().setSession('id-token', { id: 1, email: 'investor@example.com' }),
    );
    render(
      <CognitoAuthProvider>
        <LogoutButton />
      </CognitoAuthProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: /sign out/i }));

    expect(redirectToHostedLogout).toHaveBeenCalledTimes(1);
    expect(useAuthStore.getState().status).toBe('authenticated');
    expect(useAuthStore.getState().token).toBe('id-token');
  });
});

describe('SignedOutRoute', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('clears the local session after Cognito returns from hosted logout', async () => {
    const { SignedOutRoute } = await import('@/routes/SignedOutRoute');
    const { useAuthStore } = await import('@/stores/authStore');
    act(() =>
      useAuthStore.getState().setSession('id-token', { id: 1, email: 'investor@example.com' }),
    );

    render(
      <MemoryRouter initialEntries={['/signed-out']}>
        <Routes>
          <Route path="/signed-out" element={<SignedOutRoute />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /signed out/i })).toBeInTheDocument();
    await waitFor(() => expect(useAuthStore.getState().status).toBe('anonymous'));
    expect(useAuthStore.getState().token).toBeNull();
  });
});
