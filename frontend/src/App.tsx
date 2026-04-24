import { Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/layout/AppShell';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { PlaceholderRoute } from '@/routes/PlaceholderRoute';

export function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardRoute />} />
        <Route
          path="/watchlists"
          element={
            <PlaceholderRoute
              eyebrow="Watchlists"
              title="Watchlists"
              description="Named lists of tickers you want to monitor without owning. Shipping in the next iteration."
            />
          }
        />
        <Route
          path="/watchlists/:id"
          element={
            <PlaceholderRoute
              eyebrow="Watchlist"
              title="Watchlist detail"
              description="Individual watchlist detail will land with the next iteration."
            />
          }
        />
        <Route
          path="/transactions"
          element={
            <PlaceholderRoute
              eyebrow="Transactions"
              title="Import &amp; Export"
              description="CSV import with a validation preview and one-click export. Shipping after watchlists."
            />
          }
        />
        <Route
          path="/analysis/:ticker"
          element={
            <PlaceholderRoute
              eyebrow="Analysis"
              title="Stock analysis"
              description="Price chart, key statistics, and position summary for a single ticker. Next up."
            />
          }
        />
        <Route
          path="*"
          element={
            <PlaceholderRoute
              eyebrow="404"
              title="Page not found"
              description="The page you requested does not exist."
            />
          }
        />
      </Routes>
    </AppShell>
  );
}
