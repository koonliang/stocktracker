import { NavLink } from 'react-router-dom';
import {
  Bell,
  ChartNoAxesCombined,
  LayoutDashboard,
  ListChecks,
  ArrowLeftRight,
} from 'lucide-react';
import { cn } from '@/lib/cn';

type NavItem = { to: string; label: string; icon: React.ReactNode };

const items: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: <LayoutDashboard size={16} aria-hidden /> },
  { to: '/watchlists', label: 'Watchlists', icon: <ListChecks size={16} aria-hidden /> },
  { to: '/transactions', label: 'Transactions', icon: <ArrowLeftRight size={16} aria-hidden /> },
  { to: '/performance', label: 'Performance', icon: <ChartNoAxesCombined size={16} aria-hidden /> },
  { to: '/alerts', label: 'Alerts', icon: <Bell size={16} aria-hidden /> },
];

export function Sidebar({ className }: { className?: string }) {
  return (
    <aside
      className={cn(
        'fixed inset-y-0 left-0 z-30 h-screen w-60 flex-col border-r border-border bg-bg px-4 py-6',
        className,
      )}
    >
      <div className="mb-10 flex items-center gap-2 px-2">
        <div
          aria-hidden
          className="flex h-8 w-8 items-center justify-center rounded-sm bg-accent font-display text-title text-accent-fg"
        >
          S
        </div>
        <div className="leading-tight">
          <div className="font-display text-title text-text">StockTracker</div>
          <div className="eyebrow">Portfolio Ledger</div>
        </div>
      </div>
      <nav aria-label="Primary">
        <ul className="flex flex-col gap-0.5">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  cn(
                    'flex h-10 items-center gap-3 rounded-md px-3 text-body transition-colors duration-[120ms]',
                    isActive
                      ? 'bg-surface text-text shadow-card'
                      : 'text-text-muted hover:bg-surface-alt hover:text-text',
                    'focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring',
                  )
                }
              >
                {item.icon}
                <span>{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="mt-auto px-3" />
    </aside>
  );
}
