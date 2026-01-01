import { Link } from 'react-router-dom'

const Navigation = () => {
  return (
    <nav className="bg-white border-b border-slate-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-indigo-600 to-violet-600 rounded-lg flex items-center justify-center">
              <svg
                className="w-5 h-5 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
                />
              </svg>
            </div>
            <span className="text-lg font-bold text-slate-900">Stock Tracker</span>
          </div>

          {/* Navigation Links */}
          <div className="hidden md:flex items-center gap-8">
            <Link
              to="/"
              className="text-sm font-medium text-slate-700 hover:text-indigo-600 transition-colors"
            >
              Features
            </Link>
            <Link
              to="/"
              className="text-sm font-medium text-slate-700 hover:text-indigo-600 transition-colors"
            >
              Solutions
            </Link>
            <Link
              to="/"
              className="text-sm font-medium text-slate-700 hover:text-indigo-600 transition-colors"
            >
              Pricing
            </Link>
            <Link
              to="/"
              className="text-sm font-medium text-slate-700 hover:text-indigo-600 transition-colors"
            >
              Company
            </Link>
          </div>

          {/* CTA Buttons */}
          <div className="flex items-center gap-4">
            <Link
              to="/login"
              className="text-sm font-medium text-slate-700 hover:text-indigo-600 transition-colors"
            >
              Sign in
            </Link>
            <Link
              to="/login"
              className="px-5 py-2 bg-gradient-to-r from-indigo-600 to-violet-600 text-white text-sm font-semibold rounded-full shadow-button hover:-translate-y-0.5 transition-all duration-200"
            >
              Get Started
            </Link>
          </div>
        </div>
      </div>
    </nav>
  )
}

export default Navigation
