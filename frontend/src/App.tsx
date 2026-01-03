import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import {
  Home,
  Login,
  Logout,
  Dashboard,
  Portfolios,
  Watchlist,
  Register,
  OAuth2Redirect,
  Transactions,
} from '@pages/index'
import { ProtectedRoute } from '@components/auth/ProtectedRoute'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
        <Route path="/logout" element={<Logout />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard/transactions"
          element={
            <ProtectedRoute>
              <Transactions />
            </ProtectedRoute>
          }
        />
        <Route
          path="/portfolios"
          element={
            <ProtectedRoute>
              <Portfolios />
            </ProtectedRoute>
          }
        />
        <Route
          path="/watchlist"
          element={
            <ProtectedRoute>
              <Watchlist />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  )
}

export default App
