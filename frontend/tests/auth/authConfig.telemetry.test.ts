import { afterEach, describe, expect, it, vi } from 'vitest';

describe('authConfig telemetry gating', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('enables telemetry in dev mode when the flags are true', async () => {
    vi.stubEnv('VITE_AUTH_MODE', 'dev');
    vi.stubEnv('VITE_ENABLE_VERCEL_ANALYTICS', 'true');
    vi.stubEnv('VITE_ENABLE_VERCEL_SPEED_INSIGHTS', 'true');

    const { nonProdAuthConfig } = await import('@/auth/authConfig');

    expect(nonProdAuthConfig.enableVercelAnalytics).toBe(true);
    expect(nonProdAuthConfig.enableVercelSpeedInsights).toBe(true);
  });

  it('disables telemetry in cognito mode even when the flags are true', async () => {
    vi.stubEnv('VITE_AUTH_MODE', 'cognito');
    vi.stubEnv('VITE_ENABLE_VERCEL_ANALYTICS', 'true');
    vi.stubEnv('VITE_ENABLE_VERCEL_SPEED_INSIGHTS', 'true');

    const { nonProdAuthConfig } = await import('@/auth/authConfig');

    expect(nonProdAuthConfig.enableVercelAnalytics).toBe(false);
    expect(nonProdAuthConfig.enableVercelSpeedInsights).toBe(false);
  });
});
