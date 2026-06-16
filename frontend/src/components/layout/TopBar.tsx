import { LogOut, Moon, Sun } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { useAuth } from '@/auth/AuthProvider';
import { useAuthStore } from '@/stores/authStore';
import { TickerSearch } from './TickerSearch';
import { BaseCurrencySelect } from './BaseCurrencySelect';

export function TopBar() {
  const theme = useUiStore((s) => s.theme);
  const toggle = useUiStore((s) => s.toggleTheme);
  const { logout } = useAuth();
  const user = useAuthStore((s) => s.user);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-bg/90 px-4 backdrop-blur-sm sm:px-6 lg:px-10">
      <div className="flex-1 md:max-w-md">
        <TickerSearch />
      </div>
      {user ? (
        <div className="hidden items-center gap-2 md:flex">
          <span className="eyebrow">Signed in</span>
          <span className="text-small text-text-muted">{user.email}</span>
        </div>
      ) : null}
      <div className="ml-auto">
        <BaseCurrencySelect />
      </div>
      <button
        type="button"
        onClick={toggle}
        aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
        className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-surface text-text-muted transition-colors hover:border-border-strong hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
      >
        {theme === 'dark' ? <Sun size={16} aria-hidden /> : <Moon size={16} aria-hidden />}
      </button>
      <button
        type="button"
        onClick={() => void logout()}
        data-testid="logout-button"
        aria-label="Sign out"
        className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-surface text-text-muted transition-colors hover:border-border-strong hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
      >
        <LogOut size={16} aria-hidden />
      </button>
    </header>
  );
}
