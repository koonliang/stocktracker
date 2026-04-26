import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe } from 'vitest-axe';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AnalysisRoute } from '@/routes/AnalysisRoute';
import { loadTickers } from '@/lib/seed';

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<div>dashboard</div>} />
        <Route path="/analysis/:ticker" element={<AnalysisRoute />} />
      </Routes>
    </MemoryRouter>,
  );
}

const known = loadTickers()[0]!.symbol;

describe('AnalysisRoute', () => {
  beforeEach(() => {
    localStorage.clear();
  });
  afterEach(() => {
    localStorage.clear();
  });

  it('renders header, chart, and key stats for a known ticker', async () => {
    renderAt(`/analysis/${known}`);
    expect(await screen.findByRole('heading', { level: 1, name: known })).toBeInTheDocument();
    expect(screen.getByTestId('price-chart')).toBeInTheDocument();
    expect(screen.getByText('Snapshot')).toBeInTheDocument();
  });

  it('renders a not-found state for an unknown ticker with a link to the dashboard', async () => {
    renderAt('/analysis/ZZZZ');
    expect(await screen.findByText(/We could not load "ZZZZ"/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Back to dashboard/i })).toBeInTheDocument();
  });

  it('has no critical accessibility violations', async () => {
    const { container } = renderAt(`/analysis/${known}`);
    await screen.findByRole('heading', { level: 1, name: known });
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
