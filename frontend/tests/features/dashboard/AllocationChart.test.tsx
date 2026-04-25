import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AllocationChart } from '@/features/dashboard/AllocationChart';
import type { Holding } from '@/lib/types';

function h(ticker: string, name: string, weight: number, mv: number): Holding {
  return {
    ticker,
    name,
    shares: 1,
    averageCost: 1,
    costBasis: 1,
    currentPrice: 1,
    marketValue: mv,
    unrealizedPnL: 0,
    unrealizedPnLPct: 0,
    dayChange: 0,
    dayChangePct: 0,
    weight,
  };
}

describe('AllocationChart', () => {
  it('renders an accessible legend row per holding with weights summing to 1', () => {
    const holdings = [
      h('AAPL', 'Apple Inc.', 0.4, 400),
      h('MSFT', 'Microsoft', 0.35, 350),
      h('NVDA', 'NVIDIA', 0.25, 250),
    ];
    render(<AllocationChart holdings={holdings} />);
    for (const t of ['AAPL', 'MSFT', 'NVDA']) {
      expect(screen.getByText(t)).toBeInTheDocument();
    }
    // Weights rendered as percentages
    expect(screen.getByText('40.00%')).toBeInTheDocument();
    expect(screen.getByText('35.00%')).toBeInTheDocument();
    expect(screen.getByText('25.00%')).toBeInTheDocument();
    const totalWeight = holdings.reduce((s, h) => s + h.weight, 0);
    expect(totalWeight).toBeCloseTo(1);
  });

  it('renders nothing when there are no holdings', () => {
    const { container } = render(<AllocationChart holdings={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
