import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { ApiError } from '@/api/client';
import { DemoUserPanel } from '@/components/auth/DemoUserPanel';
import { SocialLoginButtons } from '@/components/auth/SocialLoginButtons';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { useAuth } from '@/auth/AuthProvider';
import { isCognitoMode, redirectToHostedUi } from '@/auth/authConfig';
import { useAuthStore } from '@/stores/authStore';
import type { DemoUserCatalog } from '@/api/types';

type FormValues = { email: string; password: string };

const ERROR_COPY: Record<'invalid' | 'unverified' | 'server', string> = {
  invalid: 'Invalid email or password.',
  unverified: 'Please verify your email before signing in.',
  server: 'Something went wrong. Please try again.',
};

export function LoginRoute() {
  const { login, loginWithProvider, fetchDemoUsers, createDemoUserSession, loginAsDemoUser } =
    useAuth();
  const status = useAuthStore((s) => s.status);
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState<string | null>(null);
  const [catalog, setCatalog] = useState<DemoUserCatalog | null>(null);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [pendingDemoSlot, setPendingDemoSlot] = useState<number | null>(null);
  const [demoFeedback, setDemoFeedback] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { email: '', password: '' } });

  const from = (location.state as { from?: string } | null)?.from ?? '/';

  useEffect(() => {
    if (isCognitoMode && status !== 'authenticated') {
      redirectToHostedUi({ from });
    }
  }, [from, status]);

  useEffect(() => {
    if (isCognitoMode) {
      return;
    }
    setCatalogLoading(true);
    fetchDemoUsers()
      .then(setCatalog)
      .catch(() => setCatalog(null))
      .finally(() => {
        setCatalogLoading(false);
        setDemoFeedback(null);
      });
  }, [fetchDemoUsers]);

  if (status === 'authenticated') {
    return <Navigate to={from} replace />;
  }

  if (isCognitoMode) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-md bg-accent text-accent-fg">
              <LineChart size={20} aria-hidden />
            </span>
            <span className="font-display text-title tracking-tight">StockTracker</span>
          </div>
          <p className="flex items-center gap-2 text-small text-text-muted">
            <span
              aria-hidden
              className="inline-block h-3 w-3 animate-spin rounded-full border-2 border-text-subtle border-t-transparent"
            />
            Redirecting to sign in…
          </p>
        </div>
      </div>
    );
  }

  async function onSubmit(values: FormValues) {
    setFormError(null);
    setDemoFeedback(null);
    const result = await login(values.email, values.password);
    if (result.ok) {
      navigate(from, { replace: true });
    } else {
      setFormError(ERROR_COPY[result.reason]);
    }
  }

  async function onCreateDemoUser() {
    setFormError(null);
    setDemoFeedback(null);
    setCatalogLoading(true);
    try {
      await createDemoUserSession();
      navigate(from, { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.code === 'DEMO_USER_LIMIT_REACHED') {
        setDemoFeedback(error.message);
      } else {
        setFormError(ERROR_COPY.server);
      }
      const nextCatalog = await fetchDemoUsers().catch(() => null);
      setCatalog(nextCatalog);
      setCatalogLoading(false);
    }
  }

  async function onLoginDemoUser(slot: number) {
    setPendingDemoSlot(slot);
    setFormError(null);
    setDemoFeedback(null);
    try {
      await loginAsDemoUser(slot);
      navigate(from, { replace: true });
    } catch {
      setFormError(ERROR_COPY.server);
    } finally {
      setPendingDemoSlot(null);
    }
  }

  return (
    <div className="min-h-dvh bg-bg text-text">
      <main className="flex min-h-dvh flex-col justify-center px-5 py-4 sm:px-8 sm:py-5">
        <div className="mx-auto w-full max-w-md space-y-3.5">
          {/* Wordmark */}
          <div className="animate-rise flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-md bg-accent text-accent-fg">
              <LineChart size={20} aria-hidden />
            </span>
            <span className="font-display text-title tracking-tight">StockTracker</span>
          </div>

          <section
            className="animate-rise rounded-xl border border-border bg-surface p-5 shadow-card sm:p-6"
            style={{ animationDelay: '60ms' }}
          >
            <p className="eyebrow">Account Access</p>
            <h1 className="mt-1.5 font-display text-display tracking-tight">Sign in</h1>
            <p className="mt-1.5 text-small text-text-muted">
              Access your portfolio, watchlists, and analysis with your email or a connected
              social account.
            </p>

            <form className="mt-5 space-y-3" onSubmit={handleSubmit(onSubmit)} noValidate>
              <div>
                <Label htmlFor="login-email">Email</Label>
                <Input
                  id="login-email"
                  type="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                  data-testid="login-email"
                  invalid={Boolean(errors.email)}
                  {...register('email', { required: 'Email is required' })}
                />
              </div>

              <div>
                <Label htmlFor="login-password">Password</Label>
                <Input
                  id="login-password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  data-testid="login-password"
                  invalid={Boolean(errors.password)}
                  {...register('password', { required: 'Password is required' })}
                />
              </div>

              {formError ? (
                <p
                  role="alert"
                  data-testid="login-error"
                  className="rounded-md border border-negative/30 bg-negative-bg px-3 py-2 text-small text-negative"
                >
                  {formError}
                </p>
              ) : null}

              <Button
                type="submit"
                size="lg"
                data-testid="login-submit"
                loading={isSubmitting}
                className="mt-1 w-full"
              >
                Sign in
              </Button>
            </form>

            <div className="mt-5">
              <div className="mb-3 flex items-center gap-3">
                <div className="h-px flex-1 bg-border" />
                <p className="text-micro uppercase tracking-[0.14em] text-text-subtle">
                  or continue with
                </p>
                <div className="h-px flex-1 bg-border" />
              </div>
              <SocialLoginButtons onClick={(provider) => loginWithProvider(provider, from)} />
            </div>

            <div className="mt-5 flex items-center justify-between border-t border-border pt-4 text-small">
              <Link to="/signup" className="font-medium text-accent hover:text-accent-hover">
                Create an account
              </Link>
              <Link to="/forgot-password" className="text-text-muted hover:text-text">
                Forgot password?
              </Link>
            </div>
          </section>

          <div className="animate-rise" style={{ animationDelay: '120ms' }}>
            <DemoUserPanel
              catalog={catalog}
              loading={catalogLoading}
              pendingSlot={pendingDemoSlot}
              feedback={demoFeedback}
              onCreate={onCreateDemoUser}
              onLogin={onLoginDemoUser}
            />
          </div>
        </div>
      </main>
    </div>
  );
}
