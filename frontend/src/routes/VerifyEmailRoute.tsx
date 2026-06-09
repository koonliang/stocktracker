import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { verifyEmail as verifyEmailRequest } from '@/api/authApi';

type VerifyStatus = 'verifying' | 'verified' | 'error' | 'missing';

export function VerifyEmailRoute() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [status, setStatus] = useState<VerifyStatus>(token ? 'verifying' : 'missing');
  // The verify request must fire exactly once even under StrictMode double-invocation.
  const started = useRef(false);

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    verifyEmailRequest(token)
      .then(() => setStatus('verified'))
      .catch(() => setStatus('error'));
  }, [token]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm" data-testid="verify-status" data-status={status}>
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>

        {status === 'verifying' ? (
          <p className="text-small text-text-muted">Verifying your email…</p>
        ) : null}

        {status === 'verified' ? (
          <>
            <h1 className="font-display text-headline">Email verified</h1>
            <p className="mt-2 text-small text-text-muted">
              Your account is active. You can sign in now.
            </p>
            <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
              Go to sign in
            </Link>
          </>
        ) : null}

        {status === 'error' || status === 'missing' ? (
          <>
            <h1 className="font-display text-headline">Verification failed</h1>
            <p className="mt-2 text-small text-text-muted">
              This verification link is invalid or has expired. Request a new one from the sign-in
              page.
            </p>
            <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
              Back to sign in
            </Link>
          </>
        ) : null}
      </div>
    </div>
  );
}
