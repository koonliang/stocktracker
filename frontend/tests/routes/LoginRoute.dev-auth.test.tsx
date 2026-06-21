import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { LoginRoute } from '@/routes/LoginRoute';
import { useAuthStore } from '@/stores/authStore';
import { renderWithProviders } from '@/test/utils';

function reset() {
  sessionStorage.clear();
  act(() => useAuthStore.getState().clearSession());
}

describe('LoginRoute dev auth', () => {
  beforeEach(reset);
  afterEach(reset);

  it('shows non-production auth affordances', async () => {
    renderWithProviders(
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
      </Routes>,
      { route: '/login' },
    );

    expect(await screen.findByText(/demo accounts/i)).toBeInTheDocument();
    expect(screen.getByTestId('social-login-google')).toBeInTheDocument();
    expect(screen.getByTestId('social-login-facebook')).toBeInTheDocument();
    expect(screen.getByTestId('demo-user-create')).toBeInTheDocument();
  });

  it('creates a demo user session from the auth hub', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route path="/" element={<div>Dashboard Home</div>} />
      </Routes>,
      { route: '/login' },
    );

    await user.click(await screen.findByTestId('demo-user-create'));

    expect(await screen.findByText('Dashboard Home')).toBeInTheDocument();
    await waitFor(() => expect(useAuthStore.getState().token).toBe('demo-jwt-1'));
  });
});
