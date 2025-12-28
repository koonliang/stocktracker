import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { Home, Login, Dashboard, Portfolios, Watchlist } from '@pages/index'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/portfolios" element={<Portfolios />} />
        <Route path="/watchlist" element={<Watchlist />} />
      </Routes>
    </Router>
  )
}

export default App
