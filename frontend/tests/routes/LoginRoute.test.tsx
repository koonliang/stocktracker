import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { LoginRoute } from '@/routes/LoginRoute';
import { useAuthStore } from '@/stores/authStore';
import { renderWithProviders } from '@tests/utils';

function reset() {
  sessionStorage.clear();
  act(() => useAuthStore.getState().clearSession());
}

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginRoute />} />
      <Route path="/" element={<div>Dashboard Home</div>} />
    </Routes>,
    { route: '/login' },
  );
}

describe('LoginRoute', () => {
  beforeEach(reset);
  afterEach(reset);

  it('signs in, stores the session, and redirects', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByTestId('login-email'), 'investor@example.com');
    await user.type(screen.getByTestId('login-password'), 'Passw0rd!');
    await user.click(screen.getByTestId('login-submit'));

    expect(await screen.findByText('Dashboard Home')).toBeInTheDocument();
    await waitFor(() => expect(useAuthStore.getState().token).toBe('mock-jwt'));
    expect(useAuthStore.getState().user?.email).toBe('investor@example.com');
  });

  it('shows a generic error on invalid credentials', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByTestId('login-email'), 'investor@example.com');
    await user.type(screen.getByTestId('login-password'), 'wrong-password');
    await user.click(screen.getByTestId('login-submit'));

    expect(await screen.findByTestId('login-error')).toHaveTextContent(
      /invalid email or password/i,
    );
    expect(useAuthStore.getState().status).toBe('anonymous');
  });
});
