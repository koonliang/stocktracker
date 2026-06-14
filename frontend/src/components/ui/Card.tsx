import { forwardRef, type HTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

type Props = HTMLAttributes<HTMLDivElement> & {
  as?: 'div' | 'section' | 'article';
  padded?: boolean;
  overflow?: 'hidden' | 'visible';
};

export const Card = forwardRef<HTMLDivElement, Props>(
  (
    { as: Tag = 'div', padded = true, overflow = 'hidden', className, children, ...props },
    ref,
  ) => {
    return (
      <Tag
        ref={ref as React.Ref<HTMLDivElement>}
        className={cn(
          'min-w-0 rounded-lg border border-border bg-surface shadow-card',
          overflow === 'hidden' ? 'overflow-hidden' : 'overflow-visible',
          padded && 'p-5 sm:p-6',
          className,
        )}
        {...props}
      >
        {children}
      </Tag>
    );
  },
);
Card.displayName = 'Card';

export function CardHeader({
  eyebrow,
  title,
  action,
  className,
}: {
  eyebrow?: string;
  title?: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn('mb-4 flex items-start justify-between gap-4', className)}>
      <div>
        {eyebrow && <div className="eyebrow mb-1">{eyebrow}</div>}
        {title && <h2 className="font-display text-title text-text">{title}</h2>}
      </div>
      {action ? <div className="flex-shrink-0">{action}</div> : null}
    </div>
  );
}
