import { useEffect, type ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { BottomTabBar } from './BottomTabBar';
import { TopBar } from './TopBar';
import { useUiStore } from '@/stores/uiStore';

type Props = { children: ReactNode };

export function AppShell({ children }: Props) {
  const theme = useUiStore((s) => s.theme);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  return (
    <div className="min-h-screen bg-bg text-text" data-testid="app-shell-authenticated">
      <Sidebar className="hidden md:flex" />
      <div className="flex min-h-screen min-w-0 flex-col md:pl-60">
        <TopBar />
        <main
          id="main"
          className="min-w-0 flex-1 overflow-x-hidden px-4 pb-24 pt-6 sm:px-6 md:pb-10 md:pt-8 lg:px-10 xl:px-14"
        >
          <div className="mx-auto w-full max-w-[1280px]">{children}</div>
        </main>
      </div>
      <BottomTabBar className="md:hidden" />
    </div>
  );
}
