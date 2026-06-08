// Selects the client-side auth strategy. `dev` drives the backend /api/auth/*
// flows (used by local dev and the e2e journey); `cognito` uses the hosted UI.
export type AuthMode = 'dev' | 'cognito';

export const authMode: AuthMode = import.meta.env.VITE_AUTH_MODE === 'cognito' ? 'cognito' : 'dev';

export const isCognitoMode = authMode === 'cognito';
export const isDevMode = authMode === 'dev';

// Hosted-UI configuration (cognito mode only). Supplied at build time from the
// Terraform cognito module outputs (contracts/cognito.md).
export const cognitoConfig = {
  domain: import.meta.env.VITE_COGNITO_DOMAIN ?? '',
  clientId: import.meta.env.VITE_COGNITO_CLIENT_ID ?? '',
  // Defaults to the SPA's own /auth/callback path on the current origin.
  redirectUri:
    import.meta.env.VITE_COGNITO_REDIRECT_URI ??
    (typeof window !== 'undefined' ? `${window.location.origin}/auth/callback` : ''),
  scopes: 'openid email profile',
};

