import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { Home, Login, Dashboard } from '@pages/index'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
      </Routes>
    </Router>
  )
}

export default App
