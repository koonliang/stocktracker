// Selects the client-side auth strategy. `dev` drives the backend /api/auth/*
// flows (used by local dev and the e2e journey); `cognito` uses the hosted UI.
export type AuthMode = 'dev' | 'cognito';

export const authMode: AuthMode = import.meta.env.VITE_AUTH_MODE === 'cognito' ? 'cognito' : 'dev';

export const isCognitoMode = authMode === 'cognito';
export const isDevMode = authMode === 'dev';

export const nonProdAuthConfig = {
  googleAuthUrl: import.meta.env.VITE_NONPROD_GOOGLE_AUTH_URL ?? '',
  facebookAuthUrl: import.meta.env.VITE_NONPROD_FACEBOOK_AUTH_URL ?? '',
  redirectUri:
    import.meta.env.VITE_NONPROD_SOCIAL_REDIRECT_URI ??
    (typeof window !== 'undefined' ? `${window.location.origin}/auth/callback` : ''),
  enableVercelAnalytics: isDevMode && import.meta.env.VITE_ENABLE_VERCEL_ANALYTICS === 'true',
  enableVercelSpeedInsights:
    isDevMode && import.meta.env.VITE_ENABLE_VERCEL_SPEED_INSIGHTS === 'true',
};

// Hosted-UI configuration (cognito mode only). Supplied at build time from the
// Terraform cognito module outputs (contracts/cognito.md).
export const cognitoConfig = {
  domain: import.meta.env.VITE_COGNITO_DOMAIN ?? '',
  clientId: import.meta.env.VITE_COGNITO_CLIENT_ID ?? '',
  // Defaults to the SPA's own /auth/callback path on the current origin.
  redirectUri:
    import.meta.env.VITE_COGNITO_REDIRECT_URI ??
    (typeof window !== 'undefined' ? `${window.location.origin}/auth/callback` : ''),
  // Cognito redirects here after clearing the hosted-UI/browser session.
  logoutUri:
    import.meta.env.VITE_COGNITO_LOGOUT_URI ??
    (typeof window !== 'undefined' ? `${window.location.origin}/signed-out` : ''),
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
  opts: {
    provider?: 'google' | 'facebook';
    flow?: keyof typeof HOSTED_UI_ENDPOINT;
    from?: string;
  } = {},
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
  if (opts.from) {
    params.set('state', encodeAuthState(opts.from));
  }
  const endpoint = opts.provider ? 'oauth2/authorize' : HOSTED_UI_ENDPOINT[opts.flow ?? 'login'];
  window.location.assign(`https://${domain}/${endpoint}?${params.toString()}`);
}

export function encodeAuthState(from: string): string {
  return window.btoa(JSON.stringify({ from: sanitizeReturnPath(from) }));
}

export function encodeProviderState(from: string, provider: 'google' | 'facebook'): string {
  return window.btoa(JSON.stringify({ from: sanitizeReturnPath(from), provider }));
}

export function decodeAuthState(state: string | null): {
  from: string;
  provider?: 'google' | 'facebook';
} {
  if (!state) {
    return { from: '/' };
  }
  try {
    const parsed = JSON.parse(window.atob(state)) as { from?: unknown; provider?: unknown };
    return {
      from: sanitizeReturnPath(typeof parsed.from === 'string' ? parsed.from : '/'),
      provider:
        parsed.provider === 'google' || parsed.provider === 'facebook'
          ? parsed.provider
          : undefined,
    };
  } catch {
    return { from: '/' };
  }
}

export function sanitizeReturnPath(path: string): string {
  if (!path.startsWith('/') || path.startsWith('//')) {
    return '/';
  }
  return path;
}

/** Clears Cognito's hosted-UI session, then returns the browser to the signed-out route. */
export function hostedLogoutUrl(config: typeof cognitoConfig = cognitoConfig): string {
  const { domain, clientId, logoutUri } = config;
  const params = new URLSearchParams({
    client_id: clientId,
    logout_uri: logoutUri,
  });
  return `https://${domain}/logout?${params.toString()}`;
}

/** Clears Cognito's hosted-UI session, then returns the browser to the signed-out route. */
export function redirectToHostedLogout(): void {
  window.location.assign(hostedLogoutUrl());
}

function providerLabel(provider: 'google' | 'facebook'): string {
  return provider === 'google' ? 'Google' : 'Facebook';
}

function providerAuthUrl(provider: 'google' | 'facebook'): string {
  return provider === 'google'
    ? nonProdAuthConfig.googleAuthUrl
    : nonProdAuthConfig.facebookAuthUrl;
}

export function buildNonProdProviderRedirectUrl(
  provider: 'google' | 'facebook',
  from: string,
): string {
  const base = providerAuthUrl(provider).trim();
  if (!base) {
    throw new Error(`${providerLabel(provider)} sign-in is not configured for this environment.`);
  }

  let url: URL;
  try {
    url = new URL(base);
  } catch {
    throw new Error(`${providerLabel(provider)} sign-in is configured with an invalid URL.`);
  }

  if (!url.searchParams.has('redirect_uri')) {
    url.searchParams.set('redirect_uri', nonProdAuthConfig.redirectUri);
  }
  url.searchParams.set('state', encodeProviderState(from, provider));
  return url.toString();
}

export function redirectToNonProdProvider(provider: 'google' | 'facebook', from: string): void {
  window.location.assign(buildNonProdProviderRedirectUrl(provider, from));
}
