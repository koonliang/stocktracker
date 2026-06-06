import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { useAuth, type SocialProvider } from '@/auth/AuthProvider';
import { isCognitoMode } from '@/auth/authConfig';
import { useAuthStore } from '@/stores/authStore';

type FormValues = { email: string; password: string };

const ERROR_COPY: Record<'invalid' | 'unverified' | 'server', string> = {
  invalid: 'Invalid email or password.',
  unverified: 'Please verify your email before signing in.',
  server: 'Something went wrong. Please try again.',
};

export function LoginRoute() {
  const { login, loginWithProvider } = useAuth();
  const status = useAuthStore((s) => s.status);
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { email: '', password: '' } });

  const from = (location.state as { from?: string } | null)?.from ?? '/';

  if (status === 'authenticated') {
    return <Navigate to={from} replace />;
  }

  async function onSubmit(values: FormValues) {
    setFormError(null);
    const result = await login(values.email, values.password);
    if (result.ok) {
      navigate(from, { replace: true });
    } else {
      setFormError(ERROR_COPY[result.reason]);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>

        <h1 className="font-display text-headline">Sign in</h1>
        <p className="mt-1 text-small text-text-muted">
          Access your portfolio, watchlists, and analysis.
        </p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div>
            <Label htmlFor="login-email">Email</Label>
            <Input
              id="login-email"
              type="email"
              autoComplete="email"
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
              data-testid="login-password"
              invalid={Boolean(errors.password)}
              {...register('password', { required: 'Password is required' })}
            />
          </div>

          {formError ? (
            <p role="alert" data-testid="login-error" className="text-small text-negative">
              {formError}
            </p>
          ) : null}

          <Button
            type="submit"
            data-testid="login-submit"
            loading={isSubmitting}
            className="w-full"
          >
            Sign in
          </Button>
        </form>

        {isCognitoMode ? (
          <div className="mt-4 space-y-2">
            <SocialButton provider="google" onClick={loginWithProvider}>
              Continue with Google
            </SocialButton>
            <SocialButton provider="facebook" onClick={loginWithProvider}>
              Continue with Facebook
            </SocialButton>
          </div>
        ) : null}

        <div className="mt-6 flex items-center justify-between text-small text-text-muted">
          <Link to="/signup" className="hover:text-text">
            Create an account
          </Link>
          <Link to="/forgot-password" className="hover:text-text">
            Forgot password?
          </Link>
        </div>
      </div>
    </div>
  );
}

function SocialButton({
  provider,
  onClick,
  children,
}: {
  provider: SocialProvider;
  onClick: (provider: SocialProvider) => void;
  children: React.ReactNode;
}) {
  return (
    <Button type="button" variant="secondary" className="w-full" onClick={() => onClick(provider)}>
      {children}
    </Button>
  );
}
