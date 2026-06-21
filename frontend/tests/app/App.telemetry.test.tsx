import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

async function renderApp({
  analyticsEnabled,
  speedInsightsEnabled,
}: {
  analyticsEnabled: boolean;
  speedInsightsEnabled: boolean;
}) {
  vi.doMock('@vercel/analytics/react', () => ({
    Analytics: () => <div data-testid="vercel-analytics" />,
  }));
  vi.doMock('@vercel/speed-insights/react', () => ({
    SpeedInsights: () => <div data-testid="vercel-speed-insights" />,
  }));
  vi.doMock('@/auth/authConfig', async () => {
    const actual = await vi.importActual<typeof import('@/auth/authConfig')>('@/auth/authConfig');
    return {
      ...actual,
      nonProdAuthConfig: {
        ...actual.nonProdAuthConfig,
        enableVercelAnalytics: analyticsEnabled,
        enableVercelSpeedInsights: speedInsightsEnabled,
      },
    };
  });

  const [{ App }, { AuthProvider }] = await Promise.all([
    import('@/App'),
    import('@/auth/AuthProvider'),
  ]);

  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('App telemetry', () => {
  afterEach(() => {
    vi.doUnmock('@vercel/analytics/react');
    vi.doUnmock('@vercel/speed-insights/react');
    vi.doUnmock('@/auth/authConfig');
    vi.resetModules();
  });

  it('mounts Vercel telemetry components when enabled', async () => {
    await renderApp({ analyticsEnabled: true, speedInsightsEnabled: true });

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByTestId('vercel-analytics')).toBeInTheDocument();
    expect(screen.getByTestId('vercel-speed-insights')).toBeInTheDocument();
  });

  it('keeps the app usable when telemetry is disabled', async () => {
    await renderApp({ analyticsEnabled: false, speedInsightsEnabled: false });

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.queryByTestId('vercel-analytics')).not.toBeInTheDocument();
    expect(screen.queryByTestId('vercel-speed-insights')).not.toBeInTheDocument();
  });
});
