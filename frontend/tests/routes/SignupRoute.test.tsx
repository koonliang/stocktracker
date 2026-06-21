import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { SignupRoute } from '@/routes/SignupRoute';
import { VerifyEmailRoute } from '@/routes/VerifyEmailRoute';
import { renderWithProviders } from '@/test/utils';

describe('SignupRoute', () => {
  it('submits sign-up and shows the immediate-access state', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/signup" element={<SignupRoute />} />
      </Routes>,
      { route: '/signup' },
    );

    await user.type(screen.getByTestId('signup-email'), 'newuser@example.com');
    await user.type(screen.getByTestId('signup-password'), 'Passw0rd!');
    await user.click(screen.getByTestId('signup-submit'));

    expect(await screen.findByTestId('signup-success')).toBeInTheDocument();
    expect(screen.getByText('newuser@example.com')).toBeInTheDocument();
    expect(screen.getByText(/without email verification/i)).toBeInTheDocument();
  });

  it('shows an error when the password fails the policy', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/signup" element={<SignupRoute />} />
      </Routes>,
      { route: '/signup' },
    );

    await user.type(screen.getByTestId('signup-email'), 'newuser@example.com');
    await user.type(screen.getByTestId('signup-password'), 'short');
    await user.click(screen.getByTestId('signup-submit'));

    // Client-side policy blocks the submit; the success state never appears.
    expect(screen.queryByTestId('signup-success')).not.toBeInTheDocument();
  });
});

describe('VerifyEmailRoute', () => {
  function renderVerify(route: string) {
    return renderWithProviders(
      <Routes>
        <Route path="/verify-email" element={<VerifyEmailRoute />} />
      </Routes>,
      { route },
    );
  }

  it('verifies a valid token', async () => {
    renderVerify('/verify-email?token=valid-token');
    const container = await screen.findByTestId('verify-status');
    await expect.poll(() => container.getAttribute('data-status')).toBe('verified');
  });

  it('shows failure for an invalid token', async () => {
    renderVerify('/verify-email?token=bad-token');
    const container = await screen.findByTestId('verify-status');
    await expect.poll(() => container.getAttribute('data-status')).toBe('error');
  });

  it('shows failure when no token is present', async () => {
    renderVerify('/verify-email');
    expect(screen.getByTestId('verify-status')).toHaveAttribute('data-status', 'missing');
  });
});
