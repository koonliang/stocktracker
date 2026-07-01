import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { LoginRoute } from '@/routes/LoginRoute';
import { useAuthStore } from '@/stores/authStore';
import { renderWithProviders } from '@tests/utils';
import { resetMockApiState, setMockApiState } from '@tests/server';

function reset() {
  sessionStorage.clear();
  resetMockApiState();
  act(() => useAuthStore.getState().clearSession());
}

describe('LoginRoute demo users', () => {
  beforeEach(reset);
  afterEach(reset);

  it('renders existing demo users and signs in with a selected slot', async () => {
    setMockApiState({
      demoUsers: [
        { slot: 1, label: 'Demo User 1', email: 'demo1@stocktracker.local' },
        { slot: 2, label: 'Demo User 2', email: 'demo2@stocktracker.local' },
      ],
    });

    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route path="/" element={<div>Dashboard Home</div>} />
      </Routes>,
      { route: '/login' },
    );

    expect(await screen.findByTestId('demo-user-list')).toHaveTextContent('Demo User 1');
    expect(screen.getByTestId('demo-user-list')).toHaveTextContent('Demo User 2');

    await user.click(screen.getByTestId('demo-user-login-2'));

    expect(await screen.findByText('Dashboard Home')).toBeInTheDocument();
    await waitFor(() => expect(useAuthStore.getState().token).toBe('demo-jwt-2'));
    expect(useAuthStore.getState().user?.email).toBe('demo2@stocktracker.local');
  });

  it('shows the full-capacity state when all demo slots are already used', async () => {
    setMockApiState({
      demoUsers: [
        { slot: 1, label: 'Demo User 1', email: 'demo1@stocktracker.local' },
        { slot: 2, label: 'Demo User 2', email: 'demo2@stocktracker.local' },
        { slot: 3, label: 'Demo User 3', email: 'demo3@stocktracker.local' },
      ],
    });

    renderWithProviders(
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
      </Routes>,
      { route: '/login' },
    );

    expect(await screen.findByTestId('demo-user-create')).toBeDisabled();
    expect(screen.getByTestId('demo-user-create')).toHaveTextContent('All Demo Slots In Use');
  });
});
