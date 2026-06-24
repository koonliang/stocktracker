import type { ElementType } from 'react';
import { Plus } from 'lucide-react';
import { cn } from '@/lib/cn';

type Props = {
  onClick: () => void;
  label: string;
  icon?: ElementType;
  className?: string;
};

export function FAB({ onClick, label, icon: Icon = Plus, className }: Props) {
  return (
    <button
      type="button"
      aria-label={label}
      data-testid="fab"
      onClick={onClick}
      className={cn(
        'fixed bottom-24 right-4 z-40 md:hidden',
        'flex h-14 w-14 items-center justify-center rounded-full',
        'bg-accent text-accent-fg shadow-lg',
        'transition-transform active:scale-95',
        'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring',
        className,
      )}
    >
      <Icon size={24} aria-hidden />
    </button>
  );
}
