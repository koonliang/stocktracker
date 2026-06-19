import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SummaryTiles } from '@/features/dashboard/SummaryTiles';

describe('SummaryTiles', () => {
  it('renders all four tile headings and values', () => {
    render(
      <SummaryTiles
        summary={{
          totalMarketValue: 100_000,
          totalCostBasis: 80_000,
          totalUnrealizedPnL: 20_000,
          totalUnrealizedPnLPct: 0.25,
          totalDayChange: -500,
          totalDayChangePct: -0.005,
        }}
      />,
    );
    expect(screen.getByText(/Market Value/i)).toBeInTheDocument();
    expect(screen.getByText(/Cost Basis/i)).toBeInTheDocument();
    expect(screen.getByText(/Unrealised P&L/i)).toBeInTheDocument();
    expect(screen.getByText(/Today/i)).toBeInTheDocument();
    expect(screen.getByText('$100,000')).toBeInTheDocument();
    expect(screen.getByText('+$20,000')).toBeInTheDocument();
    expect(screen.getByText(/−\$500$/)).toBeInTheDocument();
  });

  it('handles zero cost basis without NaN', () => {
    render(
      <SummaryTiles
        summary={{
          totalMarketValue: 0,
          totalCostBasis: 0,
          totalUnrealizedPnL: 0,
          totalUnrealizedPnLPct: 0,
          totalDayChange: 0,
          totalDayChangePct: 0,
        }}
      />,
    );
    // No NaN strings present
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });

  it('renders stale and unavailable FX indicators', () => {
    render(
      <SummaryTiles
        summary={{
          totalMarketValue: 100_000,
          totalCostBasis: 80_000,
          totalUnrealizedPnL: 20_000,
          totalUnrealizedPnLPct: 0.25,
          totalDayChange: 0,
          totalDayChangePct: 0,
          baseCurrency: 'SGD',
          marketValueConversion: {
            baseCurrency: 'SGD',
            amountBase: 100_000,
            fxDate: '2026-06-17',
            fxStatus: 'stale',
          },
          costBasisConversion: {
            baseCurrency: 'SGD',
            amountBase: 80_000,
            fxDate: null,
            fxStatus: 'unavailable',
          },
        }}
      />,
    );

    expect(screen.getByText('Stale rate')).toBeInTheDocument();
    expect(screen.getByText('Rate unavailable')).toBeInTheDocument();
  });

  it('shows the selected base currency label', () => {
    render(
      <SummaryTiles
        summary={{
          totalMarketValue: 100_000,
          totalCostBasis: 80_000,
          totalUnrealizedPnL: 20_000,
          totalUnrealizedPnLPct: 0.25,
          totalDayChange: 0,
          totalDayChangePct: 0,
          baseCurrency: 'SGD',
        }}
      />,
    );

    expect(screen.getByText(/Base currency SGD/i)).toBeInTheDocument();
  });
});
