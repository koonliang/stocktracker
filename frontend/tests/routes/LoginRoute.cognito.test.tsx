import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render } from '@/test/utils';

describe('LoginRoute in Cognito mode', () => {
  afterEach(() => {
    vi.doUnmock('@/auth/authConfig');
    vi.resetModules();
    sessionStorage.clear();
  });

  it('redirects to Cognito without rendering the local credential form', async () => {
    const redirectToHostedUi = vi.fn();
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
      redirectToHostedUi,
      redirectToHostedLogout: vi.fn(),
      decodeAuthState: vi.fn(() => ({ from: '/' })),
    }));
    const { AuthProvider } = await import('@/auth/AuthProvider');
    const { LoginRoute } = await import('@/routes/LoginRoute');

    render(
      <MemoryRouter
        initialEntries={[{ pathname: '/login', state: { from: '/watchlists?view=open#row-2' } }]}
      >
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginRoute />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    expect(screen.getByText(/redirecting to sign in/i)).toBeInTheDocument();
    expect(screen.queryByTestId('login-email')).not.toBeInTheDocument();
    expect(screen.queryByTestId('login-password')).not.toBeInTheDocument();
    await waitFor(() =>
      expect(redirectToHostedUi).toHaveBeenCalledWith({ from: '/watchlists?view=open#row-2' }),
    );
  });
});
