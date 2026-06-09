import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render } from '@/test/utils';

describe('AuthCallbackRoute', () => {
  afterEach(() => {
    vi.doUnmock('@/auth/AuthProvider');
    vi.resetModules();
    sessionStorage.clear();
  });

  it('navigates to the Cognito callback return path', async () => {
    vi.doMock('@/auth/AuthProvider', () => ({
      completeCognitoCallback: vi.fn().mockResolvedValue('/watchlists'),
    }));
    const { AuthCallbackRoute } = await import('@/routes/AuthCallbackRoute');

    render(
      <MemoryRouter initialEntries={['/auth/callback?code=abc&state=state-123']}>
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
