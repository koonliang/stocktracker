// Selects the client-side auth strategy. `dev` drives the backend /api/auth/*
// flows (used by local dev and the e2e journey); `cognito` uses the hosted UI.
export type AuthMode = 'dev' | 'cognito';

export const authMode: AuthMode = import.meta.env.VITE_AUTH_MODE === 'cognito' ? 'cognito' : 'dev';

export const isCognitoMode = authMode === 'cognito';
export const isDevMode = authMode === 'dev';
