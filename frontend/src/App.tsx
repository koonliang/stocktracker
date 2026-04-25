import { Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/layout/AppShell';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { PlaceholderRoute } from '@/routes/PlaceholderRoute';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
import { WatchlistDetailRoute } from '@/routes/WatchlistDetailRoute';
import { AnalysisRoute } from '@/routes/AnalysisRoute';
import { TransactionsRoute } from '@/routes/TransactionsRoute';

export function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardRoute />} />
        <Route path="/watchlists" element={<WatchlistsRoute />} />
        <Route path="/watchlists/:id" element={<WatchlistDetailRoute />} />
        <Route path="/transactions" element={<TransactionsRoute />} />
        <Route path="/analysis/:ticker" element={<AnalysisRoute />} />
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
