import { useEffect, useRef, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { completeCognitoCallback } from '@/auth/AuthProvider';
import { useAuthStore } from '@/stores/authStore';

type CallbackPhase = 'working' | 'done' | 'error';

// Bound the sign-in completion so a stalled token exchange / backend call surfaces an
// error instead of an indefinite spinner.
const TIMEOUT_MS = 20_000;

/**
 * Public landing for the Cognito Hosted-UI return (`/auth/callback?code=...`).
 * It must sit outside ProtectedRoute, otherwise the guard redirects to /login
 * and drops the `?code` before the token exchange can run. Once the exchange
 * sets the session we leave for the dashboard; on failure we show what went wrong.
 */
export function AuthCallbackRoute() {
  const setSession = useAuthStore((s) => s.setSession);
  const [phase, setPhase] = useState<CallbackPhase>('working');
  const [returnTo, setReturnTo] = useState('/');
  // The exchange must fire exactly once even under StrictMode double-invocation.
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const timeout = new Promise<never>((_, reject) =>
      setTimeout(() => reject(new Error('Sign-in timed out')), TIMEOUT_MS),
    );
    Promise.race([completeCognitoCallback(setSession), timeout])
      .then((from) => {
        setReturnTo(from);
        setPhase('done');
      })
      .catch((error) => {
        // Surface the real cause (CORS, token exchange, /me) in the browser console.
        console.error('Cognito sign-in failed:', error);
        setPhase('error');
      });
  }, [setSession]);

  if (phase === 'done') {
    return <Navigate to={returnTo} replace />;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>
        {phase === 'error' ? (
          <>
            <h1 className="font-display text-headline">Sign-in failed</h1>
            <p className="mt-2 text-small text-text-muted">
              We couldn&apos;t complete your sign-in. Please try again.
            </p>
            <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
              Back to sign in
            </Link>
          </>
        ) : (
          <p className="text-small text-text-muted">Completing sign-in…</p>
        )}
      </div>
    </div>
  );
}
