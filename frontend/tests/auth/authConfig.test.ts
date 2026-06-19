import { describe, expect, it } from 'vitest';
import { decodeAuthState, encodeAuthState, hostedLogoutUrl } from '@/auth/authConfig';

describe('authConfig', () => {
  it('builds the Cognito hosted logout URL', () => {
    expect(
      hostedLogoutUrl({
        domain: 'stocktracker.auth.ap-southeast-1.amazoncognito.com',
        clientId: 'client-123',
        redirectUri: 'https://app.example.com/auth/callback',
        logoutUri: 'https://app.example.com/signed-out',
        scopes: 'openid email profile',
      }),
    ).toBe(
      'https://stocktracker.auth.ap-southeast-1.amazoncognito.com/logout?client_id=client-123&logout_uri=https%3A%2F%2Fapp.example.com%2Fsigned-out',
    );
  });

  it('round-trips safe Cognito state return paths', () => {
    const state = encodeAuthState('/watchlists?tab=active#top');
    expect(decodeAuthState(state)).toEqual({ from: '/watchlists?tab=active#top' });
  });

  it('falls back when Cognito state contains an unsafe return path', () => {
    const state = window.btoa(JSON.stringify({ from: 'https://evil.example.com' }));
    expect(decodeAuthState(state)).toEqual({ from: '/' });
    expect(decodeAuthState('not-base64')).toEqual({ from: '/' });
  });
});
