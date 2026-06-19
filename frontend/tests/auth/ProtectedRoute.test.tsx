import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { act, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { useAuthStore } from '@/stores/authStore';
import { renderWithProviders } from '@/test/utils';

function reset() {
  sessionStorage.clear();
  act(() => useAuthStore.getState().clearSession());
}

function renderGuarded() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<div>Login Page</div>} />
      <Route
        path="/secret"
        element={
          <ProtectedRoute>
            <div>Secret Content</div>
          </ProtectedRoute>
        }
      />
    </Routes>,
    { route: '/secret' },
  );
}

describe('ProtectedRoute', () => {
  beforeEach(reset);
  afterEach(reset);

  it('redirects anonymous users to /login', () => {
    renderGuarded();
    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Secret Content')).not.toBeInTheDocument();
  });

  it('renders children when authenticated', () => {
    act(() => useAuthStore.getState().setSession('token-123', { id: 1, email: 'a@b.com' }));
    renderGuarded();
    expect(screen.getByText('Secret Content')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });
});
