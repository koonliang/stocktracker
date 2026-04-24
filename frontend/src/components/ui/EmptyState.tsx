import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type Props = {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
  icon?: ReactNode;
  className?: string;
};

export function EmptyState({ eyebrow, title, description, actions, icon, className }: Props) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-4 rounded-xl border border-dashed border-border bg-surface-alt/40 px-6 py-16 text-center',
        className,
      )}
    >
      {icon && (
        <div
          aria-hidden
          className="flex h-12 w-12 items-center justify-center rounded-full border border-border bg-surface text-text-muted"
        >
          {icon}
        </div>
      )}
      {eyebrow && <div className="eyebrow">{eyebrow}</div>}
      <h2 className="max-w-md font-display text-headline text-text">{title}</h2>
      {description && <p className="max-w-md text-body text-text-muted">{description}</p>}
      {actions && (
        <div className="mt-2 flex flex-wrap items-center justify-center gap-2">{actions}</div>
      )}
    </div>
  );
}
