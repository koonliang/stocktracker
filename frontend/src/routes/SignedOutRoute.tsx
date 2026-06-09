import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';

export function SignedOutRoute() {
  const clearSession = useAuthStore((s) => s.clearSession);

  useEffect(() => {
    clearSession();
  }, [clearSession]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>
        <h1 className="font-display text-headline">Signed out</h1>
        <p className="mt-2 text-small text-text-muted">Your session has ended.</p>
        <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
          Sign in again
        </Link>
      </div>
    </div>
  );
}
