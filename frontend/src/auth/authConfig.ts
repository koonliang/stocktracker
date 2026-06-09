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

// Hosted-UI page per flow. Login/signup/reset are distinct Cognito pages; social
// federation always goes through the authorize endpoint regardless of flow.
const HOSTED_UI_ENDPOINT = {
  login: 'oauth2/authorize',
  signup: 'signup',
  reset: 'forgotPassword',
} as const;

/**
 * Sends the browser to the Cognito Hosted UI (cognito mode only). Email flows land on the
 * login/signup/reset page; passing a provider starts Google/Facebook federation instead.
 */
export function redirectToHostedUi(
  opts: { provider?: 'google' | 'facebook'; flow?: keyof typeof HOSTED_UI_ENDPOINT } = {},
): void {
  const { domain, clientId, redirectUri, scopes } = cognitoConfig;
  const params = new URLSearchParams({
    client_id: clientId,
    response_type: 'code',
    scope: scopes,
    redirect_uri: redirectUri,
  });
  if (opts.provider) {
    params.set('identity_provider', opts.provider === 'google' ? 'Google' : 'Facebook');
  }
  const endpoint = opts.provider ? 'oauth2/authorize' : HOSTED_UI_ENDPOINT[opts.flow ?? 'login'];
  window.location.assign(`https://${domain}/${endpoint}?${params.toString()}`);
}
