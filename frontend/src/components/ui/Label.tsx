import { type LabelHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

/* Styled <label>; callers associate with a control via `htmlFor`. */
/* eslint-disable-next-line jsx-a11y/label-has-associated-control -- this is a reusable label component; association is enforced at call sites. */
export function Label({ className, ...props }: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    // eslint-disable-next-line jsx-a11y/label-has-associated-control
    <label
      className={cn('mb-1.5 block text-small font-medium text-text-muted', className)}
      {...props}
    />
  );
}
