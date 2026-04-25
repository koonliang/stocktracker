import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { KeyStatsGrid } from '@/features/analysis/KeyStatsGrid';
import type { KeyStats } from '@/lib/types';

const fullStats: KeyStats = {
  symbol: 'AAPL',
  open: 180,
  high: 182,
  low: 178,
  previousClose: 179,
  volume: 50_000_000,
  week52High: 200,
  week52Low: 150,
  marketCap: 3_000_000_000_000,
  peRatio: 28.4,
};

describe('KeyStatsGrid', () => {
  it('renders every labelled stat for a complete fixture', () => {
    render(<KeyStatsGrid stats={fullStats} />);
    for (const label of [
      'Open',
      'Day High',
      'Day Low',
      'Previous Close',
      'Volume',
      '52W High',
      '52W Low',
      'Market Cap',
      'P/E Ratio',
    ]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });

  it('renders em dash for null/missing values and never renders NaN', () => {
    const partial: KeyStats = { ...fullStats, peRatio: null };
    render(<KeyStatsGrid stats={partial} />);
    // P/E Ratio cell should show em dash
    const cells = screen.getAllByText('—');
    expect(cells.length).toBeGreaterThan(0);
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });

  it('renders all em dashes when stats is null', () => {
    render(<KeyStatsGrid stats={null} />);
    const cells = screen.getAllByText('—');
    expect(cells).toHaveLength(9);
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });
});
