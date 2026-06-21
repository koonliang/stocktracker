import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

describe('AuthProvider in dev mode', () => {
  afterEach(() => {
    vi.doUnmock('@/auth/authConfig');
    vi.resetModules();
    sessionStorage.clear();
  });

  it('starts the configured non-production social flow', async () => {
    const redirectToNonProdProvider = vi.fn();
    vi.doMock('@/auth/authConfig', async () => {
      const actual = await vi.importActual<typeof import('@/auth/authConfig')>('@/auth/authConfig');
      return {
        ...actual,
        authMode: 'dev',
        isDevMode: true,
        isCognitoMode: false,
        redirectToNonProdProvider,
      };
    });
    const { AuthProvider, useAuth } = await import('@/auth/AuthProvider');

    function SocialButton() {
      const { loginWithProvider } = useAuth();
      return <button onClick={() => loginWithProvider('google', '/watchlists')}>Continue</button>;
    }

    render(
      <AuthProvider>
        <SocialButton />
      </AuthProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Continue' }));

    expect(redirectToNonProdProvider).toHaveBeenCalledWith('google', '/watchlists');
  });
});
