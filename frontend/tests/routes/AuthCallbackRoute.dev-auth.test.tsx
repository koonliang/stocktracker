import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render } from '@tests/utils';

describe('AuthCallbackRoute in dev mode', () => {
  afterEach(() => {
    vi.doUnmock('@/auth/AuthProvider');
    vi.resetModules();
    sessionStorage.clear();
  });

  it('navigates to the requested route after non-production social exchange', async () => {
    vi.doMock('@/auth/AuthProvider', async () => {
      const actual =
        await vi.importActual<typeof import('@/auth/AuthProvider')>('@/auth/AuthProvider');
      return {
        ...actual,
        shouldUseDevCallback: vi.fn(() => true),
        useAuth: vi.fn(() => ({
          completeDevSocialCallback: vi.fn().mockResolvedValue('/watchlists'),
        })),
      };
    });
    const { AuthCallbackRoute } = await import('@/routes/AuthCallbackRoute');

    const state = window.btoa(JSON.stringify({ from: '/watchlists', provider: 'google' }));
    render(
      <MemoryRouter initialEntries={[`/auth/callback?code=dev-code&state=${state}`]}>
        <Routes>
          <Route path="/auth/callback" element={<AuthCallbackRoute />} />
          <Route path="/watchlists" element={<div>Watchlists Page</div>} />
          <Route path="/" element={<div>Dashboard Home</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText('Watchlists Page')).toBeInTheDocument();
  });
});
