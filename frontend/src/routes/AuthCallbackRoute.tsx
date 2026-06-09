import { useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { completeCognitoCallback } from '@/auth/AuthProvider';
import { useAuthStore } from '@/stores/authStore';

type CallbackPhase = 'working' | 'done' | 'error';

/**
 * Public landing for the Cognito Hosted-UI return (`/auth/callback?code=...`).
 * It must sit outside ProtectedRoute, otherwise the guard redirects to /login
 * and drops the `?code` before the token exchange can run. Once the exchange
 * sets the session we leave for the dashboard; on failure we fall back to login.
 */
export function AuthCallbackRoute() {
  const setSession = useAuthStore((s) => s.setSession);
  const [phase, setPhase] = useState<CallbackPhase>('working');
  // The exchange must fire exactly once even under StrictMode double-invocation.
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    completeCognitoCallback(setSession)
      .then(() => setPhase('done'))
      .catch(() => setPhase('error'));
  }, [setSession]);

  if (phase === 'done') {
    return <Navigate to="/" replace />;
  }
  if (phase === 'error') {
    return <Navigate to="/login" replace state={{ error: 'social' }} />;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>
        <p className="text-small text-text-muted">Completing sign-in…</p>
      </div>
    </div>
  );
}
