import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

type Tone = 'neutral' | 'positive' | 'negative' | 'accent';

type Props = HTMLAttributes<HTMLSpanElement> & {
  tone?: Tone;
  size?: 'sm' | 'md';
};

const toneClasses: Record<Tone, string> = {
  neutral: 'bg-surface-alt text-text-muted',
  positive: 'bg-positive-bg text-positive',
  negative: 'bg-negative-bg text-negative',
  accent: 'bg-accent/10 text-accent',
};

export function Badge({ tone = 'neutral', size = 'sm', className, ...props }: Props) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded font-medium',
        size === 'sm' ? 'px-1.5 py-0.5 text-micro tracking-[0.04em]' : 'px-2 py-0.5 text-small',
        toneClasses[tone],
        className,
      )}
      {...props}
    />
  );
}
