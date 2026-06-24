import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { BottomTabBar } from '@/components/layout/BottomTabBar';

function renderBar(route = '/') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <BottomTabBar />
    </MemoryRouter>,
  );
}

describe('BottomTabBar', () => {
  it('renders without visible text labels', () => {
    renderBar();
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
    expect(screen.queryByText('Trades')).not.toBeInTheDocument();
    expect(screen.queryByText('Returns')).not.toBeInTheDocument();
    expect(screen.queryByText('Alerts')).not.toBeInTheDocument();
    expect(screen.queryByText('Watchlists')).not.toBeInTheDocument();
  });

  it('marks the active nav item with data-testid when on dashboard', () => {
    renderBar('/');
    expect(screen.getByTestId('nav-active')).toBeInTheDocument();
  });

  it('renders nav items without an alerts badge', () => {
    renderBar();
    expect(screen.queryByTestId('alerts-badge')).not.toBeInTheDocument();
  });
});
