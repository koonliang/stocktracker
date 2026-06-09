import { describe, expect, it } from 'vitest';
import { hostedLogoutUrl } from './authConfig';

describe('authConfig', () => {
  it('builds the Cognito hosted logout URL', () => {
    expect(
      hostedLogoutUrl({
        domain: 'stocktracker.auth.ap-southeast-1.amazoncognito.com',
        clientId: 'client-123',
        redirectUri: 'https://app.example.com/auth/callback',
        logoutUri: 'https://app.example.com/login',
        scopes: 'openid email profile',
      }),
    ).toBe(
      'https://stocktracker.auth.ap-southeast-1.amazoncognito.com/logout?client_id=client-123&logout_uri=https%3A%2F%2Fapp.example.com%2Flogin',
    );
  });
});
