import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { ApiError } from '@/api/client';
import { signup as signupRequest } from '@/api/authApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { PASSWORD_RULES, passwordMeetsPolicy } from '@/auth/passwordPolicy';
import { isCognitoMode, redirectToHostedUi } from '@/auth/authConfig';

type FormValues = { email: string; password: string };

export function SignupRoute() {
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
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>

        {submitted ? (
          <div data-testid="signup-success">
            <h1 className="font-display text-headline">Check your email</h1>
            <p className="mt-2 text-small text-text-muted">
              We sent a verification link to <span className="text-text">{submitted}</span>. Confirm
              it to activate your account, then sign in.
            </p>
            <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
              Back to sign in
            </Link>
          </div>
        ) : (
          <>
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
                className="w-full"
              >
                Create account
              </Button>
            </form>

            <div className="mt-6 text-small text-text-muted">
              <Link to="/login" className="hover:text-text">
                Already have an account? Sign in
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
