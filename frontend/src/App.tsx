import { Outlet, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/layout/AppShell';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { LoginRoute } from '@/routes/LoginRoute';
import { SignupRoute } from '@/routes/SignupRoute';
import { VerifyEmailRoute } from '@/routes/VerifyEmailRoute';
import { ForgotPasswordRoute } from '@/routes/ForgotPasswordRoute';
import { ResetPasswordRoute } from '@/routes/ResetPasswordRoute';
import { AuthCallbackRoute } from '@/routes/AuthCallbackRoute';
import { SignedOutRoute } from '@/routes/SignedOutRoute';
import { DashboardRoute } from '@/routes/DashboardRoute';
import { PlaceholderRoute } from '@/routes/PlaceholderRoute';
import { WatchlistsRoute } from '@/routes/WatchlistsRoute';
import { WatchlistDetailRoute } from '@/routes/WatchlistDetailRoute';
import { AnalysisRoute } from '@/routes/AnalysisRoute';
import { TransactionsRoute } from '@/routes/TransactionsRoute';
import { PerformanceRoute } from '@/routes/PerformanceRoute';
import { AlertsRoute } from '@/routes/AlertsRoute';

export function App() {
  return (
    <Routes>
      {/* Public auth routes render outside the app shell. */}
      <Route path="/login" element={<LoginRoute />} />
      <Route path="/signup" element={<SignupRoute />} />
      <Route path="/verify-email" element={<VerifyEmailRoute />} />
      <Route path="/forgot-password" element={<ForgotPasswordRoute />} />
      <Route path="/reset-password" element={<ResetPasswordRoute />} />
      <Route path="/signed-out" element={<SignedOutRoute />} />
      {/* Cognito Hosted-UI return target — must stay public (see AuthCallbackRoute). */}
      <Route path="/auth/callback" element={<AuthCallbackRoute />} />

      {/* Everything else requires an authenticated session. */}
      <Route
        element={
          <ProtectedRoute>
            <AppShell>
              <Outlet />
            </AppShell>
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<DashboardRoute />} />
        <Route path="/watchlists" element={<WatchlistsRoute />} />
        <Route path="/watchlists/:id" element={<WatchlistDetailRoute />} />
        <Route path="/transactions" element={<TransactionsRoute />} />
        <Route path="/performance" element={<PerformanceRoute />} />
        <Route path="/alerts" element={<AlertsRoute />} />
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
      </Route>
    </Routes>
  );
}
