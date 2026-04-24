import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { AppShell } from '@/components/layout/AppShell';
import { renderWithProviders } from '@/test/utils';

describe('AppShell', () => {
  it('renders a main landmark and primary nav with Dashboard/Watchlists/Transactions', () => {
    renderWithProviders(
      <AppShell>
        <div>content</div>
      </AppShell>,
    );
    expect(screen.getByRole('main')).toBeInTheDocument();
    const navs = screen.getAllByRole('navigation', { name: /primary/i });
    expect(navs.length).toBeGreaterThan(0); // sidebar + bottom tab
    expect(screen.getAllByRole('link', { name: /Dashboard/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /Watchlists/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /Transactions/i }).length).toBeGreaterThan(0);
  });

  it('marks the active route with aria-current via NavLink', () => {
    renderWithProviders(
      <AppShell>
        <div>content</div>
      </AppShell>,
      { route: '/watchlists' },
    );
    const watchlistLinks = screen.getAllByRole('link', { name: /Watchlists/i });
    expect(watchlistLinks.some((l) => l.getAttribute('aria-current') === 'page')).toBe(true);
  });
});
