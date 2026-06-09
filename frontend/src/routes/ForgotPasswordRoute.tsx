import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { forgotPassword as forgotPasswordRequest } from '@/api/authApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { isCognitoMode, redirectToHostedUi } from '@/auth/authConfig';

type FormValues = { email: string };

export function ForgotPasswordRoute() {
  const [submitted, setSubmitted] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ defaultValues: { email: '' } });

  async function onSubmit(values: FormValues) {
    // In cognito mode Cognito owns password reset; hand off to the Hosted UI.
    if (isCognitoMode) {
      redirectToHostedUi({ flow: 'reset' });
      return;
    }
    // Non-enumerating: always show the same neutral confirmation, even on failure (FR-016, SC-005).
    await forgotPasswordRequest(values.email).catch(() => undefined);
    setSubmitted(true);
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 py-12 text-text">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2">
          <LineChart size={22} className="text-accent" aria-hidden />
          <span className="font-display text-title">StockTracker</span>
        </div>

        {submitted ? (
          <div data-testid="forgot-confirmation">
            <h1 className="font-display text-headline">Check your email</h1>
            <p className="mt-2 text-small text-text-muted">
              If an account exists for that address, we sent a link to reset your password.
            </p>
            <Link to="/login" className="mt-6 inline-block text-small text-accent hover:text-text">
              Back to sign in
            </Link>
          </div>
        ) : (
          <>
            <h1 className="font-display text-headline">Reset your password</h1>
            <p className="mt-1 text-small text-text-muted">
              Enter your email and we&apos;ll send you a reset link.
            </p>

            <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
              <div>
                <Label htmlFor="forgot-email">Email</Label>
                <Input
                  id="forgot-email"
                  type="email"
                  autoComplete="email"
                  data-testid="forgot-email"
                  invalid={Boolean(errors.email)}
                  {...register('email', { required: 'Email is required' })}
                />
              </div>

              <Button
                type="submit"
                data-testid="forgot-submit"
                loading={isSubmitting}
                className="w-full"
              >
                Send reset link
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
