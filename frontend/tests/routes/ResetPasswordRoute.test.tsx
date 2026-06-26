import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { ForgotPasswordRoute } from '@/routes/ForgotPasswordRoute';
import { ResetPasswordRoute } from '@/routes/ResetPasswordRoute';
import { renderWithProviders } from '@tests/utils';

describe('ForgotPasswordRoute', () => {
  it('shows a neutral confirmation after submitting', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/forgot-password" element={<ForgotPasswordRoute />} />
      </Routes>,
      { route: '/forgot-password' },
    );

    await user.type(screen.getByTestId('forgot-email'), 'someone@example.com');
    await user.click(screen.getByTestId('forgot-submit'));

    expect(await screen.findByTestId('forgot-confirmation')).toBeInTheDocument();
  });
});

describe('ResetPasswordRoute', () => {
  function renderReset(route: string) {
    return renderWithProviders(
      <Routes>
        <Route path="/reset-password" element={<ResetPasswordRoute />} />
      </Routes>,
      { route },
    );
  }

  it('resets the password with a valid token', async () => {
    const user = userEvent.setup();
    renderReset('/reset-password?token=valid-reset-token');

    await user.type(screen.getByTestId('reset-password'), 'NewPass456!');
    await user.click(screen.getByTestId('reset-submit'));

    expect(await screen.findByTestId('reset-confirmation')).toBeInTheDocument();
  });

  it('shows an error for an invalid token', async () => {
    const user = userEvent.setup();
    renderReset('/reset-password?token=bad-token');

    await user.type(screen.getByTestId('reset-password'), 'NewPass456!');
    await user.click(screen.getByTestId('reset-submit'));

    expect(await screen.findByTestId('reset-error')).toHaveTextContent(/invalid or has expired/i);
    expect(screen.queryByTestId('reset-confirmation')).not.toBeInTheDocument();
  });
});
