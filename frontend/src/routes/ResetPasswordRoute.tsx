import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { ApiError } from '@/api/client';
import { resetPassword as resetPasswordRequest } from '@/api/authApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { PASSWORD_RULES, passwordMeetsPolicy } from '@/auth/passwordPolicy';

type FormValues = { password: string };

export function ResetPasswordRoute() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [done, setDone] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { password: '' } });

  const password = watch('password');

  async function onSubmit(values: FormValues) {
    setFormError(null);
    if (!token) {
      setFormError('This reset link is invalid or has expired.');
      return;
    }
    try {
      await resetPasswordRequest(token, values.password);
      setDone(true);
    } catch (error) {
      if (error instanceof ApiError && error.code === 'TOKEN_INVALID') {
        setFormError('This reset link is invalid or has expired.');
      } else if (error instanceof ApiError && error.status === 400) {
        setFormError('Please choose a password that meets the policy.');
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

        {done ? (
          <div data-testid="reset-confirmation">
            <h1 className="font-display text-headline">Password updated</h1>
            <p className="mt-2 text-small text-text-muted">
              Your password has been changed. You can sign in with it now.
            </p>
            <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
              Go to sign in
            </Link>
          </div>
        ) : (
          <>
            <h1 className="font-display text-headline">Set a new password</h1>
            <p className="mt-1 text-small text-text-muted">
              Choose a strong password for your account.
            </p>

            <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
              <div>
                <Label htmlFor="reset-password">New password</Label>
                <Input
                  id="reset-password"
                  type="password"
                  autoComplete="new-password"
                  data-testid="reset-password"
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
                <p role="alert" data-testid="reset-error" className="text-small text-negative">
                  {formError}
                </p>
              ) : null}

              <Button
                type="submit"
                data-testid="reset-submit"
                loading={isSubmitting}
                className="w-full"
              >
                Update password
              </Button>
            </form>

            <div className="mt-6 text-small text-text-muted">
              <Link to="/login" className="hover:text-text">
                Back to sign in
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
