import { PortfolioTable } from '@components/dashboard'
import { DashboardNavigation } from '@components/layout'
import { mockHoldings } from '../../mocks/portfolioData'

const Dashboard = () => {
  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-12">
        {/* Dashboard Header */}
        <header className="mb-8">
          <h1 className="text-3xl sm:text-4xl font-bold text-slate-900 mb-2">
            Portfolio Overview
          </h1>
          <p className="text-slate-600">Track your investments and monitor performance</p>
        </header>

        {/* Holdings Section */}
        <main>
          <section className="holdings-section">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold text-slate-900">Your Holdings</h2>
            </div>
            <PortfolioTable holdings={mockHoldings} />
          </section>
        </main>
      </div>
    </div>
  )
}

export default Dashboard
