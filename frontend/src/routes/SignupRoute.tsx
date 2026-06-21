import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { ApiError } from '@/api/client';
import { SocialLoginButtons } from '@/components/auth/SocialLoginButtons';
import { signup as signupRequest } from '@/api/authApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { PASSWORD_RULES, passwordMeetsPolicy } from '@/auth/passwordPolicy';
import { isCognitoMode, redirectToHostedUi } from '@/auth/authConfig';
import { useAuth } from '@/auth/AuthProvider';

type FormValues = { email: string; password: string };

export function SignupRoute() {
  const { loginWithProvider } = useAuth();
  const [submitted, setSubmitted] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { email: '', password: '' } });

  const password = watch('password');

  // In cognito mode Cognito owns sign-up + verification (incl. social), so go straight
  // to the Hosted UI rather than rendering the dev email/password form first.
  useEffect(() => {
    if (isCognitoMode) {
      redirectToHostedUi({ flow: 'signup' });
    }
  }, []);
  if (isCognitoMode) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2">
            <LineChart size={22} className="text-accent" aria-hidden />
            <span className="font-display text-title">StockTracker</span>
          </div>
          <p className="text-small text-text-muted">Redirecting to sign-up…</p>
        </div>
      </div>
    );
  }

  async function onSubmit(values: FormValues) {
    setFormError(null);
    try {
      await signupRequest(values.email, values.password);
      setSubmitted(values.email);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setFormError('Please enter a valid email and a password that meets the policy.');
      } else {
        setFormError('Something went wrong. Please try again.');
      }
    }
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_rgba(217,119,6,0.08),_transparent_28%),linear-gradient(180deg,#faf7f2_0%,#f4efe6_100%)] px-4 py-10 text-text">
      <div className="mx-auto w-full max-w-md">
        <div className="mb-8 flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-accent/10 text-accent">
            <LineChart size={22} aria-hidden />
          </div>
          <span className="font-display text-title">StockTracker</span>
        </div>

        <div className="rounded-[32px] border border-black/5 bg-white/82 p-6 shadow-[0_24px_60px_rgba(15,23,42,0.10)] backdrop-blur-sm sm:p-8">
          {submitted ? (
            <div data-testid="signup-success">
              <p className="eyebrow">Account Access</p>
              <h1 className="font-display text-headline">Account ready</h1>
              <p className="mt-2 text-small text-text-muted">
                <span className="text-text">{submitted}</span> is active immediately in dev mode.
                You can sign in now without email verification.
              </p>
              <Link
                to="/login"
                className="mt-6 inline-block text-small text-accent hover:text-text"
              >
                Back to sign in
              </Link>
            </div>
          ) : (
            <>
              <p className="eyebrow">Account Access</p>
              <h1 className="font-display text-headline">Create an account</h1>
              <p className="mt-1 text-small text-text-muted">
                Start tracking your portfolio in minutes.
              </p>

              <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
                <div>
                  <Label htmlFor="signup-email">Email</Label>
                  <Input
                    id="signup-email"
                    type="email"
                    autoComplete="email"
                    data-testid="signup-email"
                    invalid={Boolean(errors.email)}
                    className="rounded-xl bg-[#fffdfa]"
                    {...register('email', { required: 'Email is required' })}
                  />
                </div>

                <div>
                  <Label htmlFor="signup-password">Password</Label>
                  <Input
                    id="signup-password"
                    type="password"
                    autoComplete="new-password"
                    data-testid="signup-password"
                    invalid={Boolean(errors.password)}
                    className="rounded-xl bg-[#fffdfa]"
                    {...register('password', {
                      required: 'Password is required',
                      validate: (value) =>
                        passwordMeetsPolicy(value) || 'Password does not meet the policy',
                    })}
                  />
                  <ul
                    className="mt-2 space-y-1 text-small text-text-muted"
                    data-testid="password-rules"
                  >
                    {PASSWORD_RULES.map((rule) => {
                      const met = rule.test(password ?? '');
                      return (
                        <li key={rule.label} className={met ? 'text-positive' : undefined}>
                          {met ? '✓' : '•'} {rule.label}
                        </li>
                      );
                    })}
                  </ul>
                </div>

                {formError ? (
                  <p role="alert" data-testid="signup-error" className="text-small text-negative">
                    {formError}
                  </p>
                ) : null}

                <Button
                  type="submit"
                  data-testid="signup-submit"
                  loading={isSubmitting}
                  className="w-full rounded-xl"
                >
                  Create account
                </Button>
              </form>

              <div className="mt-8">
                <div className="mb-4 flex items-center gap-3">
                  <div className="h-px flex-1 bg-line" />
                  <p className="text-small text-text-muted">or continue with</p>
                  <div className="h-px flex-1 bg-line" />
                </div>
                <SocialLoginButtons onClick={(provider) => loginWithProvider(provider)} />
              </div>

              <div className="mt-6 text-small text-text-muted">
                <Link to="/login" className="hover:text-text">
                  Already have an account? Sign in
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
