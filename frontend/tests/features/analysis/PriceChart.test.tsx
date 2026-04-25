import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PriceChart, filterBarsByRange } from '@/features/analysis/PriceChart';
import type { PriceBar, TimeRange } from '@/lib/types';

function makeBars(n: number): PriceBar[] {
  const bars: PriceBar[] = [];
  for (let i = 0; i < n; i += 1) {
    const close = 100 + i;
    bars.push({
      date: `2024-01-${String((i % 28) + 1).padStart(2, '0')}`,
      open: close,
      high: close + 1,
      low: close - 1,
      close,
      volume: 1_000,
    });
  }
  return bars;
}

describe('filterBarsByRange', () => {
  const bars = makeBars(2_000);

  it.each<[TimeRange, number]>([
    ['1D', 1],
    ['1W', 5],
    ['1M', 21],
    ['3M', 63],
    ['1Y', 252],
    ['5Y', 1260],
    ['ALL', 2000],
  ])('range %s yields %i bars', (range, expected) => {
    expect(filterBarsByRange(bars, range)).toHaveLength(expected);
  });

  it('returns the full series when fewer bars exist than the range', () => {
    expect(filterBarsByRange(makeBars(10), '1Y')).toHaveLength(10);
  });
});

describe('PriceChart', () => {
  it('marks the active range with aria-pressed=true and others false', () => {
    render(<PriceChart bars={makeBars(500)} range="1M" onRangeChange={() => {}} />);
    const oneMonth = screen.getByRole('button', { name: '1M' });
    const oneYear = screen.getByRole('button', { name: '1Y' });
    expect(oneMonth).toHaveAttribute('aria-pressed', 'true');
    expect(oneYear).toHaveAttribute('aria-pressed', 'false');
  });

  it('exposes the filtered bar count for the selected range', () => {
    render(<PriceChart bars={makeBars(500)} range="3M" onRangeChange={() => {}} />);
    const chart = screen.getByTestId('price-chart');
    expect(chart.getAttribute('data-bar-count')).toBe('63');
  });

  it('invokes onRangeChange when a range button is clicked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PriceChart bars={makeBars(500)} range="1Y" onRangeChange={onChange} />);
    await user.click(screen.getByRole('button', { name: '1W' }));
    expect(onChange).toHaveBeenCalledWith('1W');
  });
});
