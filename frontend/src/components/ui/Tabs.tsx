import { cn } from '@/lib/cn';

type Item<T extends string> = { value: T; label: string };

type Props<T extends string> = {
  value: T;
  onChange: (v: T) => void;
  items: Item<T>[];
  ariaLabel?: string;
  size?: 'sm' | 'md';
  className?: string;
};

export function Tabs<T extends string>({
  value,
  onChange,
  items,
  ariaLabel,
  size = 'md',
  className,
}: Props<T>) {
  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      className={cn(
        'inline-flex items-center rounded-md border border-border bg-surface p-0.5',
        className,
      )}
    >
      {items.map((item) => {
        const active = item.value === value;
        return (
          <button
            key={item.value}
            role="tab"
            type="button"
            aria-selected={active}
            tabIndex={active ? 0 : -1}
            onClick={() => onChange(item.value)}
            className={cn(
              'rounded-[4px] font-medium transition-colors duration-[120ms]',
              size === 'sm' ? 'h-7 px-2.5 text-small' : 'h-8 px-3 text-small',
              active
                ? 'bg-accent text-accent-fg'
                : 'text-text-muted hover:bg-surface-alt hover:text-text',
              'focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring',
            )}
          >
            {item.label}
          </button>
        );
      })}
    </div>
  );
}
