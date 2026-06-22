import { NavLink } from 'react-router-dom';
import {
  Bell,
  ChartNoAxesCombined,
  LayoutDashboard,
  ListChecks,
  ArrowLeftRight,
} from 'lucide-react';
import { cn } from '@/lib/cn';

const items = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/watchlists', label: 'Watchlists', icon: ListChecks },
  { to: '/transactions', label: 'Trades', icon: ArrowLeftRight },
  { to: '/performance', label: 'Returns', icon: ChartNoAxesCombined },
  { to: '/alerts', label: 'Alerts', icon: Bell },
];

export function BottomTabBar({ className }: { className?: string }) {
  return (
    <nav
      aria-label="Primary"
      className={cn(
        'fixed inset-x-0 bottom-0 left-0 right-0 z-50 border-t border-border bg-surface',
        'shadow-[0_-2px_12px_rgb(0_0_0/0.04)]',
        // iOS safe-area inset so the bar clears the home indicator.
        'pb-[env(safe-area-inset-bottom)]',
        className,
      )}
    >
      <ul className="mx-auto flex max-w-[640px] items-stretch justify-around">
        {items.map(({ to, label, icon: Icon }) => (
          <li key={to} className="flex-1">
            <NavLink
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                cn(
                  'flex h-16 flex-col items-center justify-center gap-1 text-micro uppercase tracking-tight sm:tracking-[0.08em]',
                  isActive ? 'text-accent' : 'text-text-muted',
                  'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-focus-ring',
                )
              }
            >
              <Icon size={18} aria-hidden />
              <span>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
